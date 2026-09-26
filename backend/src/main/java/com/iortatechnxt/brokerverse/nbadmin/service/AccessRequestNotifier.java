package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessUserType;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Notifications of the access request lifecycle (FR-UA-070; notification events of
 * USER_ACCESS_DESIGN section 8): the chosen approver, the second approvers, the System
 * Administrators, the requester and the user whose access changed (in the app and by e-mail).
 */
@Component
public class AccessRequestNotifier {

  /** Event: a request waits for the approver. */
  public static final String TO_APPROVE = "UAM_REQUEST_TO_APPROVE";

  private static final String ACCESS_REQUEST = "Access request ";

  private final NotificationService notifications;
  private final MessageService messages;
  private final AppUserRepository users;

  /**
   * Creates the notifier.
   *
   * @param notifications in-app notifications
   * @param messages e-mail
   * @param users users (e-mail of the affected user)
   */
  public AccessRequestNotifier(
      NotificationService notifications, MessageService messages, AppUserRepository users) {
    this.notifications = notifications;
    this.messages = messages;
    this.users = users;
  }

  /**
   * Tells the current approver that a request waits (on submission and after each approval in
   * order); without a chosen approver every holder of the approval right is told.
   *
   * @param r request
   * @param approvalPermission permission of the approvers of the request
   */
  public void toApprove(AccessRequest r, String approvalPermission) {
    Notice notice = notice(r, "to approve", AccessRequestService.describe(r));
    if (r.getAssignedApprover() == null) {
      notifications.notifyPermission(approvalPermission, notice, TO_APPROVE);
    } else {
      notifications.notifyUser(r.getAssignedApprover(), notice, TO_APPROVE);
    }
  }

  /**
   * Tells the second approvers that a flagged request waits (UAM-NFR-40).
   *
   * @param r request
   */
  public void toSecondApprove(AccessRequest r) {
    notifications.notifyPermission(
        "UAM_SECOND_APPROVE",
        notice(r, "for second approval", AccessRequestService.describe(r) + " " + r.riskFlags()),
        "UAM_SECOND_APPROVAL");
  }

  /**
   * Tells the System Administrators that an approved group-profile request waits (BRD-11 p.6).
   *
   * @param r request
   */
  public void toImplement(AccessRequest r) {
    notifications.notifyPermission(
        "ROLE_MANAGE",
        notice(r, "to implement", AccessRequestService.describe(r)),
        "UAM_FOR_IMPLEMENTATION");
  }

  /**
   * Tells the requester the outcome (approved, scheduled, rejected, implemented...).
   *
   * @param r request
   * @param outcome outcome text
   */
  public void decided(AccessRequest r, String outcome) {
    notifications.notifyUser(
        requester(r),
        notice(
            r,
            outcome,
            AccessRequestService.describe(r) + AccessRequestService.note(r.getDecisionComment())),
        "UAM_REQUEST_DECIDED");
  }

  /**
   * Tells the requester that the request was returned, with the remarks (BRD 1.006.1.1).
   *
   * @param r request
   */
  public void returned(AccessRequest r) {
    notifications.notifyUser(
        requester(r),
        notice(r, "returned", AccessRequestService.describe(r) + " - " + r.getDecisionComment()),
        "UAM_REQUEST_RETURNED");
  }

  /**
   * Tells the approver that a request assigned to them was cancelled (BRD 1.007).
   *
   * @param r request
   * @param approver approver to tell, null for none
   */
  public void cancelled(AccessRequest r, String approver) {
    if (approver != null) {
      notifications.notifyUser(
          approver,
          notice(r, "cancelled", AccessRequestService.describe(r) + " - " + r.getCancelReason()),
          "UAM_REQUEST_CANCELLED");
    }
  }

  /**
   * Tells the user whose access changed, in the app and by e-mail (UAM-NFR-39).
   *
   * @param r applied request about an internal user
   */
  public void accessChanged(AccessRequest r) {
    if (r.getUsername() == null || r.getUserType() == AccessUserType.EXTERNAL) {
      return;
    }
    String body =
        "Your access changed: "
            + AccessRequestService.describe(r)
            + " (request "
            + r.getRequestNo()
            + ").";
    notifications.notifyUser(
        r.getUsername(),
        new Notice("Your access changed", body, "/", AccessRequestService.ENTITY, idOf(r)),
        "UAM_ACCESS_CHANGED");
    users
        .findByUsernameIgnoreCase(r.getUsername())
        .map(u -> u.getEmail())
        .filter(e -> e != null && !e.isBlank())
        .ifPresent(
            email ->
                messages.queueEmail(
                    new OutboundEmail(
                        null,
                        "ACCESS_CHANGED",
                        List.of(email),
                        List.of(),
                        "BrokerVerse: your access changed",
                        body,
                        List.of(),
                        null,
                        new RecordLink(AccessRequestService.ENTITY, idOf(r), r.getRequestNo()))));
  }

  private static Notice notice(AccessRequest r, String what, String body) {
    return new Notice(
        ACCESS_REQUEST + r.getRequestNo() + " " + what,
        body,
        AccessRequestService.link(r),
        AccessRequestService.ENTITY,
        idOf(r));
  }

  private static String idOf(AccessRequest r) {
    return String.valueOf(r.getId());
  }

  /**
   * The requester of a request (the submitter, or the creator of a draft).
   *
   * @param r request
   * @return user name
   */
  public static String requester(AccessRequest r) {
    return r.getSubmittedBy() == null ? r.getCreatedBy() : r.getSubmittedBy();
  }
}
