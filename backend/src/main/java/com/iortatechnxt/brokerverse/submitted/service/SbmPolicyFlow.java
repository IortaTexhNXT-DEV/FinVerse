package com.iortatechnxt.brokerverse.submitted.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Moves a masterlist record through its {@code SBM_POLICY} work case (SUBMITTED_POLICIES_DESIGN
 * section 7): the processing run, the expiry scan and the account and booking events move it as
 * system transitions; handlers act with user transitions (permission and reason of the transition
 * checked by the workflow engine). The status of the record mirrors the stage.
 */
@Component
public class SbmPolicyFlow {

  private final WorkflowService workflow;
  private final WorkCaseRepository cases;
  private final CurrentUser currentUser;

  /**
   * Creates the flow.
   *
   * @param workflow workflow engine
   * @param cases work cases (assignee)
   * @param currentUser current user
   */
  public SbmPolicyFlow(WorkflowService workflow, WorkCaseRepository cases, CurrentUser currentUser) {
    this.workflow = workflow;
    this.cases = cases;
    this.currentUser = currentUser;
  }

  /**
   * Opens the work case of a new record in its current status.
   *
   * @param policy saved record
   */
  public void start(SbmPolicy policy) {
    workflow.start(
        new StartCase(
            policy.getCompanyId(),
            SubmittedCodes.WORKFLOW,
            new CaseRecord(
                SubmittedCodes.ENTITY,
                key(policy),
                policy.getSbmNo(),
                title(policy),
                SubmittedCodes.link(policy.getId()),
                null),
            policy.getStatus().name()));
    assign(policy, policy.getHandlerUsername());
  }

  /**
   * A system route to a status (processing run); nothing happens when the record is already there.
   *
   * @param policy record
   * @param target status
   * @param reason reason shown in the history, may be null
   */
  public void route(SbmPolicy policy, SbmPolicyStatus target, String reason) {
    if (policy.getStatus() == target) {
      return;
    }
    workflow.systemTransition(
        SubmittedCodes.ENTITY,
        key(policy),
        "route_" + target.name().toLowerCase(Locale.ROOT),
        TransitionNote.comment(reason));
    policy.enter(target, reason);
  }

  /**
   * A user action (dispose, exclude, reinstate, renew, close, validate); without a signed-in user it
   * runs as a system action.
   *
   * @param policy record
   * @param action action code
   * @param note reason and comment
   */
  public void act(SbmPolicy policy, String action, TransitionNote note) {
    WorkCase moved =
        currentUser.optionalUsername().isPresent()
            ? workflow.transition(SubmittedCodes.ENTITY, key(policy), action, note)
            : workflow.systemTransition(SubmittedCodes.ENTITY, key(policy), action, note);
    policy.enter(SbmPolicyStatus.valueOf(moved.getStageCode()), text(note));
  }

  /**
   * A system action (hand-off, placed, booked, not renewed).
   *
   * @param policy record
   * @param action action code
   * @param comment comment
   */
  public void system(SbmPolicy policy, String action, String comment) {
    WorkCase moved =
        workflow.systemTransition(
            SubmittedCodes.ENTITY, key(policy), action, TransitionNote.comment(comment));
    policy.enter(SbmPolicyStatus.valueOf(moved.getStageCode()), comment);
  }

  /**
   * Puts the handler as the assignee of the work case (My Work).
   *
   * @param policy record
   * @param assignee user, null for the team queue
   */
  public void assign(SbmPolicy policy, String assignee) {
    cases
        .findByEntityTypeAndEntityId(SubmittedCodes.ENTITY, key(policy))
        .ifPresent(c -> c.assignTo(assignee));
  }

  /**
   * Refreshes the title of the work case after the data changed.
   *
   * @param policy record
   */
  public void describe(SbmPolicy policy) {
    workflow.describe(SubmittedCodes.ENTITY, key(policy), policy.getSbmNo(), title(policy));
  }

  private static String text(TransitionNote note) {
    if (note == null) {
      return null;
    }
    return note.comment() != null ? note.comment() : note.reasonCode();
  }

  private static String key(SbmPolicy policy) {
    return String.valueOf(policy.getId());
  }

  private static String title(SbmPolicy policy) {
    return policy.getAssured().assuredName()
        + " - expires "
        + DisplayFormat.date(policy.getTerms().expiryDate());
  }
}
