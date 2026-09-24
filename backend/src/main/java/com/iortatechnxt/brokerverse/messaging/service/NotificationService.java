package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.Notification;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationRepository;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-app notifications (BRNB.015): "a request was returned to you", "an account waits in your
 * queue". Created in the business transaction, shown by the bell in the header.
 */
@Service
@Transactional
public class NotificationService {

  private final NotificationRepository notifications;
  private final NotificationPreferenceService preferences;
  private final UserDirectory users;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param notifications notifications
   * @param preferences users' notification preferences (RMTID.034)
   * @param users user directory (permission holders)
   * @param currentUser current user
   * @param clock clock
   */
  public NotificationService(
      NotificationRepository notifications,
      NotificationPreferenceService preferences,
      UserDirectory users,
      CurrentUser currentUser,
      Clock clock) {
    this.notifications = notifications;
    this.preferences = preferences;
    this.users = users;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Notifies one user.
   *
   * @param username recipient
   * @param notice content
   */
  public void notifyUser(String username, Notice notice) {
    if (username != null && !CurrentUser.SYSTEM.equals(username)) {
      notifications.save(new Notification(username, notice, clock.instant()));
    }
  }

  /**
   * Notifies one user about an event, unless the user switched its in-app notifications off
   * (RMTID.034).
   *
   * @param username recipient
   * @param notice content
   * @param eventCode notification event ({@code msg_notification_event})
   * @return true when the notification was created
   */
  public boolean notifyUser(String username, Notice notice, String eventCode) {
    if (username == null || !preferences.wantsInApp(username, eventCode)) {
      return false;
    }
    notifyUser(username, notice);
    return !CurrentUser.SYSTEM.equals(username);
  }

  /**
   * Notifies every enabled holder of a permission about an event, except the user who caused it and
   * users who switched the event's in-app notifications off (RMTID.034).
   *
   * @param permission permission
   * @param notice content
   * @param eventCode notification event
   * @return number of users notified
   */
  public int notifyPermission(String permission, Notice notice, String eventCode) {
    String actor = currentUser.username();
    int count = 0;
    for (String user : users.usersWithPermission(permission)) {
      if (!CurrentUser.sameUser(user, actor) && notifyUser(user, notice, eventCode)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Notifies every enabled user holding a permission, except the user who caused the event.
   *
   * @param permission permission
   * @param notice content
   * @return number of users notified
   */
  public int notifyPermission(String permission, Notice notice) {
    String actor = currentUser.username();
    int count = 0;
    for (String user : users.usersWithPermission(permission)) {
      if (!CurrentUser.sameUser(user, actor)) {
        notifyUser(user, notice);
        count++;
      }
    }
    return count;
  }

  /**
   * The current user's notifications, newest first.
   *
   * @param unreadOnly only unread
   * @param pageable page
   * @return notifications
   */
  @Transactional(readOnly = true)
  public Page<Notification> mine(boolean unreadOnly, Pageable pageable) {
    String me = currentUser.username();
    return unreadOnly
        ? notifications.findByRecipientIgnoreCaseAndReadAtIsNullOrderByIdDesc(me, pageable)
        : notifications.findByRecipientIgnoreCaseOrderByIdDesc(me, pageable);
  }

  /**
   * Unread count of the current user (header badge).
   *
   * @return count
   */
  @Transactional(readOnly = true)
  public long unreadCount() {
    return notifications.countByRecipientIgnoreCaseAndReadAtIsNull(currentUser.username());
  }

  /**
   * Marks one of the current user's notifications read.
   *
   * @param id notification
   * @return the notification
   */
  public Notification markRead(Long id) {
    Notification n =
        notifications
            .findById(id)
            .filter(x -> CurrentUser.sameUser(x.getRecipient(), currentUser.username()))
            .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    n.markRead(clock.instant());
    return n;
  }

  /**
   * Marks all of the current user's notifications read.
   *
   * @return number marked
   */
  public int markAllRead() {
    return notifications.markAllRead(currentUser.username(), clock.instant());
  }
}
