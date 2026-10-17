package com.iortatechnxt.brokerverse.submitted.service;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNotificationRecipient;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNotificationRecipientRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Tells the recipients of a lifecycle event of a submitted policy (FR-SP-082): those defined on
 * Submitted Policies Setup &gt; Notification Recipients for the event type and the record's segment
 * - holders of a role, named users, the assigned handler or AO, outside addresses of the bank
 * counterpart - in the app and by e-mail as each user prefers. An event type switched off for the
 * segment tells nobody; an event type without recipients tells the users the caller names (the
 * handler and the AO of the record, as before the set-up).
 */
@Service
public class SbmNotifier {

  private final SbmNotificationRecipientRepository recipients;
  private final NotificationService notifications;
  private final NoticeDelivery delivery;
  private final AppUserRepository users;

  /**
   * Creates the notifier.
   *
   * @param recipients recipients set up
   * @param notifications in-app notifications (the default recipients)
   * @param delivery in-app and e-mail delivery (the recipients set up)
   * @param users holders of a role
   */
  public SbmNotifier(
      SbmNotificationRecipientRepository recipients,
      NotificationService notifications,
      NoticeDelivery delivery,
      AppUserRepository users) {
    this.recipients = recipients;
    this.notifications = notifications;
    this.delivery = delivery;
    this.users = users;
  }

  /**
   * Tells the recipients of an event of a record.
   *
   * @param p the record
   * @param eventCode notification event
   * @param notice the notice
   * @param defaults users told when the event has no recipient set up (nulls ignored)
   * @return the users told in the app
   */
  public Set<String> notify(SbmPolicy p, String eventCode, Notice notice, List<String> defaults) {
    List<SbmNotificationRecipient> defined =
        recipients
            .findByCompanyIdAndEventCodeAndRecordStatus(
                p.getCompanyId(), eventCode, RecordStatus.ACTIVE)
            .stream()
            .filter(r -> r.appliesTo(p.getSegment()))
            .toList();
    if (defined.isEmpty()) {
      Set<String> told = new LinkedHashSet<>();
      for (String user : distinct(defaults)) {
        if (notifications.notifyUser(user, notice, eventCode)) {
          told.add(user);
        }
      }
      return told;
    }
    if (defined.stream().anyMatch(r -> SbmNotificationRecipient.OFF.equals(r.getRecipientType()))) {
      return Set.of();
    }
    return toDefined(p, defined, eventCode, notice);
  }

  private Set<String> toDefined(
      SbmPolicy p, List<SbmNotificationRecipient> defined, String eventCode, Notice notice) {
    List<String> addresses = new ArrayList<>();
    List<String> named = new ArrayList<>();
    for (SbmNotificationRecipient r : defined) {
      switch (r.getRecipientType()) {
        case SbmNotificationRecipient.ROLE ->
            named.addAll(users.findEnabledUsernamesWithRole(r.getRecipient()));
        case SbmNotificationRecipient.USER -> named.add(r.getRecipient());
        case SbmNotificationRecipient.HANDLER -> named.add(p.getHandlerUsername());
        case SbmNotificationRecipient.AO -> named.add(p.getAoUsername());
        case SbmNotificationRecipient.EMAIL -> addresses.add(r.getRecipient());
        default -> {
          // OFF is handled by the caller.
        }
      }
    }
    Set<String> told = new LinkedHashSet<>();
    for (String user : distinct(named)) {
      if (delivery.toUser(user, notice, eventCode, true)) {
        told.add(user);
      }
    }
    delivery.toAddresses(List.copyOf(new LinkedHashSet<>(addresses)), notice, eventCode);
    return told;
  }

  /** Users once each, ignoring case and nulls, in the order given. */
  private static List<String> distinct(List<String> names) {
    List<String> result = new ArrayList<>();
    for (String n : names) {
      if (n != null
          && !n.isBlank()
          && result.stream().noneMatch(u -> CurrentUser.sameUser(u, n.strip()))) {
        result.add(n.strip());
      }
    }
    return result;
  }
}
