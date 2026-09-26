package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.CommitteeVote;
import com.iortatechnxt.brokerverse.screening.cases.domain.CommitteeVoteRepository;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningQueries;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRun;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListFileService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of screening cases (SNSRP-402, 403, 405; FR-SS-041, 042, 045): the case list by tab, search
 * text and filters within the user's scope (investigators: own and team cases; coordinators,
 * Compliance, approvers, committee and auditors: all), a case, the client's cases, the committee
 * votes and the Screening Home tiles.
 */
@Service
@Transactional(readOnly = true)
public class CaseQueries {

  /** Shortest search text (FR-SS-042 R1). */
  static final int MIN_SEARCH = 3;

  private final ScreeningCaseRepository cases;
  private final CommitteeVoteRepository votes;
  private final CaseAccess access;
  private final CaseClientFacts clientFacts;
  private final ScreeningQueries matches;
  private final ListFileService listFiles;
  private final Clock clock;

  /**
   * Creates the reads.
   *
   * @param cases cases
   * @param votes committee votes
   * @param access case access
   * @param clientFacts sales teams
   * @param matches match reads
   * @param listFiles watchlist runs
   * @param clock clock
   */
  public CaseQueries(
      ScreeningCaseRepository cases,
      CommitteeVoteRepository votes,
      CaseAccess access,
      CaseClientFacts clientFacts,
      ScreeningQueries matches,
      ListFileService listFiles,
      Clock clock) {
    this.cases = cases;
    this.votes = votes;
    this.access = access;
    this.clientFacts = clientFacts;
    this.matches = matches;
    this.listFiles = listFiles;
    this.clock = clock;
  }

  /**
   * The case list.
   *
   * @param search tab, text and filters
   * @param pageable page
   * @return cases, newest first
   */
  public Page<ScreeningCase> search(CaseSearch search, Pageable pageable) {
    validate(search);
    Specification<ScreeningCase> spec = scope(search.companyId());
    spec =
        and(
            spec,
            CaseSpecs.tab(search.tab() == null ? CaseSearch.Tab.ALL : search.tab(), access.user()));
    if (search.q() != null && !search.q().isBlank()) {
      spec = spec.and(CaseSpecs.text(search.q()));
    }
    spec = and(spec, search.stage() == null ? null : CaseSpecs.equal("stage", search.stage()));
    spec = and(spec, filter("caseType", search.caseType()));
    spec = and(spec, filter("riskCategory", search.riskCategory()));
    spec = and(spec, filter("marketingUnit", search.marketingUnit()));
    spec = and(spec, filter("unitHead", search.unitHead()));
    spec = and(spec, filter("disposition", search.disposition()));
    spec =
        and(spec, blank(search.assignee()) ? null : CaseSpecs.assignee(search.assignee().strip()));
    spec = and(spec, search.sla() == null ? null : CaseSpecs.sla(search.sla(), clock.instant()));
    spec =
        and(
            spec,
            search.createdFrom() == null ? null : CaseSpecs.createdFrom(search.createdFrom()));
    spec = and(spec, search.createdTo() == null ? null : CaseSpecs.createdTo(search.createdTo()));
    return cases.findAll(spec, pageable);
  }

  private static void validate(CaseSearch search) {
    if (search.q() != null && !search.q().isBlank() && search.q().strip().length() < MIN_SEARCH) {
      throw new BusinessRuleException("SCR_SEARCH_TOO_SHORT", "Enter at least 3 characters");
    }
    if (search.createdFrom() != null
        && search.createdTo() != null
        && search.createdTo().isBefore(search.createdFrom())) {
      throw new BusinessRuleException(
          "SCR_PERIOD_INVALID", "The end date must be on or after the start date");
    }
  }

  private static Specification<ScreeningCase> and(
      Specification<ScreeningCase> spec, Specification<ScreeningCase> more) {
    return more == null ? spec : spec.and(more);
  }

  private static Specification<ScreeningCase> filter(String attribute, String value) {
    return blank(value) ? null : CaseSpecs.equal(attribute, value.strip());
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private Specification<ScreeningCase> scope(Long companyId) {
    Specification<ScreeningCase> company = CaseSpecs.company(companyId);
    if (access.seesAll()) {
      return company;
    }
    String user = access.user();
    List<CaseStage> own =
        Arrays.stream(CaseStage.values())
            .filter(s -> CaseAccess.ownerOf(s) != null && access.can(CaseAccess.ownerOf(s)))
            .toList();
    return company.and(
        CaseSpecs.teamScope(user, clientFacts.teamOf(companyId, user).orElse(null), own));
  }

  /**
   * A case the user may see.
   *
   * @param id the case
   * @return the case
   */
  public ScreeningCase get(Long id) {
    ScreeningCase c =
        cases.findById(id).orElseThrow(() -> new ResourceNotFoundException(CaseCodes.ENTITY, id));
    if (!access.seesAll()
        && cases.count(scope(c.getCompanyId()).and(CaseSpecs.equal("id", id))) == 0) {
      throw new AccessDeniedException("Case " + c.getCaseNo() + " is not in your scope");
    }
    return c;
  }

  /**
   * The cases of a client, newest first (client Screening tab).
   *
   * @param clientId the client
   * @return cases
   */
  public List<ScreeningCase> ofClient(Long clientId) {
    return cases.findByClientIdOrderByCreatedAtDesc(clientId);
  }

  /**
   * The committee votes of a case, in order.
   *
   * @param caseId the case
   * @return votes
   */
  public List<CommitteeVote> votes(Long caseId) {
    return votes.findByCaseIdOrderByIdAsc(caseId);
  }

  /**
   * Whether the current user voted on the case's current committee round.
   *
   * @param c the case
   * @return true when voted
   */
  public boolean voted(ScreeningCase c) {
    return votes.existsByCaseIdAndRoundNoAndMemberIgnoreCase(
        c.getId(), c.getCommitteeRound(), access.user());
  }

  /**
   * The Screening Home tiles within the user's scope (FR-SS-045).
   *
   * @param companyId company
   * @return the counts and the last list run
   */
  public Tiles tiles(Long companyId) {
    Specification<ScreeningCase> scope = scope(companyId);
    Map<CaseStage, Long> byStage = new EnumMap<>(CaseStage.class);
    for (CaseStage stage : CaseStage.values()) {
      if (stage != CaseStage.CLOSED) {
        byStage.put(stage, cases.count(scope.and(CaseSpecs.equal("stage", stage))));
      }
    }
    Instant now = clock.instant();
    Instant endOfDay =
        LocalDate.now(clock.withZone(CaseSpecs.MANILA))
            .plusDays(1)
            .atStartOfDay(CaseSpecs.MANILA)
            .toInstant();
    Optional<IngestionRun> lastRun =
        listFiles.runs(null, PageRequest.of(0, 1)).stream().findFirst();
    return new Tiles(
        byStage,
        cases.count(scope.and(CaseSpecs.dueBefore(endOfDay))),
        cases.count(scope.and(CaseSpecs.sla(CaseSla.SlaState.BREACHED, now))),
        matches.openPotentialMatches(companyId),
        lastRun.orElse(null));
  }

  /**
   * The Screening Home tiles.
   *
   * @param openByStage open cases per stage
   * @param dueToday open cases due before the end of today and not breached
   * @param breached open cases past their SLA
   * @param potentialMatches potential matches not yet in a case
   * @param lastRun the last watchlist ingestion run, may be null
   */
  public record Tiles(
      Map<CaseStage, Long> openByStage,
      long dueToday,
      long breached,
      long potentialMatches,
      IngestionRun lastRun) {}
}
