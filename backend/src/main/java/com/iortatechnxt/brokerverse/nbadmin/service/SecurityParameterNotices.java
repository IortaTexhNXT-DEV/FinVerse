package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.system.domain.SystemParameter;
import com.iortatechnxt.brokerverse.system.service.SecurityParameterApprovals;
import com.iortatechnxt.brokerverse.system.service.SecurityParameterChangeRequested;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The second approval of the security parameters in the user access flow: the approvers
 * (SECURITY_PARAMETER_APPROVE) are told of each change that waits, and find it in My Approvals,
 * never their own.
 */
@Component
public class SecurityParameterNotices implements PendingApprovalSource {

  /** Event of a change to approve. */
  public static final String EVENT = "UAM_PARAMETER_TO_APPROVE";

  /** Screen of the parameters. */
  public static final String SCREEN = "/admin/parameters";

  private static final String ENTITY = "SystemParameter";

  private final NotificationService notifications;
  private final SecurityParameterApprovals approvals;
  private final UserDirectory users;

  /**
   * Creates the component.
   *
   * @param notifications notices
   * @param approvals changes that wait
   * @param users display names of the requesters
   */
  public SecurityParameterNotices(
      NotificationService notifications,
      SecurityParameterApprovals approvals,
      UserDirectory users) {
    this.notifications = notifications;
    this.approvals = approvals;
    this.users = users;
  }

  /**
   * Tells the approvers of a change that waits.
   *
   * @param event the change
   */
  @EventListener
  public void on(SecurityParameterChangeRequested event) {
    notifications.notifyPermission(
        SecurityParameterApprovals.APPROVE,
        new Notice(
            "Security setting to approve: " + event.description(),
            users.displayName(event.requestedBy())
                + " requests a change from '"
                + event.currentValue()
                + "' to '"
                + event.requestedValue()
                + "'",
            SCREEN,
            ENTITY,
            event.key()),
        EVENT);
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(SecurityParameterApprovals.APPROVE)) {
      return List.of();
    }
    return approvals.pending().stream()
        .filter(p -> viewer.mayApproveItemOf(p.getPendingBy()))
        .map(SecurityParameterNotices::item)
        .toList();
  }

  private static PendingApproval item(SystemParameter p) {
    return new PendingApproval(
        AccessRequestApprovalSource.MODULE,
        "Security setting",
        p.getKey(),
        p.getDescription() + ": from '" + p.getValue() + "' to '" + p.getPendingValue() + "'",
        null,
        null,
        p.getPendingBy(),
        p.getPendingAt(),
        null,
        SCREEN);
  }
}
