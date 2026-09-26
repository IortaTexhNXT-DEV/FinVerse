package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigVersionRef;
import com.iortatechnxt.brokerverse.screening.matching.domain.MatchStatus;
import com.iortatechnxt.brokerverse.screening.matching.domain.MatchSuppression;
import com.iortatechnxt.brokerverse.screening.matching.domain.MatchSuppressionRepository;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatch;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatchRepository;
import com.iortatechnxt.brokerverse.screening.risk.service.ManualRiskChange;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskOutcome;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskOverrideService;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskProfiler;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decisions on screening matches (SNSRP-301, 302, 304; FR-SS-032, 035):
 *
 * <ul>
 *   <li><b>confirm</b> (case investigation): the match becomes TRUE_MATCH and the risk rules are
 *       evaluated again on it (the evaluation point of a combination such as "SANCTION and
 *       TRUE_MATCH", design 17.2);
 *   <li><b>mark false positive</b>: justification and evidence mandatory; the match becomes
 *       FALSE_POSITIVE, the client is suppressed for the entry version (SQ12) and, when a rating or
 *       tags are given, the profile is corrected by hand (source MANUAL);
 *   <li><b>open case</b>: through the {@link MatchCaseOpener} port of the case wave;
 *   <li><b>link to case</b>: the case wave stamps its case on matches.
 * </ul>
 */
@Service
@Transactional
public class MatchDecisionService {

  /** Attachment entity type of match evidence. */
  public static final String MATCH_ENTITY = "ScreeningMatch";

  private static final String MATCH_PREFIX = "Match ";
  private static final String RISK_TAG = "SCR_RISK_TAG";

  private final ScreeningMatchRepository matches;
  private final MatchSuppressionRepository suppressions;
  private final ClientService clients;
  private final RiskProfiler profiler;
  private final RiskOverrideService overrides;
  private final ActiveConfig config;
  private final AttachmentService attachments;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final ObjectProvider<MatchCaseOpener> caseOpener;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param matches matches
   * @param suppressions suppressions
   * @param clients client master (read)
   * @param profiler risk rules
   * @param overrides manual risk changes
   * @param config active configuration
   * @param attachments evidence documents
   * @param audit audit trail
   * @param currentUser current user
   * @param caseOpener case wave port (optional)
   * @param clock clock
   */
  public MatchDecisionService(
      ScreeningMatchRepository matches,
      MatchSuppressionRepository suppressions,
      ClientService clients,
      RiskProfiler profiler,
      RiskOverrideService overrides,
      ActiveConfig config,
      AttachmentService attachments,
      AuditTrailService audit,
      CurrentUser currentUser,
      ObjectProvider<MatchCaseOpener> caseOpener,
      Clock clock) {
    this.matches = matches;
    this.suppressions = suppressions;
    this.clients = clients;
    this.profiler = profiler;
    this.overrides = overrides;
    this.config = config;
    this.attachments = attachments;
    this.audit = audit;
    this.currentUser = currentUser;
    this.caseOpener = caseOpener;
    this.clock = clock;
  }

  /**
   * A match.
   *
   * @param id match id
   * @return the match
   */
  @Transactional(readOnly = true)
  public ScreeningMatch get(Long id) {
    return matches.findById(id).orElseThrow(() -> new ResourceNotFoundException(MATCH_ENTITY, id));
  }

  /**
   * Confirms a potential match (investigation) and evaluates the risk rules on it.
   *
   * @param matchId the match
   * @param caseId the case that confirmed it, may be {@code null}
   * @param remarks remarks, may be {@code null}
   * @return the decision with the risk outcome
   */
  public MatchDecision confirm(Long matchId, Long caseId, String remarks) {
    ScreeningMatch match = get(matchId);
    match.confirm(currentUser.username(), clock.instant(), remarks);
    if (caseId != null) {
      match.linkCase(caseId);
    }
    audit.record(MATCH_ENTITY, matchId, AuditAction.UPDATE, describe(match, "confirmed"));
    Long riskVersion =
        config
            .activeVersion(match.getCompanyId(), ConfigType.RISK_RULES, null, LocalDate.now(clock))
            .map(ConfigVersionRef::id)
            .orElse(null);
    RiskOutcome outcome =
        profiler
            .evaluate(
                ScreeningSubject.of(clients.get(match.getClientId())),
                riskVersion,
                null,
                MATCH_PREFIX + matchId)
            .orElse(null);
    return new MatchDecision(ScreenedMatch.from(match), outcome, null);
  }

  /**
   * Marks a match a false positive.
   *
   * @param matchId the match
   * @param request justification, evidence and optional profile correction
   * @return the decision
   */
  public MatchDecision markFalsePositive(Long matchId, FalsePositive request) {
    if (request.justification() == null || request.justification().isBlank()) {
      throw new BusinessRuleException(
          "SCR_JUSTIFICATION_REQUIRED", "Enter the justification and attach the evidence");
    }
    if (request.changesProfile() && !currentUser.hasAuthority(RISK_TAG)) {
      throw new AccessDeniedException("Changing the risk profile needs " + RISK_TAG);
    }
    ScreeningMatch match = get(matchId);
    List<Long> evidence = evidence(matchId, request);
    String justification = request.justification().trim();
    match.clear(currentUser.username(), clock.instant(), justification);
    suppress(match, justification, evidence, request.caseId());
    audit.record(
        MATCH_ENTITY,
        matchId,
        AuditAction.UPDATE,
        describe(match, "false positive: " + justification));
    Long entryId =
        request.changesProfile()
            ? overrides
                .override(
                    match.getClientId(),
                    new ManualRiskChange(
                        request.riskRating(),
                        request.addTags(),
                        request.removeTags(),
                        justification,
                        evidence,
                        matchId,
                        request.caseId(),
                        MATCH_PREFIX + matchId))
                .getId()
            : null;
    return new MatchDecision(ScreenedMatch.from(match), null, entryId);
  }

  private List<Long> evidence(Long matchId, FalsePositive request) {
    List<Long> evidence =
        request.evidenceAttachmentIds().isEmpty()
            ? attachments.list(new AttachmentTarget(MATCH_ENTITY, String.valueOf(matchId))).stream()
                .map(Attachment::getId)
                .toList()
            : request.evidenceAttachmentIds();
    if (evidence.isEmpty()) {
      throw new BusinessRuleException(
          "SCR_EVIDENCE_REQUIRED", "Attach at least one evidence document");
    }
    return evidence;
  }

  private void suppress(
      ScreeningMatch match, String justification, List<Long> evidence, Long caseId) {
    if (!suppressions.existsByClientIdAndEntryIdAndEntryVersion(
        match.getClientId(), match.getEntryId(), match.getEntryVersion())) {
      suppressions.save(
          new MatchSuppression(
              match,
              justification,
              evidence.stream().map(String::valueOf).collect(Collectors.joining(",")),
              caseId));
    }
  }

  /**
   * Opens a case for a potential match, or adds it to the client's open case (FR-SS-032).
   *
   * @param matchId the match
   * @return the case
   */
  public MatchCaseOpener.OpenedCase openCase(Long matchId) {
    ScreeningMatch match = get(matchId);
    if (match.getStatus() != MatchStatus.POTENTIAL || match.getCaseId() != null) {
      throw new BusinessRuleException(
          "SCR_MATCH_NOT_OPEN", "Only a potential match not yet in a case can open a case");
    }
    MatchCaseOpener opener = caseOpener.getIfAvailable();
    if (opener == null) {
      throw new BusinessRuleException(
          "SCR_CASES_NOT_AVAILABLE", "Screening cases are not available yet");
    }
    return opener.open(ScreenedMatch.from(match));
  }

  /**
   * Stamps a case on matches (case wave).
   *
   * @param matchIds the matches
   * @param caseId the case
   */
  public void linkToCase(Collection<Long> matchIds, Long caseId) {
    for (Long id : matchIds) {
      ScreeningMatch match = get(id);
      match.linkCase(caseId);
      audit.record(
          MATCH_ENTITY, id, AuditAction.UPDATE, describe(match, "added to case " + caseId));
    }
  }

  private static String describe(ScreeningMatch m, String what) {
    return MATCH_PREFIX
        + m.getId()
        + " ("
        + m.getClientCode()
        + " / "
        + m.getEntryName()
        + " v"
        + m.getEntryVersion()
        + ") "
        + what;
  }
}
