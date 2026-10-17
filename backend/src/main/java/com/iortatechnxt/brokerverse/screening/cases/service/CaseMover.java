package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves a case through workflow {@code SCR_CASE} and assigns it (SNSRP-401, 404): every stage
 * change is a workflow transition inside the caller's transaction (the stage is mirrored by {@link
 * CaseStageMirror}); the assignee is set on the case and on the work item, and recorded on the
 * timeline.
 */
@Component
@Transactional
public class CaseMover {

  private final WorkflowService workflow;
  private final WorkAssignmentService assignments;
  private final RoleMembers members;
  private final CaseRouter router;
  private final CaseTimeline timeline;

  /**
   * Creates the mover.
   *
   * @param workflow workflow
   * @param assignments work assignment
   * @param members permission holders
   * @param router eligibility
   * @param timeline case timeline
   */
  public CaseMover(
      WorkflowService workflow,
      WorkAssignmentService assignments,
      RoleMembers members,
      CaseRouter router,
      CaseTimeline timeline) {
    this.workflow = workflow;
    this.assignments = assignments;
    this.members = members;
    this.router = router;
    this.timeline = timeline;
  }

  /**
   * A user action of the workflow (the user's permission is checked by the workflow).
   *
   * @param c the case
   * @param action the action code
   * @param note reason and comment
   */
  public void act(ScreeningCase c, String action, TransitionNote note) {
    workflow.transition(CaseCodes.ENTITY, String.valueOf(c.getId()), action, note);
  }

  /**
   * A system action of the workflow (route, committee finalisation).
   *
   * @param c the case
   * @param action the action code
   * @param note reason and comment
   */
  public void system(ScreeningCase c, String action, TransitionNote note) {
    workflow.systemTransition(CaseCodes.ENTITY, String.valueOf(c.getId()), action, note);
  }

  /**
   * Assigns the case in its current stage, or leaves it in the stage queue.
   *
   * @param c the case
   * @param user the assignee, null for the stage queue
   * @param event ASSIGNED or REASSIGNED
   * @param facts reason and remarks of the timeline entry
   */
  public void assign(ScreeningCase c, String user, CaseEventType event, AssignFacts facts) {
    String previous = c.getAssignee();
    c.assignTo(user);
    if (c.getWorkCaseId() != null && CaseAccess.ownerOf(c.getStage()) != null) {
      assignments.assign(
          c.getWorkCaseId(), user, eligible(c, user), facts.reasonCode(), facts.comment());
    }
    timeline.record(
        c,
        event,
        EventFacts.change(
            previous, user == null ? "queue" : user, facts.reasonCode(), facts.remarks()));
  }

  private List<String> eligible(ScreeningCase c, String user) {
    List<String> eligible =
        new ArrayList<>(members.withPermission(CaseAccess.ownerOf(c.getStage())));
    if (user != null && !router.eligible(c, user, CaseAccess.ownerOf(c.getStage()))) {
      eligible.remove(user);
    }
    return eligible;
  }

  /**
   * Reason and remarks of an assignment.
   *
   * @param reasonCode the reason (re-assignment), may be null
   * @param comment the comment kept in the workflow history, may be null
   * @param remarks the remarks of the timeline entry
   */
  public record AssignFacts(String reasonCode, String comment, String remarks) {

    /**
     * An automatic assignment.
     *
     * @param remarks how the assignee was chosen
     * @return facts
     */
    public static AssignFacts auto(String remarks) {
      return new AssignFacts(null, null, remarks);
    }
  }
}
