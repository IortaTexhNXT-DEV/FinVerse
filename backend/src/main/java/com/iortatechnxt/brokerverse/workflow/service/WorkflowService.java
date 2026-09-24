package com.iortatechnxt.brokerverse.workflow.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.domain.StageChange;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseHistory;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseHistoryRepository;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowStage;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowTransition;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves business records through their workflow (BRNB.022/115): every change is validated against
 * the transition table, checked against the user's permissions, time-stamped in the status history
 * with the responsible user (or flagged as a system action), audited, and published as {@link
 * WorkCaseTransitioned}.
 *
 * <p>Assignment rule: a case that returns to the stage team of its first stage (Marketing) is
 * assigned back to its originator; a case moving to another team's stage waits unassigned in that
 * team's queue until someone claims it or a team leader assigns it ({@link WorkAssignmentService},
 * BRNB.080). The originator is notified of every change made by someone else (BRNB.015). Reads are
 * in {@link WorkflowViewService}.
 */
@Service
@Transactional
public class WorkflowService {

  /** Audit entity type of work cases. */
  public static final String ENTITY = "WorkCase";

  private final WorkCaseRepository cases;
  private final WorkCaseHistoryRepository history;
  private final WorkflowDefinitions definitions;
  private final LovService lovs;
  private final NotificationService notifications;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param history status history
   * @param definitions stage and transition definitions
   * @param lovs lists of values (reasons)
   * @param notifications in-app notifications
   * @param events event publisher
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public WorkflowService(
      WorkCaseRepository cases,
      WorkCaseHistoryRepository history,
      WorkflowDefinitions definitions,
      LovService lovs,
      NotificationService notifications,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.cases = cases;
    this.history = history;
    this.definitions = definitions;
    this.lovs = lovs;
    this.notifications = notifications;
    this.events = events;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Opens the case of a new record in the workflow's initial stage (or the given stage).
   *
   * @param start company, workflow, record and optional first stage
   * @return the case
   */
  public WorkCase start(StartCase start) {
    var record = start.record();
    if (cases.findByEntityTypeAndEntityId(record.entityType(), record.entityId()).isPresent()) {
      throw new DuplicateResourceException(record.entityType(), record.entityId());
    }
    WorkflowStage first =
        start.stageCode() == null
            ? definitions.initialStage(start.workflowCode())
            : definitions.stage(start.workflowCode(), start.stageCode());
    WorkCase workCase =
        cases.save(
            new WorkCase(start.companyId(), start.workflowCode(), record, first, clock.instant()));
    if (first.getOwnerPermission() != null
        && first
            .getOwnerPermission()
            .equals(definitions.initialStage(start.workflowCode()).getOwnerPermission())) {
      workCase.assignTo(currentUser.optionalUsername().orElse(null));
    }
    history.save(
        new WorkCaseHistory(
            workCase.getId(),
            new StageChange(null, first.getStageCode(), "start", null, null),
            currentUser.username(),
            currentUser.optionalUsername().isEmpty(),
            clock.instant()));
    audit.record(
        ENTITY, workCase.getReference(), AuditAction.CREATE, "Opened in " + first.getName());
    return workCase;
  }

  /**
   * A user action: the transition must exist from the current stage and the user must hold one of
   * its permissions.
   *
   * @param entityType record type
   * @param entityId record id
   * @param action action code
   * @param note reason and comment
   * @return the case in its new stage
   */
  public WorkCase transition(
      String entityType, String entityId, String action, TransitionNote note) {
    WorkCase workCase = requireCase(entityType, entityId);
    WorkflowTransition t = definitions.requireTransition(workCase, action);
    if (!t.allowedFor(currentUser::hasAuthority)) {
      throw new BusinessRuleException(
          "WORKFLOW_ACTION_NOT_PERMITTED", "You are not allowed to '" + t.getLabel() + "'");
    }
    return move(workCase, t, note, false);
  }

  /**
   * A system action (automatic step, e.g. booking after the e-policy is received): no permission
   * check, flagged as automatic in the history.
   *
   * @param entityType record type
   * @param entityId record id
   * @param action action code
   * @param note reason and comment
   * @return the case in its new stage
   */
  public WorkCase systemTransition(
      String entityType, String entityId, String action, TransitionNote note) {
    WorkCase workCase = requireCase(entityType, entityId);
    return move(workCase, definitions.requireTransition(workCase, action), note, true);
  }

  /**
   * A generic action from the workflow API (return, void...): only transitions flagged generic.
   *
   * @param caseId case
   * @param action action code
   * @param note reason and comment
   * @return the case in its new stage
   */
  public WorkCase genericTransition(Long caseId, String action, TransitionNote note) {
    WorkCase workCase =
        cases.findById(caseId).orElseThrow(() -> new ResourceNotFoundException(ENTITY, caseId));
    WorkflowTransition t = definitions.requireTransition(workCase, action);
    if (!t.isGeneric()) {
      throw new BusinessRuleException(
          "WORKFLOW_ACTION_NOT_GENERIC",
          "'" + t.getLabel() + "' must be done from the record screen");
    }
    return transition(workCase.getEntityType(), workCase.getEntityId(), action, note);
  }

  private WorkCase move(
      WorkCase workCase, WorkflowTransition t, TransitionNote note, boolean automatic) {
    TransitionNote given = note == null ? TransitionNote.NONE : note;
    validateReason(t, given);
    String from = workCase.getStageCode();
    WorkflowStage target = definitions.stage(workCase.getWorkflowCode(), t.getToStage());
    workCase.enter(target, clock.instant());
    workCase.assignTo(assigneeAfterMove(workCase, target));
    history.save(
        new WorkCaseHistory(
            workCase.getId(),
            new StageChange(
                from, target.getStageCode(), t.getAction(), given.reasonCode(), given.comment()),
            currentUser.username(),
            automatic,
            clock.instant()));
    audit.record(
        ENTITY,
        workCase.getReference(),
        AuditAction.UPDATE,
        t.getLabel() + ": " + from + " -> " + target.getStageCode() + reasonText(given));
    notifyChange(workCase, target, given);
    events.publishEvent(
        new WorkCaseTransitioned(
            workCase.getId(),
            workCase.getWorkflowCode(),
            workCase.getEntityType(),
            workCase.getEntityId(),
            from,
            target.getStageCode(),
            t.getAction(),
            given.reasonCode(),
            given.comment()));
    return workCase;
  }

  private void validateReason(WorkflowTransition t, TransitionNote note) {
    if (t.getReasonLov() == null) {
      return;
    }
    if (note.reasonCode() == null || note.reasonCode().isBlank()) {
      throw new BusinessRuleException(
          "WORKFLOW_REASON_REQUIRED", "Select a reason for '" + t.getLabel() + "'");
    }
    lovs.requireValid(t.getReasonLov(), note.reasonCode(), LocalDate.now(clock));
  }

  private String assigneeAfterMove(WorkCase workCase, WorkflowStage target) {
    if (target.isTerminal() || target.getOwnerPermission() == null) {
      return null;
    }
    WorkflowStage first = definitions.initialStage(workCase.getWorkflowCode());
    return target.getOwnerPermission().equals(first.getOwnerPermission())
        ? workCase.getCreatedBy()
        : null;
  }

  private void notifyChange(WorkCase workCase, WorkflowStage target, TransitionNote note) {
    String title = workCase.getReference() + ": " + target.getName();
    String body = workCase.getTitle() + (note.comment() == null ? "" : " - " + note.comment());
    Notice notice =
        new Notice(
            title, body, workCase.getLink(), workCase.getEntityType(), workCase.getEntityId());
    String actor = currentUser.username();
    String originator = workCase.getCreatedBy();
    if (!CurrentUser.sameUser(originator, actor)) {
      notifications.notifyUser(originator, notice);
    }
    String assignee = workCase.getAssignee();
    if (assignee != null
        && !CurrentUser.sameUser(assignee, actor)
        && !CurrentUser.sameUser(assignee, originator)) {
      notifications.notifyUser(assignee, notice);
    }
  }

  private static String reasonText(TransitionNote note) {
    StringBuilder sb = new StringBuilder();
    if (note.reasonCode() != null) {
      sb.append(" (").append(note.reasonCode()).append(')');
    }
    if (note.comment() != null && !note.comment().isBlank()) {
      sb.append(" - ").append(note.comment());
    }
    return sb.toString();
  }

  /**
   * Updates the reference or title shown in queues after the record changed.
   *
   * @param entityType record type
   * @param entityId record id
   * @param reference reference
   * @param title title
   */
  public void describe(String entityType, String entityId, String reference, String title) {
    requireCase(entityType, entityId).describe(reference, title);
  }

  private WorkCase requireCase(String entityType, String entityId) {
    return cases
        .findByEntityTypeAndEntityId(entityType, entityId)
        .orElseThrow(() -> new ResourceNotFoundException(entityType + " work item", entityId));
  }
}
