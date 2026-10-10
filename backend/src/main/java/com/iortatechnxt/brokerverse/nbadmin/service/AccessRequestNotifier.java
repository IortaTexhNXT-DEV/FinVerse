package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRiskFlag;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessUserType;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Notifications of the access request lifecycle (FR-UA-070; notification events of
 * USER_ACCESS_DESIGN section 8): the chosen approver, the second approvers, the System
 * Administrators, the requester and the user whose access changed (in the app and by e-mail); the
 * members of a deactivated or reactivated group profile; the users of the dormant-user job.
 */
@Component
public class AccessRequestNotifier {

  /** Event: a request waits for the approver. */
  public static final String TO_APPROVE = "UAM_REQUEST_TO_APPROVE";

  /** Event: the user's access changed. */
  public static final String ACCESS_CHANGED = "UAM_ACCESS_CHANGED";

  /** Event: a dormant user is told to sign in before the deactivation date. */
  public static final String DORMANT_WARNING = "UAM_DORMANT_WARNING";

  /** Event: the requesters are told which dormant users were deactivated. */
  public static final String DORMANT_DEACTIVATED = "UAM_DORMANT_DEACTIVATED";

  /** Setting: the request notices are also e-mailed by preference (BDOI FRS FRUM.013.01). */
  public static final String EMAIL_COPY = "UAM_REQUEST_NOTICE_EMAIL";

  private static final String ACCESS_REQUEST = "Access request ";
  private static final String YOUR_ACCESS_CHANGED = "Your access changed";
  private static final String PROFILE = "/profile";

  private final NotificationService notifications;
  private final MessageService messages;
  private final AppUserRepository users;
  private final AccessRequestDescriber describer;
  private final NoticeDelivery delivery;
  private final SystemParameterService parameters;

  /**
   * Creates the notifier.
   *
   * @param notifications in-app notifications
   * @param messages e-mail
   * @param users users (e-mail of the affected user)
   * @param describer descriptions of the requests
   * @param delivery request notices in the system and by e-mail as each user prefers
   * @param parameters the setting of the e-mail copy ({@value #EMAIL_COPY})
   */
  public AccessRequestNotifier(
      NotificationService notifications,
      MessageService messages,
      AppUserRepository users,
      AccessRequestDescriber describer,
      NoticeDelivery delivery,
      SystemParameterService parameters) {
    this.delivery = delivery;
    this.parameters = parameters;
    this.notifications = notifications;
    this.messages = messages;
    this.users = users;
    this.describer = describer;
  }

  /**
   * Tells the current approver that a request waits (on submission and after each approval in
   * order); without a chosen approver every holder of the approval right is told.
   *
   * @param r request
   * @param approvalPermission permission of the approvers of the request
   */
  public void toApprove(AccessRequest r, String approvalPermission) {
    Notice notice = notice(r, "to approve", describer.describe(r));
    if (r.getAssignedApprover() == null) {
      toPermission(approvalPermission, notice, TO_APPROVE);
    } else {
      toUser(r.getAssignedApprover(), notice, TO_APPROVE);
    }
  }

  /**
   * Tells the second approvers that a flagged request waits (UAM-NFR-40).
   *
   * @param r request
   */
  public void toSecondApprove(AccessRequest r) {
    toPermission(
        "UAM_SECOND_APPROVE",
        notice(r, "for second approval", describer.describe(r) + " (" + riskLabels(r) + ")"),
        "UAM_SECOND_APPROVAL");
  }

  /**
   * The risk flags of a request in words, for example "privilege increase, outside working hours".
   *
   * @param r request
   * @return labels
   */
  public static String riskLabels(AccessRequest r) {
    return r.riskFlags().stream().map(AccessRiskFlag::label).collect(Collectors.joining(", "));
  }

  /**
   * Tells the System Administrators that an approved group-profile request waits (BRD-11 p.6).
   *
   * @param r request
   */
  public void toImplement(AccessRequest r) {
    toPermission(
        "ROLE_MANAGE", notice(r, "to implement", describer.describe(r)), "UAM_FOR_IMPLEMENTATION");
  }

  /**
   * Tells the requester the outcome (approved, scheduled, rejected, implemented...).
   *
   * @param r request
   * @param outcome outcome text
   */
  public void decided(AccessRequest r, String outcome) {
    toUser(
        requester(r),
        notice(
            r, outcome, describer.describe(r) + AccessRequestService.note(r.getDecisionComment())),
        "UAM_REQUEST_DECIDED");
  }

  /**
   * The outcome of an approval that applies on a later date, for example "approved, applies on
   * 27-Oct-2026".
   *
   * @param date effective date
   * @return outcome text
   */
  public static String appliesOn(LocalDate date) {
    return "approved, applies on " + DisplayFormat.date(date);
  }

  /**
   * Tells the requester that the request was returned, with the remarks (BRD 1.006.1.1).
   *
   * @param r request
   */
  public void returned(AccessRequest r) {
    toUser(
        requester(r),
        notice(r, "returned", describer.describe(r) + " - " + r.getDecisionComment()),
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
      toUser(
          approver,
          notice(r, "cancelled", describer.describe(r) + " - " + r.getCancelReason()),
          "UAM_REQUEST_CANCELLED");
    }
  }

  /**
   * Tells the user whose access changed, in the app and by e-mail (UAM-NFR-39); for the
   * deactivation or reactivation of a group profile, its members (FR-UA-042, FR-UA-043).
   *
   * @param r applied request
   */
  public void accessChanged(AccessRequest r) {
    AccessRequestType type = r.getRequestType();
    if (type == AccessRequestType.DEACTIVATE_ROLE || type == AccessRequestType.REACTIVATE_ROLE) {
      profileMembersChanged(r, users.findEnabledUsernamesWithRole(r.getRoleCode()));
    } else if (r.getUsername() != null && r.getUserType() != AccessUserType.EXTERNAL) {
      tellUser(
          r.getUsername(),
          "Your access changed: " + describer.describe(r) + " (request " + r.getRequestNo() + ").",
          r);
    }
  }

  /**
   * Tells the members of a deactivated or reactivated group profile that their access changed, in
   * the app and by e-mail (FR-UA-042, FR-UA-043).
   *
   * @param r implemented group-profile request
   * @param members user names of the members
   */
  void profileMembersChanged(AccessRequest r, List<String> members) {
    String body =
        "Your access changed: "
            + describer.describe(r)
            + ", a group profile you hold (request "
            + r.getRequestNo()
            + ").";
    members.forEach(m -> tellUser(m, body, r));
  }

  private void tellUser(String username, String body, AccessRequest r) {
    notifications.notifyUser(
        username,
        new Notice(YOUR_ACCESS_CHANGED, body, "/", AccessRequestService.ENTITY, idOf(r)),
        ACCESS_CHANGED);
    email(
        username,
        BrandAssets.SYSTEM_NAME + ": your access changed",
        body,
        "ACCESS_CHANGED",
        new RecordLink(AccessRequestService.ENTITY, idOf(r), r.getRequestNo()));
  }

  /**
   * Tells a dormant user to sign in before the deactivation date (dormant-user job).
   *
   * @param username user
   * @param deactivationDate date of the deactivation
   */
  public void dormantWarning(String username, LocalDate deactivationDate) {
    String body =
        "You have not signed in to "
            + BrandAssets.SYSTEM_NAME
            + " for a long time. Sign in before "
            + DisplayFormat.date(deactivationDate)
            + ", or your account is deactivated and a new access request is needed.";
    notifications.notifyUser(
        username,
        new Notice("Your account will be deactivated", body, PROFILE, "AppUser", username),
        DORMANT_WARNING);
    email(
        username,
        BrandAssets.SYSTEM_NAME + ": sign in to keep your account",
        body,
        DORMANT_WARNING,
        new RecordLink("AppUser", username, "Dormant account"));
  }

  /**
   * Tells the requesters of access which dormant users were deactivated (dormant-user job).
   *
   * @param deactivated user names
   * @param days dormancy period in days
   */
  public void dormantDeactivated(List<String> deactivated, int days) {
    if (deactivated.isEmpty()) {
      return;
    }
    notifications.notifyPermission(
        "ACCESS_REQUEST",
        new Notice(
            "Dormant users deactivated",
            deactivated.size()
                + " user(s) without a sign-in for "
                + days
                + " days were deactivated: "
                + String.join(", ", deactivated),
            AccessRequestService.SCREEN,
            AccessRequestService.ENTITY,
            null),
        DORMANT_DEACTIVATED);
  }

  private void email(
      String username, String subject, String body, String purpose, RecordLink link) {
    users
        .findByUsernameIgnoreCase(username)
        .map(u -> u.getEmail())
        .filter(e -> e != null && !e.isBlank())
        .ifPresent(
            address ->
                messages.queueEmail(
                    new OutboundEmail(
                        null,
                        purpose,
                        List.of(address),
                        List.of(),
                        subject,
                        body,
                        List.of(),
                        null,
                        link)));
  }

  private void toUser(String username, Notice notice, String event) {
    delivery.toUser(username, notice, event, emailCopy());
  }

  private void toPermission(String permission, Notice notice, String event) {
    delivery.toPermission(permission, notice, event, emailCopy());
  }

  private boolean emailCopy() {
    return Boolean.parseBoolean(parameters.text(EMAIL_COPY, "true").trim());
  }

  private Notice notice(AccessRequest r, String what, String body) {
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
