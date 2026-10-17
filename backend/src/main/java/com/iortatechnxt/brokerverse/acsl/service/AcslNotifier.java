package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import org.springframework.stereotype.Component;

/**
 * Notifications of ACSL (ACSL 2.5.4, 2.6.2, 2.10-2.12): the requester hears of a case result, the
 * preparer of a returned, approved or posted correction, the Account Officer of a short or over
 * payment, and the next team of work waiting for it.
 */
@Component
public class AcslNotifier {

  private final NotificationService notifications;

  /**
   * Creates the notifier.
   *
   * @param notifications notifications
   */
  public AcslNotifier(NotificationService notifications) {
    this.notifications = notifications;
  }

  /**
   * Notifies one user.
   *
   * @param username user
   * @param title title
   * @param body body
   * @param link route
   * @param entityType record type
   * @param entityId record id
   */
  public void user(
      String username, String title, String body, String link, String entityType, Long entityId) {
    if (username != null) {
      notifications.notifyUser(
          username,
          new Notice(title, body, link, entityType, String.valueOf(entityId)),
          Acsl.STATUS_EVENT);
    }
  }

  /**
   * Notifies the holders of a permission.
   *
   * @param permission permission of the next step
   * @param title title
   * @param body body
   * @param link route
   * @param entityType record type
   * @param entityId record id
   */
  public void team(
      String permission, String title, String body, String link, String entityType, Long entityId) {
    notifications.notifyPermission(
        permission,
        new Notice(title, body, link, entityType, String.valueOf(entityId)),
        Acsl.STATUS_EVENT);
  }
}
