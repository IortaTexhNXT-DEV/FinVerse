package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimStatusChanged;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusHistory;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusHistoryRepository;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The effects of every status or phase change of a claim, in the caller's transaction
 * (BRCLM.011/027/035; CLAIMS_BROKING_DESIGN 8.1): the status history row with the days spent in the
 * previous status, the stage of workflow {@code BCL_CLAIM}, the audit entry, the in-app notice to
 * the account officer ({@code BCL_STATUS_CHANGED}) and, after the history row, the Spring event
 * {@link ClaimStatusChanged} (also for the first status, a closure and a reopen). The callers have
 * already checked the permission and the matrix and changed the claim's progress.
 */
@Service
@Transactional
public class StatusTransitions {

  /** Notification event of a status change (design 9.4). */
  static final String STATUS_CHANGED_EVENT = "BCL_STATUS_CHANGED";

  private final StatusHistoryRepository history;
  private final ClaimWorkflow workflow;
  private final StatusRules rules;
  private final NotificationService notifications;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param history status history
   * @param workflow workflow wiring
   * @param rules status rules (labels)
   * @param notifications in-app notifications
   * @param events event publisher
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public StatusTransitions(
      StatusHistoryRepository history,
      ClaimWorkflow workflow,
      StatusRules rules,
      NotificationService notifications,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.history = history;
    this.workflow = workflow;
    this.rules = rules;
    this.notifications = notifications;
    this.events = events;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records a change the caller has just applied to the claim's progress.
   *
   * @param claim claim in its new status and phase
   * @param from status and phase before the change (nulls for the first status)
   * @param statusSinceBefore time the previous status was set, null for the first status
   * @param note remark, and the reason of a reopen
   */
  public void record(
      Claim claim, StatusHistory.Step from, Instant statusSinceBefore, TransitionNote note) {
    Instant now = clock.instant();
    String by = currentUser.username();
    StatusHistory.Step to =
        new StatusHistory.Step(claim.getProgress().getStatusCode(), claim.getProgress().getPhase());
    Integer daysInPrevious =
        statusSinceBefore == null
            ? null
            : ClaimAgeing.ageThisStage(statusSinceBefore, ClaimAgeing.dateOf(now));
    String remark = remark(note);
    history.save(
        new StatusHistory(
            claim.getId(), from, to, new StatusHistory.Change(by, now, remark, daysInPrevious)));
    workflow.sync(claim, note);
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        from.status() == null ? AuditAction.CREATE : AuditAction.UPDATE,
        describe(from, to) + (remark == null ? "" : " - " + remark));
    notifyAccountOfficer(claim, to);
    events.publishEvent(
        new ClaimStatusChanged(
            claim.getId(),
            claim.getCompanyId(),
            claim.getClaimNo(),
            from.status(),
            to.status(),
            from.phase(),
            to.phase(),
            by,
            now));
  }

  private String describe(StatusHistory.Step from, StatusHistory.Step to) {
    String target = "Status " + rules.statusLabel(to.status()) + " (" + to.phase() + ")";
    if (from.status() == null) {
      return target;
    }
    return target + " from " + rules.statusLabel(from.status()) + " (" + from.phase() + ")";
  }

  private void notifyAccountOfficer(Claim claim, StatusHistory.Step to) {
    String officer = claim.getCover().getAccountOfficer();
    if (officer == null || CurrentUser.sameUser(officer, currentUser.username())) {
      return;
    }
    String what = to.phase() == ClaimPhase.CLOSED ? "Closed" : rules.statusLabel(to.status());
    notifications.notifyUser(
        officer,
        new Notice(
            claim.getClaimNo() + ": " + what,
            "Claim of "
                + claim.getCover().getAssuredName()
                + " ("
                + claim.getCover().getArn()
                + ")",
            "/claims-handling/" + claim.getId(),
            ClaimCodes.ENTITY_TYPE,
            String.valueOf(claim.getId())),
        STATUS_CHANGED_EVENT);
  }

  private static String remark(TransitionNote note) {
    if (note == null) {
      return null;
    }
    String comment = note.comment() == null || note.comment().isBlank() ? null : note.comment();
    if (note.reasonCode() == null) {
      return comment;
    }
    return note.reasonCode() + (comment == null ? "" : ": " + comment);
  }
}
