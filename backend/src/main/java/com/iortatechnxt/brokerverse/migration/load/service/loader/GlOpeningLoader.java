package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTbLine;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTbLineRepository;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Loader of the legacy GL trial balance (object G01; DATA_MIGRATION_DESIGN 14.2 and 17.7). The
 * lines of a version, branch and currency are loaded together and kept with their BIBS account
 * (code map GL_ACCOUNT; legacy control accounts map to Migration Clearing). The PROVISIONAL version
 * is posted as the opening trial balance journal (reference MIG-TB-&lt;as-of&gt;) on the opening
 * value date; the true-up versions (TU1-TU3, FINAL) are kept for the true-up reconciliation only.
 */
@Component
public class GlOpeningLoader implements MigrationLoader {

  /** Entity of a posted opening journal in the cross-reference. */
  public static final String JOURNAL = "JournalBatch";

  /** Entity of a trial balance kept for reconciliation. */
  public static final String TRIAL_BALANCE = "TrialBalance";

  private static final String PROVISIONAL = "PROVISIONAL";
  private static final String VERSION = "tb_version";
  private static final String BRANCH = "branch_code";
  private static final String CURRENCY = "currency";

  private final OpeningJournals journals;
  private final MigTbLineRepository lines;
  private final BranchRepository branches;
  private final MigrationParameters parameters;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param journals opening journals
   * @param lines trial balance lines
   * @param branches branches
   * @param parameters migration parameters
   * @param currentUser current user
   * @param clock clock
   */
  public GlOpeningLoader(
      OpeningJournals journals,
      MigTbLineRepository lines,
      BranchRepository branches,
      MigrationParameters parameters,
      CurrentUser currentUser,
      Clock clock) {
    this.journals = journals;
    this.lines = lines;
    this.branches = branches;
    this.parameters = parameters;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Override
  public String objectCode() {
    return "G01";
  }

  @Override
  public List<LoadUnit> group(List<LoadUnit> units) {
    return JournalGroups.group(
        units, u -> u.value(VERSION) + "|" + u.value(BRANCH) + "|" + u.value(CURRENCY));
  }

  @Override
  public String partitionKey(LoadUnit unit) {
    return unit.value(VERSION) + "|" + unit.value(BRANCH) + "|" + unit.value(CURRENCY);
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    String version = Values.code(v.get(VERSION));
    String branch = Values.code(v.get(BRANCH));
    String currency = Values.code(v.get(CURRENCY));
    LocalDate asOf = Values.date(v.get("as_of_date")).orElse(null);
    Long branchId =
        branches
            .findByCompanyIdAndCode(ctx.companyId(), branch)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_BRANCH_UNKNOWN", "Branch " + branch + " not found"))
            .getId();
    MigTbLine.Key key =
        new MigTbLine.Key(ctx.companyId(), ctx.batch().getId(), version, asOf, branchId, currency);
    List<OpeningJournals.Amounts> amounts = new ArrayList<>();
    MigTbLine first = null;
    for (StageRow row : JournalGroups.lines(unit)) {
      Map<String, String> r = row.getMappedPayload();
      MigTbLine line = lines.save(line(key, r, row.getRawPayload()));
      first = first == null ? line : first;
      amounts.add(
          OpeningJournals.Amounts.of(
              Values.text(r.get("legacy_account_code")),
              Values.text(r.get("cost_center")),
              Values.amount(r.get("debit_fc")),
              Values.amount(r.get("credit_fc")),
              Values.amount(r.get("debit_php")),
              Values.amount(r.get("credit_php"))));
    }
    if (!PROVISIONAL.equals(version)) {
      return LoadOutcome.of(
          TRIAL_BALANCE, first.getId(), "TB-" + version + "-" + branch + "-" + currency, 0L);
    }
    String reference = "MIG-TB-" + asOf;
    JournalBatch batch =
        journals.post(
            new OpeningJournals.Header(
                ctx.companyId(),
                branchId,
                currency,
                parameters.openingValueDate(),
                reference,
                reference + "-" + branch + "-" + currency,
                "Provisional opening trial balance " + asOf + " (PROVISIONAL)"),
            amounts,
            !"B".equals(parameters.yearEndOption()));
    return LoadOutcome.of(JOURNAL, batch.getId(), batch.getBatchNo(), batch.getVersion());
  }

  private MigTbLine line(MigTbLine.Key key, Map<String, String> mapped, Map<String, String> raw) {
    return new MigTbLine(
        key,
        new MigTbLine.Account(
            Values.text(raw.get("legacy_account_code")),
            Values.text(mapped.get("legacy_account_code")),
            Values.text(mapped.get("cost_center"))),
        new MigTbLine.Amounts(
            Values.amount(mapped.get("debit_fc")),
            Values.amount(mapped.get("credit_fc")),
            Values.amount(mapped.get("debit_php")),
            Values.amount(mapped.get("credit_php"))),
        currentUser.username(),
        clock.instant());
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    lines.findByBatchId(entry.getBatchId()).stream()
        .filter(l -> l.getRolledBackAt() == null)
        .forEach(l -> l.rollBack(clock.instant()));
    if (JOURNAL.equals(entry.getTargetEntity())) {
      journals.reverse(ctx.companyId(), entry.getTargetCode(), parameters.openingValueDate());
    }
    return true;
  }
}
