package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseKind;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseTypes;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseVersions;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigVersionRef;
import com.iortatechnxt.brokerverse.screening.matching.service.MatchCaseOpener;
import com.iortatechnxt.brokerverse.screening.matching.service.MatchDecisionService;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreenedMatch;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens screening cases (SNSRP-303, 106; FR-SS-032, 034): one open case per client and case type (a
 * later match joins the open case and the timeline records it); a new case gets its number {@code
 * <SCR_CASE_SEQUENCE_PREFIX>-yyyy-nnnnnn}, the configuration versions in force, the active-policy
 * flag, the client's marketing unit and unit head, a review on the template in force, and is routed
 * to INVESTIGATION with the investigator of the assignment matrix. A high-risk or PEP client
 * without an active policy gets a MONITOR case and the UCC and the investigators are notified.
 * Implements the matching wave's "Open Case" port.
 */
@Service
@Transactional
public class CaseOpeningService implements MatchCaseOpener {

  /** Parameter of the case number prefix. */
  static final String PREFIX_PARAMETER = "SCR_CASE_SEQUENCE_PREFIX";

  private static final String DEFAULT_PREFIX = "SCR";

  private final ScreeningCaseRepository cases;
  private final ClientService clients;
  private final CaseClientFacts clientFacts;
  private final ActivePolicyQuery activePolicy;
  private final ActiveConfig config;
  private final DocumentNumberService numbers;
  private final SystemParameterService parameters;
  private final WorkflowService workflow;
  private final CaseMover mover;
  private final CaseAssigner assigner;
  private final CaseReviewService reviews;
  private final CaseNotifier notifier;
  private final CaseTimeline timeline;
  private final MatchDecisionService matches;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param clients client master (read)
   * @param clientFacts sales facts of the client
   * @param activePolicy active-policy port
   * @param config active configuration
   * @param numbers case numbers
   * @param parameters business parameters
   * @param workflow workflow
   * @param mover transitions and assignment
   * @param assigner assignment matrix
   * @param reviews case reviews
   * @param notifier notices
   * @param timeline case timeline
   * @param matches match decisions (lazy: the matching wave calls this port)
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of case creation
  public CaseOpeningService(
      ScreeningCaseRepository cases,
      ClientService clients,
      CaseClientFacts clientFacts,
      ActivePolicyQuery activePolicy,
      ActiveConfig config,
      DocumentNumberService numbers,
      SystemParameterService parameters,
      WorkflowService workflow,
      CaseMover mover,
      CaseAssigner assigner,
      CaseReviewService reviews,
      CaseNotifier notifier,
      CaseTimeline timeline,
      @Lazy MatchDecisionService matches,
      AuditTrailService audit,
      Clock clock) {
    this.cases = cases;
    this.clients = clients;
    this.clientFacts = clientFacts;
    this.activePolicy = activePolicy;
    this.config = config;
    this.numbers = numbers;
    this.parameters = parameters;
    this.workflow = workflow;
    this.mover = mover;
    this.assigner = assigner;
    this.reviews = reviews;
    this.notifier = notifier;
    this.timeline = timeline;
    this.matches = matches;
    this.audit = audit;
    this.clock = clock;
  }

  /** "Open Case" on a potential match (FR-SS-032): a NAME_MATCH case, or the open one. */
  @Override
  public OpenedCase open(ScreenedMatch match) {
    Client client = clients.get(match.clientId());
    Opened opened =
        openOrJoin(
            client,
            new OpenSpec(
                CaseTypes.NAME_MATCH,
                "MANUAL",
                "Match " + match.matchId(),
                null,
                false,
                List.of(match.matchId()),
                null,
                null));
    return new OpenedCase(
        opened.screeningCase().getId(), opened.screeningCase().getCaseNo(), !opened.created());
  }

  /**
   * Whether the client has an active policy today (SNSRP-303).
   *
   * @param clientId the client
   * @return true when active
   */
  @Transactional(readOnly = true)
  public boolean hasActivePolicy(Long clientId) {
    return activePolicy.hasActivePolicy(clientId, LocalDate.now(clock));
  }

  /**
   * Opens a case of a type for a client, or adds the matches to the client's open case of that type
   * (FR-SS-034 R1).
   *
   * @param client the client
   * @param spec type, trigger, category, EDD flag, matches and versions
   * @return the case and whether it was created
   */
  public Opened openOrJoin(Client client, OpenSpec spec) {
    Optional<ScreeningCase> open =
        cases.findByClientIdAndCaseTypeAndStatus(client.getId(), spec.caseType(), CaseStatus.OPEN);
    if (open.isPresent()) {
      ScreeningCase c = open.get();
      if (c.getRiskCategory() == null && spec.riskCategory() != null) {
        c.categorise(spec.riskCategory());
      }
      link(c, spec.matchIds());
      return new Opened(c, false);
    }
    return new Opened(create(client, spec), true);
  }

  private ScreeningCase create(Client client, OpenSpec spec) {
    LocalDate today = LocalDate.now(clock);
    Long companyId = client.getCompanyId();
    boolean active = activePolicy.hasActivePolicy(client.getId(), today);
    String prefix = parameters.text(PREFIX_PARAMETER, DEFAULT_PREFIX);
    ScreeningCase c =
        cases.save(
            new ScreeningCase(
                numbers.next(prefix + "-" + today.getYear()),
                clientFacts.of(client),
                new CaseKind(
                    spec.trigger(),
                    spec.reference(),
                    spec.caseType(),
                    spec.riskCategory(),
                    templateOf(spec, active),
                    active),
                versions(companyId, spec, today),
                clock.instant()));
    c.linkWorkCase(startWork(c).getId());
    timeline.record(
        c,
        CaseEventType.CREATED,
        EventFacts.remarks(
            "Opened by "
                + spec.trigger()
                + (spec.reference() == null ? "" : " (" + spec.reference() + ")")
                + (active ? "; active policy" : "; no active policy")));
    link(c, spec.matchIds());
    reviews.start(c);
    routeAndAssign(c);
    if (CaseTypes.MONITOR.equals(c.getCaseType())) {
      String what = "client without an active policy";
      notifier.role(CaseCodes.UCC_ROLE, c, CaseCodes.EVENT_NO_POLICY, what);
      notifier.owner(c, CaseCodes.INVESTIGATE, CaseCodes.EVENT_NO_POLICY, what);
    }
    audit.record(
        CaseCodes.ENTITY,
        c.getCaseNo(),
        AuditAction.CREATE,
        c.getCaseType() + " case of " + c.getClientCode() + " opened by " + spec.trigger());
    return c;
  }

  private WorkCase startWork(ScreeningCase c) {
    return SystemActor.call(
        () ->
            workflow.start(
                new StartCase(
                    c.getCompanyId(),
                    CaseCodes.WORKFLOW,
                    new CaseRecord(
                        CaseCodes.ENTITY,
                        String.valueOf(c.getId()),
                        c.getCaseNo(),
                        c.getClientName() + " - " + c.getCaseType(),
                        CaseCodes.link(c.getId()),
                        c.getMarketingUnit()),
                    null)));
  }

  private void routeAndAssign(ScreeningCase c) {
    SystemActor.call(
        () -> {
          mover.system(c, "route", TransitionNote.comment("Assignment matrix"));
          return c;
        });
    CaseRouter.Approver investigator = assigner.assign(c);
    mover.assign(
        c,
        investigator.user(),
        CaseEventType.ASSIGNED,
        CaseMover.AssignFacts.auto(investigator.note()));
    c.team(clientFacts.teamOf(c.getCompanyId(), investigator.user()).orElse(null));
  }

  private static String templateOf(OpenSpec spec, boolean active) {
    if (CaseTypes.EDD.equals(spec.caseType()) || spec.requiresEdd() && active) {
      return TemplateType.EDD.name();
    }
    return CaseTypes.ACCOUNT_APPLICATION.equals(spec.caseType())
        ? TemplateType.TRANSACTION_REVIEW.name()
        : TemplateType.KYC_REVIEW.name();
  }

  private CaseVersions versions(Long companyId, OpenSpec spec, LocalDate today) {
    return new CaseVersions(
        spec.matchVersionId() != null
            ? spec.matchVersionId()
            : version(companyId, ConfigType.MATCH_CRITERIA, today),
        spec.riskVersionId() != null
            ? spec.riskVersionId()
            : version(companyId, ConfigType.RISK_RULES, today),
        version(companyId, ConfigType.APPROVAL_MATRIX, today),
        version(companyId, ConfigType.ASSIGNMENT_MATRIX, today),
        version(companyId, ConfigType.SLA_MATRIX, today),
        version(companyId, ConfigType.VALIDATION_RULES, today));
  }

  private Long version(Long companyId, ConfigType type, LocalDate today) {
    return config
        .activeVersion(companyId, type, null, today)
        .map(ConfigVersionRef::id)
        .orElse(null);
  }

  private void link(ScreeningCase c, List<Long> matchIds) {
    List<Long> uncased =
        matchIds.stream().filter(id -> matches.get(id).getCaseId() == null).toList();
    if (uncased.isEmpty()) {
      return;
    }
    matches.linkToCase(uncased, c.getId());
    for (Long id : uncased) {
      timeline.record(
          c, CaseEventType.MATCH_ADDED, EventFacts.change(null, "Match " + id, null, null));
    }
  }

  /**
   * What case to open.
   *
   * @param caseType the case type
   * @param trigger the screening trigger (or MANUAL)
   * @param reference the trigger's reference
   * @param riskCategory the risk category, may be null
   * @param requiresEdd whether the category requires EDD
   * @param matchIds the matches to link
   * @param matchVersionId the MATCH_CRITERIA version of the run, null for the one in force
   * @param riskVersionId the RISK_RULES version of the run, null for the one in force
   */
  public record OpenSpec(
      String caseType,
      String trigger,
      String reference,
      String riskCategory,
      boolean requiresEdd,
      List<Long> matchIds,
      Long matchVersionId,
      Long riskVersionId) {

    /** Defensive copy. */
    public OpenSpec {
      matchIds = matchIds == null ? List.of() : List.copyOf(matchIds);
    }
  }

  /**
   * A case opened or joined.
   *
   * @param screeningCase the case
   * @param created true for a new case
   */
  public record Opened(ScreeningCase screeningCase, boolean created) {}
}
