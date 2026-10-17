package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import org.springframework.stereotype.Component;

/**
 * Moves a renewal through its {@code RNW_CASE} work case (RENEWAL_DESIGN section 7.1): user actions
 * run as user transitions (permission of the transition checked, history by user), routes,
 * matching, expiry and booking as system transitions. {@link CandidateStageListener} mirrors the
 * stage on the candidate in the same transaction. The work case carries the assigned AO or PO so
 * the renewal is in their My Work queue.
 */
@Component
public class RenewalFlow {

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
  public RenewalFlow(WorkflowService workflow, WorkCaseRepository cases, CurrentUser currentUser) {
    this.workflow = workflow;
    this.cases = cases;
    this.currentUser = currentUser;
  }

  /**
   * Opens the work case of a new candidate in stage EXTRACTED.
   *
   * @param candidate saved candidate
   */
  public void start(RenewalCandidate candidate) {
    workflow.start(
        new StartCase(
            candidate.getCompanyId(),
            RenewalCodes.WORKFLOW,
            new CaseRecord(
                RenewalCodes.ENTITY,
                String.valueOf(candidate.getId()),
                candidate.getRenewalRef(),
                title(candidate),
                RenewalCodes.LINK + candidate.getRenewalRef(),
                candidate.getOwnerUnit()),
            null));
    candidate.enter(RenewalStage.EXTRACTED);
  }

  /**
   * A user action: the transition must exist from the current stage and the user must hold one of
   * its permissions; without a signed-in user (jobs, uploads committed in the background) it runs
   * as a system action.
   *
   * @param candidate candidate
   * @param action action code
   * @param note reason and comment
   */
  public void act(RenewalCandidate candidate, String action, TransitionNote note) {
    if (currentUser.optionalUsername().isPresent()) {
      workflow.transition(RenewalCodes.ENTITY, key(candidate), action, note);
    } else {
      workflow.systemTransition(RenewalCodes.ENTITY, key(candidate), action, note);
    }
  }

  /**
   * A system action (route, match, expiry, booking): no permission check, flagged automatic.
   *
   * @param candidate candidate
   * @param action action code
   * @param comment comment
   */
  public void system(RenewalCandidate candidate, String action, String comment) {
    workflow.systemTransition(
        RenewalCodes.ENTITY, key(candidate), action, TransitionNote.comment(comment));
  }

  /**
   * Puts the assignee of the work case (My Work).
   *
   * @param candidate candidate
   * @param assignee user, null for the team queue
   */
  public void assign(RenewalCandidate candidate, String assignee) {
    cases
        .findByEntityTypeAndEntityId(RenewalCodes.ENTITY, key(candidate))
        .ifPresent(c -> c.assignTo(assignee));
  }

  /**
   * Refreshes the title of the work case after the snapshot changed.
   *
   * @param candidate candidate
   */
  public void describe(RenewalCandidate candidate) {
    workflow.describe(
        RenewalCodes.ENTITY, key(candidate), candidate.getRenewalRef(), title(candidate));
  }

  private static String key(RenewalCandidate candidate) {
    return String.valueOf(candidate.getId());
  }

  private static String title(RenewalCandidate candidate) {
    String product =
        candidate.getSnapshot().product() == null
            ? null
            : candidate.getSnapshot().product().productCode();
    return candidate.getSnapshot().clientName()
        + (product == null ? "" : " - " + product)
        + " - expires "
        + candidate.getExpiryDate();
  }
}
