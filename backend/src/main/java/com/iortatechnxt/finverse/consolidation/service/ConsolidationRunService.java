package com.iortatechnxt.finverse.consolidation.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationGroup;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationLineValues;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRun;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRunLine;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRunRepository;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRunStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consolidation runs: collects member trial balances, translates them, applies the elimination
 * rules and stores the consolidated ledger.
 *
 * <p>A re-run for the same group and date cancels the previous DRAFT run; a FINAL run blocks
 * duplicates (spec exception "Duplicate Consolidation Run").
 */
@Service
@Transactional
public class ConsolidationRunService {

  private static final String ENTITY = "ConsolidationRun";
  private static final Set<ConsolidationRunStatus> LIVE =
      EnumSet.of(ConsolidationRunStatus.DRAFT, ConsolidationRunStatus.FINAL);

  private final ConsolidationRunRepository runs;
  private final ConsolidationGroupService groups;
  private final TranslationService translation;
  private final IntercompanyService intercompany;
  private final ChartOfAccountsService accounts;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param runs run repository
   * @param groups group service
   * @param translation translation service
   * @param intercompany inter-company relationships
   * @param accounts chart of accounts
   * @param numbers document numbering
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ConsolidationRunService(
      ConsolidationRunRepository runs,
      ConsolidationGroupService groups,
      TranslationService translation,
      IntercompanyService intercompany,
      ChartOfAccountsService accounts,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.runs = runs;
    this.groups = groups;
    this.translation = translation;
    this.intercompany = intercompany;
    this.accounts = accounts;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Runs the consolidation of a group as of a date.
   *
   * @param groupId group
   * @param asOf as-of date
   * @return draft run with its consolidated ledger
   */
  public ConsolidationRun run(Long groupId, LocalDate asOf) {
    ConsolidationGroup group = groups.get(groupId);
    if (!group.isActive()) {
      throw new BusinessRuleException(
          "INVALID_CONSOLIDATION_GROUP", "Consolidation group " + group.getCode() + " is inactive");
    }
    List<ConsolidationRun> existing = runs.findByGroupIdAndAsOfDateAndStatusIn(groupId, asOf, LIVE);
    if (existing.stream().anyMatch(r -> r.getStatus() == ConsolidationRunStatus.FINAL)) {
      throw new BusinessRuleException(
          "DUPLICATE_CONSOLIDATION_RUN",
          "A final consolidation of " + group.getCode() + " as of " + asOf + " already exists");
    }
    existing.forEach(ConsolidationRun::cancel);
    List<ConsolidationLineValues> lines = new ArrayList<>();
    for (Long companyId : group.companyIds()) {
      lines.addAll(
          translation.translate(companyId, group.getCurrency(), group.getCtaAccount(), asOf));
    }
    Map<String, String> names =
        accounts.list(group.getParentCompanyId()).stream()
            .collect(Collectors.toMap(GlAccount::getCode, GlAccount::getName));
    List<ConsolidationLineValues> eliminations =
        ConsolidationEliminations.build(
            group, lines, intercompany.relationships(null), code -> names.getOrDefault(code, code));
    lines.addAll(eliminations);
    ConsolidationRun run =
        new ConsolidationRun(
            groupId, numbers.next("CON-" + group.getCode()), asOf, group.getCurrency());
    run.replaceLines(lines);
    if (!run.isBalanced()) {
      throw new BusinessRuleException(
          "CONSOLIDATION_UNBALANCED",
          "Consolidated trial balance does not balance: "
              + run.getTotalDebit()
              + " / "
              + run.getTotalCredit());
    }
    ConsolidationRun saved = runs.save(run);
    audit.record(
        ENTITY,
        saved.getRunNo(),
        AuditAction.RUN,
        "Consolidated "
            + group.getCode()
            + " as of "
            + asOf
            + ", "
            + eliminations.size()
            + " eliminations");
    return saved;
  }

  /**
   * Locks a run as final.
   *
   * @param runId run
   * @return run
   */
  public ConsolidationRun finalizeRun(Long runId) {
    ConsolidationRun run = get(runId);
    run.finalizeRun(currentUser.username(), clock.instant());
    audit.record(ENTITY, run.getRunNo(), AuditAction.AUTHORIZE, "Finalized consolidation run");
    return run;
  }

  /**
   * Lists runs of a group.
   *
   * @param groupId group
   * @return runs, newest first
   */
  @Transactional(readOnly = true)
  public List<ConsolidationRun> list(Long groupId) {
    return runs.findByGroupIdOrderByAsOfDateDescIdDesc(groupId);
  }

  /**
   * Gets a run with its lines.
   *
   * @param runId id
   * @return run
   */
  @Transactional(readOnly = true)
  public ConsolidationRun get(Long runId) {
    ConsolidationRun run =
        runs.findById(runId).orElseThrow(() -> new ResourceNotFoundException(ENTITY, runId));
    Hibernate.initialize(run.getLines());
    return run;
  }

  /**
   * Latest live (draft or final) run of a group on or before a date, with lines.
   *
   * @param groupCode group code
   * @param asOf latest as-of date, or null for the latest run
   * @return run if any
   */
  @Transactional(readOnly = true)
  public Optional<ConsolidationRun> latest(String groupCode, LocalDate asOf) {
    ConsolidationGroup group = groups.getByCode(groupCode);
    Optional<ConsolidationRun> run =
        asOf == null
            ? runs.findFirstByGroupIdAndStatusInOrderByAsOfDateDescIdDesc(group.getId(), LIVE)
            : runs.findFirstByGroupIdAndAsOfDateLessThanEqualAndStatusInOrderByAsOfDateDescIdDesc(
                group.getId(), asOf, LIVE);
    run.ifPresent(r -> Hibernate.initialize(r.getLines()));
    return run;
  }

  /**
   * Consolidated trial balance of a run: net amount per account code.
   *
   * @param run run
   * @return balances keyed by account code, ordered by code
   */
  public static Map<String, ConsolidatedBalance> trialBalance(ConsolidationRun run) {
    return run.getLines().stream()
        .collect(
            Collectors.toMap(
                ConsolidationRunLine::getAccountCode,
                ConsolidatedBalance::of,
                ConsolidatedBalance::plus,
                TreeMap::new));
  }
}
