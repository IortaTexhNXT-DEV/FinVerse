package com.iortatechnxt.brokerverse.workflow.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowStage;
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

  private final WorkCaseRepository cases;
  private final WorkflowDefinitions definitions;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param definitions stage definitions
   * @param notifications in-app notifications
   * @param audit audit trail
   * @param currentUser current user
   */
  public WorkAssignmentService(
      WorkCaseRepository cases,
      WorkflowDefinitions definitions,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser) {
    this.cases = cases;
    this.definitions = definitions;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
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
    WorkCase workCase = get(caseId);
    if (assignee != null
        && eligibleUsers.stream().noneMatch(u -> CurrentUser.sameUser(u, assignee))) {
      throw new BusinessRuleException(
          "WORK_ASSIGNEE_NOT_ELIGIBLE", assignee + " does not work this queue");
    }
    return assign(workCase, assignee);
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
