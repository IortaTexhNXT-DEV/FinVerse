package com.iortatechnxt.brokerverse.submitted.masterlist.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyOrigin;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmSourceRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTracking;
import com.iortatechnxt.brokerverse.submitted.service.SbmHistoryService;
import com.iortatechnxt.brokerverse.submitted.service.SbmPolicyChecks;
import com.iortatechnxt.brokerverse.submitted.service.SbmPolicyFlow;
import com.iortatechnxt.brokerverse.submitted.service.SbmScopeService;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Submitted Masterlist (BRIDSP-03, 04, 29; FRS FR-SP-003, 010, 012): manual entry and edit, the
 * create-or-update of a record on its natural key (intake, extraction, migration), the manual
 * renewal tag, the tracking fields and the handlers' actions (dispose, exclude, reinstate, close).
 * Every change writes the field history and the audit trail.
 */
@SuppressWarnings("PMD.GodClass") // the record operations of the masterlist behind one scope check
@Service
@Transactional
public class MasterlistService {

  /** Parameter: prefix of the masterlist number. */
  public static final String NUMBER_PREFIX = "SBM_NUMBER_PREFIX";

  private static final String MAINTAIN = "SBM_MAINTAIN";

  private final SbmPolicyRepository policies;
  private final SbmSourceRepository sources;
  private final SbmHistoryService history;
  private final SbmPolicyFlow flow;
  private final SbmScopeService scope;
  private final LovService lovs;
  private final UserDirectory users;
  private final NotificationService notifications;
  private final DocumentNumberService numbers;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final OrganizationDirectory organization;

  /**
   * Creates the service.
   *
   * @param policies masterlist
   * @param sources source register
   * @param history field history
   * @param flow work case
   * @param scope data scope
   * @param lovs lists of values
   * @param users user directory
   * @param notifications notifications
   * @param numbers document numbers
   * @param parameters business parameters
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the masterlist
  public MasterlistService(
      SbmPolicyRepository policies,
      SbmSourceRepository sources,
      SbmHistoryService history,
      SbmPolicyFlow flow,
      SbmScopeService scope,
      LovService lovs,
      UserDirectory users,
      NotificationService notifications,
      DocumentNumberService numbers,
      SystemParameterService parameters,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      OrganizationDirectory organization) {
    this.policies = policies;
    this.sources = sources;
    this.history = history;
    this.flow = flow;
    this.scope = scope;
    this.lovs = lovs;
    this.users = users;
    this.notifications = notifications;
    this.numbers = numbers;
    this.parameters = parameters;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.organization = organization;
  }

  /**
   * A record, within the user's scope.
   *
   * @param id record
   * @return record
   */
  @Transactional(readOnly = true)
  public SbmPolicy get(Long id) {
    return scope.requireVisible(require(id));
  }

  /**
   * A record by id, whatever the scope (services, jobs).
   *
   * @param id record
   * @return record
   */
  @Transactional(readOnly = true)
  public SbmPolicy require(Long id) {
    return policies
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Submitted policy", id));
  }

  /**
   * Creates a record by hand (FR-SP-003): status VALIDATED, source MANUAL.
   *
   * @param companyId company
   * @param data policy data
   * @param tracking handler, AO, conversion status, opportunity and remarks
   * @return the record
   */
  public SbmPolicy create(Long companyId, SbmPolicyData data, SbmTracking tracking) {
    requireValid(data, SubmittedCodes.SOURCE_MANUAL);
    findSame(companyId, data)
        .ifPresent(
            same -> {
              throw new BusinessRuleException(
                  "SBM_POLICY_EXISTS",
                  "Policy "
                      + label(data)
                      + " is already in the masterlist ("
                      + same.getSbmNo()
                      + ")");
            });
    SbmPolicy p =
        newRecord(
            companyId,
            data,
            new SbmPolicyOrigin(
                SubmittedCodes.SOURCE_MANUAL,
                null,
                BusinessClock.today(clock),
                SbmPolicyStatus.VALIDATED));
    if (tracking != null) {
      requireHandler(tracking.handlerUsername());
      p.track(tracking);
    }
    return started(p, SbmHistorySource.MANUAL, null);
  }

  /**
   * Creates a record, or updates the one with the same natural key (intake, extraction, migration;
   * BRIDSP-01 R2). A record already in the renewal is not changed (DUPLICATE).
   *
   * @param companyId company
   * @param data policy data
   * @param context source, run, date received, first status and history source
   * @return the record and what happened
   */
  public Upserted upsert(Long companyId, SbmPolicyData data, UpsertContext context) {
    Optional<SbmPolicy> same = findSame(companyId, data);
    if (same.isEmpty()) {
      SbmPolicy p = newRecord(companyId, data, context.origin());
      if (context.handler() != null) {
        p.track(new SbmTracking(context.handler(), null, null, null, null));
      }
      return new Upserted(started(p, context.source(), context.reference()), Outcome.CREATED);
    }
    SbmPolicy p = same.get();
    if (!p.getStatus().isProcessable()) {
      return new Upserted(p, Outcome.DUPLICATE);
    }
    Map<String, String> before = history.snapshot(p);
    p.apply(inBaseCurrency(p.getCompanyId(), data));
    int changed = history.record(p, before, context.source(), context.reference());
    if (changed > 0) {
      flow.describe(p);
    }
    return new Upserted(p, changed > 0 ? Outcome.UPDATED : Outcome.DUPLICATE);
  }

  /**
   * Edits the data of a record (FR-SP-003); a received record becomes VALIDATED.
   *
   * @param id record
   * @param data policy data
   * @return the record
   */
  public SbmPolicy update(Long id, SbmPolicyData data) {
    SbmPolicy p = get(id);
    requireOpenForEdit(p);
    requireValid(data, SubmittedCodes.SOURCE_MANUAL);
    findSame(p.getCompanyId(), data)
        .filter(other -> !other.getId().equals(p.getId()))
        .ifPresent(
            other -> {
              throw new BusinessRuleException(
                  "SBM_POLICY_EXISTS",
                  "Policy "
                      + label(data)
                      + " is already in the masterlist ("
                      + other.getSbmNo()
                      + ")");
            });
    write(p, data, SbmHistorySource.MANUAL, null);
    audit.record(SubmittedCodes.ENTITY, p.getSbmNo(), AuditAction.UPDATE, "Policy details edited");
    return p;
  }

  /**
   * Writes confirmed or edited data to a record and validates a received record.
   *
   * @param p record
   * @param data policy data
   * @param source what changed it
   * @param reference reference of the change, may be null
   */
  public void write(SbmPolicy p, SbmPolicyData data, SbmHistorySource source, String reference) {
    Map<String, String> before = history.snapshot(p);
    p.apply(inBaseCurrency(p.getCompanyId(), data));
    history.record(p, before, source, reference);
    flow.describe(p);
    if (p.getStatus() == SbmPolicyStatus.RECEIVED) {
      flow.act(p, "validate", TransitionNote.NONE);
    }
  }

  /**
   * Tags the renewal opportunity by hand (FR-SP-003); a Non-Renewable tag needs a reason.
   *
   * @param id record
   * @param tag RENEWABLE or NON_RENEWABLE
   * @param reason reason (LOV SBM_NON_RENEWAL_REASON), required for NON_RENEWABLE
   * @return the record
   */
  public SbmPolicy tag(Long id, String tag, String reason) {
    SbmPolicy p = get(id);
    if (!SubmittedCodes.RENEWABLE.equals(tag) && !SubmittedCodes.NON_RENEWABLE.equals(tag)) {
      throw new BusinessRuleException("SBM_TAG_INVALID", "Select Renewable or Non-Renewable");
    }
    String clean = reason == null || reason.isBlank() ? null : reason.strip();
    requireReason(tag, clean);
    Map<String, String> before = history.snapshot(p);
    p.tagManually(tag, clean, currentUser.username(), clock.instant());
    history.record(p, before, SbmHistorySource.MANUAL, null);
    audit.record(
        SubmittedCodes.ENTITY,
        p.getSbmNo(),
        AuditAction.UPDATE,
        "Tagged " + (SubmittedCodes.RENEWABLE.equals(tag) ? "Renewable" : "Non-Renewable"));
    return p;
  }

  private void requireReason(String tag, String reason) {
    if (!SubmittedCodes.NON_RENEWABLE.equals(tag)) {
      return;
    }
    if (reason == null) {
      throw new BusinessRuleException(
          "SBM_TAG_REASON_REQUIRED", "Select the reason for Non-Renewable");
    }
    lovs.requireValid(SubmittedCodes.LOV_NON_RENEWAL, reason, BusinessClock.today(clock));
  }

  /**
   * Updates the tracking fields (FR-SP-012); a new handler is notified.
   *
   * @param id record
   * @param tracking handler, AO, conversion status, opportunity and remarks
   * @return the record
   */
  public SbmPolicy track(Long id, SbmTracking tracking) {
    SbmPolicy p = get(id);
    requireHandler(tracking.handlerUsername());
    if (tracking.conversionStatus() != null && !tracking.conversionStatus().isBlank()) {
      lovs.requireValid(
          SubmittedCodes.LOV_CONVERSION, tracking.conversionStatus(), BusinessClock.today(clock));
    }
    String previousHandler = p.getHandlerUsername();
    Map<String, String> before = history.snapshot(p);
    p.track(tracking);
    history.record(p, before, SbmHistorySource.MANUAL, null);
    if (!CurrentUser.sameUser(previousHandler, tracking.handlerUsername())) {
      flow.assign(p, tracking.handlerUsername());
      notifyHandler(p);
    }
    return p;
  }

  /**
   * Assigns a handler to several records (FR-SP-012 Assign Handler).
   *
   * @param companyId company
   * @param ids records
   * @param handler handler (active user with SBM_MAINTAIN)
   * @return records assigned
   */
  public int assign(Long companyId, List<Long> ids, String handler) {
    requireHandler(handler);
    if (handler == null || handler.isBlank()) {
      throw new BusinessRuleException("SBM_HANDLER_REQUIRED", "Select the handler");
    }
    int done = 0;
    for (Long id : ids) {
      SbmPolicy p = get(id);
      if (!p.getCompanyId().equals(companyId)
          || CurrentUser.sameUser(p.getHandlerUsername(), handler)) {
        continue;
      }
      SbmTracking t = p.tracking();
      Map<String, String> before = history.snapshot(p);
      p.track(
          new SbmTracking(
              handler, t.aoUsername(), t.conversionStatus(), t.opportunityTag(), t.remarks()));
      history.record(p, before, SbmHistorySource.MANUAL, null);
      flow.assign(p, handler);
      done++;
    }
    if (done > 0) {
      notifications.notifyUser(
          handler,
          new Notice(
              done + " submitted policies assigned to you",
              "Open the masterlist to work on them.",
              "/submitted/masterlist",
              SubmittedCodes.ENTITY,
              null),
          SubmittedCodes.EVT_HANDLER_ASSIGNED);
    }
    return done;
  }

  /**
   * A handler's action on a record (dispose for renewal, exclude, reinstate, close; FR-SP-031,
   * 033). The workflow checks the permission and the reason of the action.
   *
   * @param id record
   * @param action action code
   * @param reasonCode reason
   * @param comment comment, may be null
   * @return the record
   */
  public SbmPolicy act(Long id, String action, String reasonCode, String comment) {
    SbmPolicy p = get(id);
    Map<String, String> before = history.snapshot(p);
    flow.act(p, action, new TransitionNote(blankToNull(reasonCode), blankToNull(comment)));
    if ("exclude".equals(action)) {
      p.tagManually(
          SubmittedCodes.NON_RENEWABLE, reasonCode, currentUser.username(), clock.instant());
    } else if ("dispose".equals(action) || "reinstate".equals(action)) {
      p.fallout(null, p.getLastRunNo());
    }
    history.record(p, before, SbmHistorySource.MANUAL, null);
    audit.record(
        SubmittedCodes.ENTITY, p.getSbmNo(), AuditAction.UPDATE, action + " " + reasonCode);
    return p;
  }

  private void requireOpenForEdit(SbmPolicy p) {
    if (!p.getStatus().isProcessable()) {
      throw new BusinessRuleException(
          "SBM_POLICY_IN_RENEWAL",
          "Policy " + p.getSbmNo() + " is in the renewal; its details are kept as handed over");
    }
  }

  private void requireValid(SbmPolicyData data, String sourceCode) {
    lovs.requireValid(SubmittedCodes.LOV_SEGMENT, data.segment(), BusinessClock.today(clock));
    List<String> mandatory =
        sources.findByCode(sourceCode).map(s -> s.mandatory()).orElse(List.of());
    List<String> problems = SbmPolicyChecks.problems(data, mandatory);
    if (!problems.isEmpty()) {
      throw new BusinessRuleException("SBM_POLICY_INCOMPLETE", String.join("; ", problems));
    }
  }

  private void requireHandler(String handler) {
    if (handler == null || handler.isBlank()) {
      return;
    }
    if (!users.usersWithPermission(MAINTAIN).contains(handler)) {
      throw new BusinessRuleException(
          "SBM_HANDLER_NOT_ACTIVE",
          users.displayName(handler) + " is not an active user of Submitted Policies");
    }
  }

  private Optional<SbmPolicy> findSame(Long companyId, SbmPolicyData data) {
    return policies.findByCompanyIdAndSegmentAndBusinessTypeAndNaturalKeyAndTermsExpiryDate(
        companyId,
        data.segment(),
        data.businessType(),
        SbmPolicy.naturalKey(data.loan().pnNo(), data.terms().policyNo()),
        data.terms().expiryDate());
  }

  private SbmPolicy newRecord(Long companyId, SbmPolicyData data, SbmPolicyOrigin origin) {
    String prefix = parameters.text(NUMBER_PREFIX, "SBM");
    String no = numbers.next(prefix + "-" + BusinessClock.today(clock).getYear());
    return new SbmPolicy(companyId, no, inBaseCurrency(companyId, data), origin);
  }

  private SbmPolicyData inBaseCurrency(Long companyId, SbmPolicyData data) {
    return data.orInCurrency(organization.company(companyId).baseCurrency());
  }

  private SbmPolicy started(SbmPolicy p, SbmHistorySource source, String reference) {
    SbmPolicy saved = policies.save(p);
    flow.start(saved);
    history.record(saved, Map.of(), source, reference);
    audit.record(
        SubmittedCodes.ENTITY,
        saved.getSbmNo(),
        AuditAction.CREATE,
        "Masterlist record from " + saved.getSourceCode());
    return saved;
  }

  private void notifyHandler(SbmPolicy p) {
    if (p.getHandlerUsername() == null) {
      return;
    }
    notifications.notifyUser(
        p.getHandlerUsername(),
        new Notice(
            "Submitted policy " + p.getSbmNo() + " assigned to you",
            p.getAssured().assuredName(),
            SubmittedCodes.link(p.getId()),
            SubmittedCodes.ENTITY,
            p.getId().toString()),
        SubmittedCodes.EVT_HANDLER_ASSIGNED);
  }

  private static String label(SbmPolicyData data) {
    return data.terms().policyNo() != null ? data.terms().policyNo() : data.loan().pnNo();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /** What an upsert did. */
  public enum Outcome {
    /** A new record. */
    CREATED,
    /** An existing record changed. */
    UPDATED,
    /** Nothing changed (same data, or the record is already in the renewal). */
    DUPLICATE
  }

  /**
   * The result of an upsert.
   *
   * @param policy record
   * @param outcome what happened
   */
  public record Upserted(SbmPolicy policy, Outcome outcome) {}

  /**
   * Context of an upsert.
   *
   * @param origin source, run, date received and first status of a new record
   * @param source history source
   * @param reference reference of the change (intake run, extraction, legacy file)
   * @param handler handler of a new record, may be null
   */
  public record UpsertContext(
      SbmPolicyOrigin origin, SbmHistorySource source, String reference, String handler) {

    /**
     * A context without handler.
     *
     * @param sourceCode source
     * @param intakeRunId intake run, may be null
     * @param dateReceived date received
     * @param status first status
     * @param source history source
     * @param reference reference
     * @return context
     */
    @SuppressWarnings("java:S107") // the facts of a new record
    public static UpsertContext of(
        String sourceCode,
        Long intakeRunId,
        LocalDate dateReceived,
        SbmPolicyStatus status,
        SbmHistorySource source,
        String reference) {
      return new UpsertContext(
          new SbmPolicyOrigin(sourceCode, intakeRunId, dateReceived, status),
          source,
          reference,
          null);
    }
  }
}
