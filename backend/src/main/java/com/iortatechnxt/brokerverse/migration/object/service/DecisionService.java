package com.iortatechnxt.brokerverse.migration.object.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationWorkflow;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import com.iortatechnxt.brokerverse.migration.object.domain.MigObjectDecision;
import com.iortatechnxt.brokerverse.migration.object.domain.MigObjectDecisionRepository;
import com.iortatechnxt.brokerverse.migration.object.domain.MigrationClass;
import com.iortatechnxt.brokerverse.migration.signoff.domain.Gate;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoff;
import com.iortatechnxt.brokerverse.migration.signoff.service.GateRecorder;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decisions of the class of a data object (gate G1; BRID 1.1a; FR-DM-002): the Data Migration Lead
 * submits the proposal with its criteria; the business owner of the object, never the submitter,
 * approves or returns it. The approved class is the one the loads enforce. A change away from
 * MIGRATE or CARRY_FORWARD is refused while a batch of the object is loaded and not rolled back.
 */
@Service
@Transactional
public class DecisionService {

  private static final EnumSet<BatchStatus> HOLDING_DATA =
      EnumSet.of(
          BatchStatus.LOADING,
          BatchStatus.LOADED,
          BatchStatus.LOADED_WITH_REJECTS,
          BatchStatus.RECONCILED,
          BatchStatus.SIGNED_OFF,
          BatchStatus.ROLLBACK_REQUESTED,
          BatchStatus.FAILED);

  private final ObjectRegisterService register;
  private final MigObjectDecisionRepository decisions;
  private final MigBatchRepository batches;
  private final DocumentNumberService numbers;
  private final MigrationWorkflow workflow;
  private final GateRecorder gates;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param register register
   * @param decisions decisions
   * @param batches batches (loaded data check)
   * @param numbers document numbers
   * @param workflow workflow cases
   * @param gates gate sign-offs
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public DecisionService(
      ObjectRegisterService register,
      MigObjectDecisionRepository decisions,
      MigBatchRepository batches,
      DocumentNumberService numbers,
      MigrationWorkflow workflow,
      GateRecorder gates,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.register = register;
    this.decisions = decisions;
    this.batches = batches;
    this.numbers = numbers;
    this.workflow = workflow;
    this.gates = gates;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Submits the proposed class of an object to its business owner.
   *
   * @param companyId company
   * @param code object
   * @param conditionMet the condition of a conditional class is stated as met
   * @return the decision
   */
  public MigObjectDecision submit(Long companyId, String code, boolean conditionMet) {
    MigDataObject object = register.get(code);
    if (object.getRationale() == null || object.getRationale().isBlank()) {
      throw new BusinessRuleException(
          "MIG_RATIONALE_REQUIRED", "Enter the rationale for the proposed class");
    }
    object.submitted();
    String no = numbers.next("MGD-" + BusinessClock.today(clock).getYear());
    MigObjectDecision decision =
        decisions.save(
            new MigObjectDecision(
                companyId,
                no,
                object,
                conditionMet,
                criteriaText(object),
                currentUser.username(),
                clock.instant()));
    workflow.open(
        companyId,
        MigrationCodes.WF_DECISION,
        new CaseRecord(
            MigrationCodes.ENTITY_DECISION,
            String.valueOf(decision.getId()),
            no,
            object.getCode() + " " + object.getName() + ": " + object.getProposedClass(),
            "/migration/objects?object=" + object.getCode(),
            null));
    audit.record(
        MigrationCodes.ENTITY_DECISION,
        no,
        AuditAction.SUBMIT,
        object.getCode() + " proposed as " + object.getProposedClass());
    return decision;
  }

  private static String criteriaText(MigDataObject o) {
    MigDataObject.Criteria c = o.criteria();
    return "Day-1 need: "
        + yesNo(c.day1Need(), c.day1Note())
        + "; compliance need: "
        + yesNo(c.complianceNeed(), c.complianceNote())
        + "; read-only / archival option: "
        + yesNo(c.archivalOption(), c.archivalNote())
        + "; data trust: "
        + c.dataTrust();
  }

  private static String yesNo(boolean value, String note) {
    return (value ? "Yes" : "No") + (note == null || note.isBlank() ? "" : " (" + note + ")");
  }

  /**
   * Approves a decision (gate G1).
   *
   * @param decisionNo decision
   * @param comment comment
   * @return the decision
   */
  public MigObjectDecision approve(String decisionNo, String comment) {
    MigObjectDecision decision = get(decisionNo);
    MigDataObject object = register.get(decision.getObjectCode());
    String user = currentUser.username();
    requireBusinessOwner(object, user);
    requireClassChangeAllowed(decision, object);
    decision.approve(user, clock.instant());
    object.decide(decision.getProposedClass(), decision.isConditionMet(), user, clock.instant());
    workflow.move(
        MigrationCodes.ENTITY_DECISION, String.valueOf(decision.getId()), "approve", comment);
    gates.record(
        decision.getCompanyId(),
        new MigSignoff.Scope(object.getCode(), null, null),
        Gate.G1,
        new MigSignoff.Signer("DATA_OWNER", user),
        true,
        "Class " + decision.getProposedClass() + (comment == null ? "" : ": " + comment));
    audit.record(
        MigrationCodes.ENTITY_DECISION,
        decisionNo,
        AuditAction.AUTHORIZE,
        object.getCode() + " decided as " + decision.getProposedClass());
    return decision;
  }

  /**
   * Returns a decision to the Data Migration Lead.
   *
   * @param decisionNo decision
   * @param reason reason
   * @return the decision
   */
  public MigObjectDecision returnDecision(String decisionNo, String reason) {
    MigObjectDecision decision = get(decisionNo);
    MigDataObject object = register.get(decision.getObjectCode());
    String user = currentUser.username();
    requireBusinessOwner(object, user);
    decision.returnWith(user, reason, clock.instant());
    object.returned();
    workflow.move(
        MigrationCodes.ENTITY_DECISION, String.valueOf(decision.getId()), "return", reason);
    audit.record(MigrationCodes.ENTITY_DECISION, decisionNo, AuditAction.REJECT, reason);
    return decision;
  }

  private static void requireBusinessOwner(MigDataObject object, String user) {
    if (object.getBusinessOwner() != null
        && !object.getBusinessOwner().isBlank()
        && !CurrentUser.sameUser(object.getBusinessOwner(), user)) {
      throw new BusinessRuleException(
          "MIG_NOT_OBJECT_OWNER",
          "Only the business owner of object " + object.getCode() + " can decide its class");
    }
  }

  private void requireClassChangeAllowed(MigObjectDecision decision, MigDataObject object) {
    MigrationClass current = object.getDecidedClass();
    boolean wasLoadable =
        current == MigrationClass.MIGRATE || current == MigrationClass.CARRY_FORWARD;
    boolean willArchive =
        decision.getProposedClass() == MigrationClass.ARCHIVE
            || decision.getProposedClass() == MigrationClass.EXCLUDED;
    if (wasLoadable
        && willArchive
        && batches.existsByCompanyIdAndObjectCodeAndStatusIn(
            decision.getCompanyId(), object.getCode(), HOLDING_DATA)) {
      throw new BusinessRuleException(
          "MIG_CLASS_CHANGE_LOADED",
          "Roll back the loaded batches before changing the class of this object");
    }
  }

  /**
   * A decision.
   *
   * @param decisionNo number
   * @return decision
   */
  @Transactional(readOnly = true)
  public MigObjectDecision get(String decisionNo) {
    return decisions
        .findByDecisionNo(decisionNo)
        .orElseThrow(
            () -> new ResourceNotFoundException(MigrationCodes.ENTITY_DECISION, decisionNo));
  }

  /**
   * The decision history of an object.
   *
   * @param code object
   * @return decisions, newest first
   */
  @Transactional(readOnly = true)
  public List<MigObjectDecision> history(String code) {
    return decisions.findByObjectCodeOrderBySubmittedAtDescIdDesc(code);
  }

  /**
   * Decisions waiting for a business owner.
   *
   * @return decisions
   */
  @Transactional(readOnly = true)
  public List<MigObjectDecision> pending() {
    return decisions.findByStatusOrderBySubmittedAtAsc(MigObjectDecision.Status.FOR_DECISION);
  }
}
