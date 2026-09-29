package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.domain.MfaResetRequest;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.security.service.mfa.MfaResetApplied;
import com.iortatechnxt.brokerverse.security.service.mfa.MfaResetRequested;
import com.iortatechnxt.brokerverse.security.service.mfa.MfaResetService;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The four-eyes reset of a user's second factor in the user access flow: the approvers
 * (MFA_RESET_APPROVE) are told of each reset that waits and find it in My Approvals, never their
 * own; the user is told once the reset is applied.
 */
@Component
public class MfaResetNotices implements PendingApprovalSource {

  /** Event of a reset to approve. */
  public static final String TO_APPROVE = "MFA_RESET_TO_APPROVE";

  /** Event of an applied reset (to the user). */
  public static final String DONE = "MFA_RESET_DONE";

  /** Screen of the resets. */
  public static final String SCREEN = "/admin/second-factor";

  private static final String ENTITY = "AppUser";

  private final NotificationService notifications;
  private final MfaResetService resets;
  private final UserDirectory users;

  /**
   * Creates the component.
   *
   * @param notifications notices
   * @param resets reset requests
   * @param users display names
   */
  public MfaResetNotices(
      NotificationService notifications, MfaResetService resets, UserDirectory users) {
    this.notifications = notifications;
    this.resets = resets;
    this.users = users;
  }

  /**
   * Tells the approvers of a reset that waits.
   *
   * @param event the reset
   */
  @EventListener
  public void on(MfaResetRequested event) {
    notifications.notifyPermission(
        MfaResetService.APPROVE,
        new Notice(
            "Second factor reset to approve: " + users.displayName(event.username()),
            users.displayName(event.requestedBy())
                + " asks for the reset. Reason: "
                + event.reason(),
            SCREEN,
            ENTITY,
            event.username()),
        TO_APPROVE);
  }

  /**
   * Tells the user that the authenticator app was reset.
   *
   * @param event the applied reset
   */
  @EventListener
  public void on(MfaResetApplied event) {
    notifications.notifyUser(
        event.username(),
        new Notice(
            "Your authenticator app was reset",
            "Enrol your authenticator app again at your next sign-in.",
            "/profile",
            ENTITY,
            event.username()),
        DONE);
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(MfaResetService.APPROVE)) {
      return List.of();
    }
    return resets.list(MfaResetRequest.PENDING).stream()
        .filter(r -> viewer.mayApproveItemOf(r.getRequestedBy()))
        .map(this::item)
        .toList();
  }

  private PendingApproval item(MfaResetRequest r) {
    return new PendingApproval(
        AccessRequestApprovalSource.MODULE,
        "Second factor reset",
        users.displayName(r.getUsername()),
        "Reset of the authenticator app. Reason: " + r.getReason(),
        null,
        null,
        r.getRequestedBy(),
        r.getRequestedAt(),
        null,
        SCREEN);
  }
}
