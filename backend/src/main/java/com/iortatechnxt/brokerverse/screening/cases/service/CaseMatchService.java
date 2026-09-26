package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocument;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocumentRepository;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseTypes;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatch;
import com.iortatechnxt.brokerverse.screening.matching.service.FalsePositive;
import com.iortatechnxt.brokerverse.screening.matching.service.MatchDecision;
import com.iortatechnxt.brokerverse.screening.matching.service.MatchDecisionService;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningQueries;
import com.iortatechnxt.brokerverse.screening.risk.domain.RiskProfileEntry;
import com.iortatechnxt.brokerverse.screening.risk.service.ManualRiskChange;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskOutcome;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskOverrideService;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The matches and the risk tag of a case under investigation (SNSRP-301, 302, 304; FR-SS-032, 035):
 * the investigator confirms a match of the case (the risk rules are evaluated again and the
 * category's case is opened or joined) or clears it as a false positive with the case documents as
 * evidence, and updates the client's risk tag with a justification and evidence ("Update Risk
 * Tag"). Each decision is on the case timeline.
 */
@Service
@Transactional
public class CaseMatchService {

  private static final Set<CaseStage> WORKING =
      EnumSet.of(CaseStage.INVESTIGATION, CaseStage.RETURNED);

  private final ScreeningCaseRepository cases;
  private final CaseDocumentRepository documents;
  private final MatchDecisionService decisions;
  private final ScreeningQueries queries;
  private final RiskOverrideService overrides;
  private final ClientService clients;
  private final CaseOpeningService opening;
  private final CaseAccess access;
  private final CaseTimeline timeline;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param documents case documents (evidence)
   * @param decisions match decisions
   * @param queries match reads
   * @param overrides manual risk changes
   * @param clients client master (read)
   * @param opening case opening
   * @param access case access
   * @param timeline case timeline
   */
  @SuppressWarnings("java:S107") // collaborators of the match decisions
  public CaseMatchService(
      ScreeningCaseRepository cases,
      CaseDocumentRepository documents,
      MatchDecisionService decisions,
      ScreeningQueries queries,
      RiskOverrideService overrides,
      ClientService clients,
      CaseOpeningService opening,
      CaseAccess access,
      CaseTimeline timeline) {
    this.cases = cases;
    this.documents = documents;
    this.decisions = decisions;
    this.queries = queries;
    this.overrides = overrides;
    this.clients = clients;
    this.opening = opening;
    this.access = access;
    this.timeline = timeline;
  }

  /**
   * The matches of a case.
   *
   * @param c the case
   * @return matches, newest first
   */
  @Transactional(readOnly = true)
  public List<ScreeningMatch> of(ScreeningCase c) {
    return queries.clientMatches(c.getClientId()).stream()
        .filter(m -> Objects.equals(m.getCaseId(), c.getId()))
        .toList();
  }

  /**
   * Confirms a match of the case as a true match (FR-SS-032).
   *
   * @param caseId the case
   * @param matchId the match
   * @param remarks remarks
   * @return the decision
   */
  public MatchDecision confirm(Long caseId, Long matchId, String remarks) {
    ScreeningCase c = actor(caseId, "confirm the match");
    requireOwn(c, matchId);
    MatchDecision decision = decisions.confirm(matchId, caseId, remarks);
    timeline.record(
        c,
        CaseEventType.MATCH_DECIDED,
        EventFacts.change("POTENTIAL", "TRUE_MATCH", null, "Match " + matchId + ": " + remarks));
    RiskOutcome outcome = decision.riskOutcome();
    if (outcome != null
        && outcome.caseType() != null
        && !outcome.caseType().equals(c.getCaseType())) {
      String type =
          CaseTypes.NEED_ACTIVE_POLICY.contains(outcome.caseType())
                  && !opening.hasActivePolicy(c.getClientId())
              ? CaseTypes.MONITOR
              : outcome.caseType();
      if (!type.equals(c.getCaseType())) {
        opening.openOrJoin(
            clients.get(c.getClientId()),
            new CaseOpeningService.OpenSpec(
                type,
                "MANUAL",
                c.getCaseNo(),
                outcome.categoryCode(),
                outcome.requiresEdd(),
                List.of(),
                null,
                outcome.riskVersionId()));
      }
    }
    return decision;
  }

  /**
   * Clears a match of the case as a false positive; the case documents are the evidence unless
   * others are given (FR-SS-035).
   *
   * @param caseId the case
   * @param matchId the match
   * @param request justification, evidence and optional profile correction
   * @return the decision
   */
  public MatchDecision falsePositive(Long caseId, Long matchId, FalsePositive request) {
    ScreeningCase c = actor(caseId, "clear the match");
    requireOwn(c, matchId);
    List<Long> evidence =
        request.evidenceAttachmentIds().isEmpty() ? evidence(c) : request.evidenceAttachmentIds();
    MatchDecision decision =
        decisions.markFalsePositive(
            matchId,
            new FalsePositive(
                request.justification(),
                evidence,
                request.riskRating(),
                request.addTags(),
                request.removeTags(),
                caseId));
    timeline.record(
        c,
        CaseEventType.MATCH_DECIDED,
        EventFacts.change(
            "POTENTIAL",
            "FALSE_POSITIVE",
            null,
            "Match " + matchId + ": " + request.justification()));
    return decision;
  }

  /**
   * "Update Risk Tag" on the case (SNSRP-304; FR-SS-035): the client's rating and tags with a
   * justification and evidence (the case documents unless others are given).
   *
   * @param caseId the case
   * @param change rating, tags, justification and evidence
   * @return the risk-profile history row
   */
  public RiskProfileEntry riskTag(Long caseId, ManualRiskChange change) {
    ScreeningCase c = actor(caseId, "update the risk tag");
    access.requirePermission(CaseCodes.RISK_TAG);
    List<Long> evidence =
        change.evidenceAttachmentIds().isEmpty() ? evidence(c) : change.evidenceAttachmentIds();
    RiskProfileEntry entry =
        overrides.override(
            c.getClientId(),
            new ManualRiskChange(
                change.riskRating(),
                change.addTags(),
                change.removeTags(),
                change.justification(),
                evidence,
                change.matchId(),
                caseId,
                c.getCaseNo()));
    timeline.record(
        c,
        CaseEventType.RISK_TAG_CHANGED,
        EventFacts.change(
            entry.getPreviousRating(), entry.getKycRiskRating(), null, change.justification()));
    return entry;
  }

  private ScreeningCase actor(Long caseId, String action) {
    ScreeningCase c = CaseChecks.get(cases, caseId);
    access.requireActor(c, action, WORKING);
    return c;
  }

  private void requireOwn(ScreeningCase c, Long matchId) {
    if (!Objects.equals(decisions.get(matchId).getCaseId(), c.getId())) {
      throw new BusinessRuleException(
          "SCR_MATCH_NOT_IN_CASE", "Match " + matchId + " is not a match of case " + c.getCaseNo());
    }
  }

  private List<Long> evidence(ScreeningCase c) {
    return documents.findByCaseIdOrderByIdAsc(c.getId()).stream()
        .map(CaseDocument::getAttachmentId)
        .toList();
  }
}
