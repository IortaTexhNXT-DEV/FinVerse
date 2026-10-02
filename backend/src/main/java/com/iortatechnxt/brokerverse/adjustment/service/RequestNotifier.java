package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import org.springframework.stereotype.Component;

/**
 * Notifications of endorsement requests (event {@code ADJ_REQUEST_STATUS}, each user's preference
 * applies): the team of the next stage, and the requester when the request is returned or posted
 * (ADJID.007).
 */
@Component
public class RequestNotifier {

  private final NotificationService notifications;
  private final CurrentUser currentUser;

  /**
   * Creates the notifier.
   *
   * @param notifications in-app and e-mail notifications
   * @param currentUser current user (not notified of their own action)
   */
  public RequestNotifier(NotificationService notifications, CurrentUser currentUser) {
    this.notifications = notifications;
    this.currentUser = currentUser;
  }

  /**
   * Tells the holders of a permission that a request waits for them.
   *
   * @param permission team permission
   * @param request request
   * @param what what is waiting (e.g. "for validation")
   */
  public void team(String permission, EndorsementRequest request, String what) {
    notifications.notifyPermission(permission, notice(request, what), Adjustments.STATUS_EVENT);
  }

  /**
   * Tells the requester what happened to the request.
   *
   * @param request request
   * @param what what happened (e.g. "posted")
   */
  public void requester(EndorsementRequest request, String what) {
    String requester = request.getCreatedBy();
    if (requester != null && !CurrentUser.sameUser(requester, currentUser.username())) {
      notifications.notifyUser(requester, notice(request, what), Adjustments.STATUS_EVENT);
    }
  }

  private static Notice notice(EndorsementRequest request, String what) {
    return new Notice(
        "Endorsement request " + request.getRequestNo() + " " + what,
        request.getSubject().invoiceNo()
            + " - "
            + request.getSubject().assuredName()
            + ": "
            + request.getTerms().description(),
        Adjustments.link(request.getId()),
        Adjustments.ENTITY,
        String.valueOf(request.getId()));
  }
}
