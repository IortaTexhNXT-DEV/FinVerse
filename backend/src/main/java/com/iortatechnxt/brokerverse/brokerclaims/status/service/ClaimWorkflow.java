package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wiring of a claim to workflow {@code BCL_CLAIM} (CLAIMS_BROKING_DESIGN 8.2): the case gives the
 * My Work queue per handler, assignment and the case history; its stage mirrors the claim phase.
 * Every transition is a business action of the Claims services ({@code systemTransition} after
 * their own permission and matrix checks): {@code progress} NEW to IN_PROGRESS, {@code temp_close},
 * {@code resume} TEMP_CLOSED to IN_PROGRESS, {@code close} and {@code reopen} CLOSED to
 * IN_PROGRESS.
 *
 * <p>The workflow has no way back to NEW (CL0 as built): the status engine refuses a status of
 * phase NEW once the claim has left it ({@code BCL_STATUS_BACK_TO_NEW}), so the stage and the phase
 * never diverge. A claim without a case (recorded before the case existed, or migrated) gets its
 * case on its first status change, in the stage of its phase.
 */
@Service
@Transactional
public class ClaimWorkflow {

  private final WorkflowService workflow;
  private final WorkCaseRepository cases;

  /**
   * Creates the wiring.
   *
   * @param workflow workflow engine
   * @param cases work cases
   */
  public ClaimWorkflow(WorkflowService workflow, WorkCaseRepository cases) {
    this.workflow = workflow;
    this.cases = cases;
  }

  /**
   * The case of a claim, opened in the stage of the claim's phase when it has none.
   *
   * @param claim claim
   * @return the case
   */
  public WorkCase open(Claim claim) {
    Optional<WorkCase> existing = find(claim);
    if (existing.isPresent()) {
      return existing.get();
    }
    CaseRecord record =
        new CaseRecord(
            ClaimCodes.ENTITY_TYPE,
            key(claim),
            claim.getClaimNo(),
            title(claim),
            "/claims-handling/" + claim.getId(),
            claim.getUnitCode());
    WorkCase opened =
        workflow.start(
            new StartCase(
                claim.getCompanyId(),
                ClaimCodes.WORKFLOW,
                record,
                claim.getProgress().getPhase().stageCode()));
    opened.assignTo(claim.getHandler());
    return opened;
  }

  /**
   * Brings the case to the stage of the claim's phase: opens it when the claim has none, else runs
   * the transition from the case's stage; nothing when they already agree.
   *
   * @param claim claim (already in its new phase)
   * @param note reason (reopen) and comment
   */
  public void sync(Claim claim, TransitionNote note) {
    Optional<WorkCase> found = find(claim);
    if (found.isEmpty()) {
      open(claim);
      return;
    }
    ClaimPhase from = ClaimPhase.valueOf(found.get().getStageCode());
    ClaimPhase to = claim.getProgress().getPhase();
    if (from == to) {
      return;
    }
    WorkCase moved =
        workflow.systemTransition(ClaimCodes.ENTITY_TYPE, key(claim), action(from, to), note);
    if (to == ClaimPhase.IN_PROGRESS) {
      // The stage is worked by the claim's handler, not by the case originator.
      moved.assignTo(claim.getHandler());
    }
  }

  /**
   * Hands the case to the claim's handler after a reassignment: a stage worked by the claims
   * handlers (NEW, IN_PROGRESS) is assigned to him; the case of a closed claim stays unassigned.
   *
   * @param claim claim with its new handler
   */
  public void assign(Claim claim) {
    ClaimPhase phase = claim.getProgress().getPhase();
    if (phase == ClaimPhase.NEW || phase == ClaimPhase.IN_PROGRESS) {
      find(claim).ifPresent(c -> c.assignTo(claim.getHandler()));
    }
  }

  /**
   * The case of a claim, if any.
   *
   * @param claim claim
   * @return case
   */
  @Transactional(readOnly = true)
  public Optional<WorkCase> find(Claim claim) {
    return cases.findByEntityTypeAndEntityId(ClaimCodes.ENTITY_TYPE, key(claim));
  }

  /**
   * The transition of workflow {@code BCL_CLAIM} between two phases.
   *
   * @param from previous phase
   * @param to new phase
   * @return action code
   */
  static String action(ClaimPhase from, ClaimPhase to) {
    return switch (to) {
      case CLOSED -> "close";
      case TEMP_CLOSED -> "temp_close";
      case IN_PROGRESS -> inProgressAction(from);
      case NEW -> throw new IllegalStateException("Workflow BCL_CLAIM has no way back to NEW");
    };
  }

  private static String inProgressAction(ClaimPhase from) {
    return switch (from) {
      case TEMP_CLOSED -> "resume";
      case CLOSED -> "reopen";
      default -> "progress";
    };
  }

  private static String key(Claim claim) {
    return String.valueOf(claim.getId());
  }

  private static String title(Claim claim) {
    String assured = claim.getCover().getAssuredName();
    return (assured == null ? "" : assured + " - ") + claim.getCover().getArn();
  }
}
