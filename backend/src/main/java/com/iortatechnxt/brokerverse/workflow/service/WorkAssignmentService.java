package com.iortatechnxt.brokerverse.workflow.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.domain.StageChange;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseHistory;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseHistoryRepository;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowStage;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workload assignment (BRNB.080): users claim unassigned items from the queues of their team; team
 * leaders assign or re-assign items to users of the stage's team.
 */
@Service
@Transactional
public class WorkAssignmentService {

  /** History action of a re-assignment with a reason. */
  public static final String REASSIGN = "reassign";

  private final WorkCaseRepository cases;
  private final WorkCaseHistoryRepository history;
  private final WorkflowDefinitions definitions;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param history status history
   * @param definitions stage definitions
   * @param notifications in-app notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public WorkAssignmentService(
      WorkCaseRepository cases,
      WorkCaseHistoryRepository history,
      WorkflowDefinitions definitions,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.cases = cases;
    this.history = history;
    this.definitions = definitions;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Takes an unassigned case from a queue the user works.
   *
   * @param caseId case
   * @return the case
   */
  public WorkCase claim(Long caseId) {
    WorkCase workCase = get(caseId);
    WorkflowStage stage = definitions.stageOf(workCase);
    if (!stage.isQueueStage() || !currentUser.hasAuthority(stage.getOwnerPermission())) {
      throw new BusinessRuleException("WORK_CLAIM_NOT_ALLOWED", "This item is not in your queue");
    }
    return assign(workCase, currentUser.username());
  }

  /**
   * Assigns or re-assigns a case to a user of the stage's team (team leader action).
   *
   * @param caseId case
   * @param assignee user; null releases the case to the team queue
   * @param eligibleUsers users holding the stage's permission
   * @return the case
   */
  public WorkCase assign(Long caseId, String assignee, List<String> eligibleUsers) {
    return assign(caseId, assignee, eligibleUsers, null, null);
  }

  /**
   * Assigns or re-assigns a case with a reason (e.g. SNSRP-404: workload, absence, conflict of
   * interest). When a reason or comment is given, the re-assignment is kept in the status history
   * (action {@value #REASSIGN}); without one it behaves as {@link #assign(Long, String, List)}.
   *
   * @param caseId case
   * @param assignee user; null releases the case to the team queue
   * @param eligibleUsers users holding the stage's permission
   * @param reasonCode reason code (list of values of the business module), may be null
   * @param comment remarks, may be null
   * @return the case
   */
  public WorkCase assign(
      Long caseId, String assignee, List<String> eligibleUsers, String reasonCode, String comment) {
    WorkCase workCase = get(caseId);
    if (assignee != null
        && eligibleUsers.stream().noneMatch(u -> CurrentUser.sameUser(u, assignee))) {
      throw new BusinessRuleException(
          "WORK_ASSIGNEE_NOT_ELIGIBLE", assignee + " does not work this queue");
    }
    String previous = workCase.getAssignee();
    WorkCase assigned = assign(workCase, assignee);
    if (reasonCode != null || comment != null) {
      history.save(
          new WorkCaseHistory(
              workCase.getId(),
              new StageChange(
                  workCase.getStageCode(),
                  workCase.getStageCode(),
                  REASSIGN,
                  reasonCode,
                  assignmentText(previous, assignee, comment)),
              currentUser.username(),
              false,
              clock.instant()));
    }
    return assigned;
  }

  private static String assignmentText(String previous, String assignee, String comment) {
    String text =
        (previous == null ? "" : previous + " -> ") + (assignee == null ? "team queue" : assignee);
    return comment == null || comment.isBlank() ? text : text + ": " + comment;
  }

  private WorkCase assign(WorkCase workCase, String assignee) {
    if (workCase.isClosed()) {
      throw new BusinessRuleException("WORK_CASE_CLOSED", "The item is closed");
    }
    String previous = workCase.getAssignee();
    workCase.assignTo(assignee);
    audit.record(
        WorkflowService.ENTITY,
        workCase.getReference(),
        AuditAction.UPDATE,
        "Assigned "
            + (previous == null ? "" : "from " + previous + " ")
            + "to "
            + (assignee == null ? "team queue" : assignee));
    if (assignee != null && !CurrentUser.sameUser(assignee, currentUser.username())) {
      notifications.notifyUser(
          assignee,
          new Notice(
              workCase.getReference() + " assigned to you",
              workCase.getTitle(),
              workCase.getLink(),
              workCase.getEntityType(),
              workCase.getEntityId()));
    }
    return workCase;
  }

  private WorkCase get(Long caseId) {
    return cases
        .findById(caseId)
        .orElseThrow(() -> new ResourceNotFoundException(WorkflowService.ENTITY, caseId));
  }
}
