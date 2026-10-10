package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Delivers a notice in the system and by e-mail, each as the user chose in the notification
 * preferences for the event (BDOI FRS FRUM.013.01 and FRPM.029.01: "in the system and by e-mail as
 * each user prefers"). The e-mail carries the notice text and a link to the record.
 */
@Service
public class NoticeDelivery {

  /** Longest reference of an e-mail (the title of the notice). */
  private static final int MAX_REFERENCE = 60;

  private final NotificationService notifications;
  private final NotificationPreferenceService preferences;
  private final MessageService messages;
  private final AppUserRepository users;
  private final UserDirectory directory;
  private final CurrentUser currentUser;

  /**
   * Creates the delivery.
   *
   * @param notifications in-app notifications (respects the in-app preference)
   * @param preferences notification preferences (the e-mail choice)
   * @param messages e-mail queue
   * @param users users (e-mail addresses)
   * @param directory holders of a permission
   * @param currentUser the actor (not told of his or her own action)
   */
  public NoticeDelivery(
      NotificationService notifications,
      NotificationPreferenceService preferences,
      MessageService messages,
      AppUserRepository users,
      UserDirectory directory,
      CurrentUser currentUser) {
    this.notifications = notifications;
    this.preferences = preferences;
    this.messages = messages;
    this.users = users;
    this.directory = directory;
    this.currentUser = currentUser;
  }

  /**
   * Tells one user, in the system and by e-mail, as the user chose for the event.
   *
   * @param username user
   * @param notice the notice
   * @param eventCode notification event
   * @param withEmail false to leave out the e-mail (in-app only)
   * @return true when the user was told in the system
   */
  public boolean toUser(String username, Notice notice, String eventCode, boolean withEmail) {
    if (username == null || CurrentUser.SYSTEM.equals(username)) {
      return false;
    }
    boolean told = notifications.notifyUser(username, notice, eventCode);
    if (withEmail && preferences.wantsEmail(username, eventCode)) {
      email(username, notice, eventCode);
    }
    return told;
  }

  /**
   * Tells every holder of a permission (except the actor), in the system and by e-mail, as each
   * chose for the event.
   *
   * @param permission permission
   * @param notice the notice
   * @param eventCode notification event
   * @param withEmail false to leave out the e-mail (in-app only)
   * @return users told in the system
   */
  public int toPermission(String permission, Notice notice, String eventCode, boolean withEmail) {
    String actor = currentUser.username();
    int count = 0;
    for (String user : directory.usersWithPermission(permission)) {
      if (!CurrentUser.sameUser(user, actor) && toUser(user, notice, eventCode, withEmail)) {
        count++;
      }
    }
    return count;
  }

  /**
   * E-mails a notice to fixed addresses (for example the mailbox of Information Security).
   *
   * @param addresses e-mail addresses
   * @param notice the notice
   * @param purpose purpose of the e-mail (the event)
   */
  public void toAddresses(List<String> addresses, Notice notice, String purpose) {
    if (addresses.isEmpty()) {
      return;
    }
    messages.queueEmail(
        new OutboundEmail(
            null,
            purpose,
            addresses,
            List.of(),
            BrandAssets.SYSTEM_NAME + ": " + notice.title(),
            notice.body(),
            List.of(),
            null,
            new RecordLink(notice.entityType(), notice.entityId(), reference(notice.title()))));
  }

  private static String reference(String title) {
    return title == null || title.length() <= MAX_REFERENCE
        ? title
        : title.substring(0, MAX_REFERENCE);
  }

  private void email(String username, Notice notice, String purpose) {
    users
        .findByUsernameIgnoreCase(username)
        .map(AppUser::getEmail)
        .filter(e -> e != null && !e.isBlank())
        .ifPresent(address -> toAddresses(List.of(address), notice, purpose));
  }
}
