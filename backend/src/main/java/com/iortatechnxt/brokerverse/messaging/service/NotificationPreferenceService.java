package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationEvent;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationEventRepository;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationPreference;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationPreference.Channels;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationPreferenceRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification preferences per user and event (RMTID.034): each event has default channels; a user
 * may switch the in-app notification or the e-mail of an event off or on. {@link
 * NotificationService} honours the in-app choice for notifications raised with an event code;
 * modules that e-mail an event ask {@link #wantsEmail}.
 */
@Service
@Transactional
public class NotificationPreferenceService {

  private static final String ENTITY = "NotificationPreference";

  private final NotificationEventRepository events;
  private final NotificationPreferenceRepository preferences;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param events notification events
   * @param preferences preferences
   * @param currentUser current user
   * @param audit audit trail
   */
  public NotificationPreferenceService(
      NotificationEventRepository events,
      NotificationPreferenceRepository preferences,
      CurrentUser currentUser,
      AuditTrailService audit) {
    this.events = events;
    this.preferences = preferences;
    this.currentUser = currentUser;
    this.audit = audit;
  }

  /**
   * The current user's channels for every event.
   *
   * @return one entry per event, defaults where the user chose nothing
   */
  @Transactional(readOnly = true)
  public List<EventChannels> mine() {
    Map<String, NotificationPreference> chosen =
        preferences.findByUsernameIgnoreCase(currentUser.username()).stream()
            .collect(Collectors.toMap(NotificationPreference::getEventCode, Function.identity()));
    return events.findAllByOrderBySortOrderAscCodeAsc().stream()
        .map(e -> EventChannels.of(e, chosen.get(e.getCode())))
        .toList();
  }

  /**
   * Sets the current user's channels for an event.
   *
   * @param eventCode event
   * @param channels in-app and e-mail choice
   * @return the event with the user's channels
   */
  public EventChannels update(String eventCode, Channels channels) {
    NotificationEvent event = requireEvent(eventCode);
    String me = currentUser.username();
    NotificationPreference preference =
        preferences
            .findByUsernameIgnoreCaseAndEventCode(me, eventCode)
            .orElseGet(() -> new NotificationPreference(me, eventCode, channels));
    preference.change(channels);
    preferences.save(preference);
    audit.record(
        ENTITY,
        me + ":" + eventCode,
        AuditAction.UPDATE,
        "In-app " + channels.inApp() + ", e-mail " + channels.email());
    return EventChannels.of(event, preference);
  }

  /**
   * Whether a user receives in-app notifications of an event.
   *
   * @param username user
   * @param eventCode event; null or unknown events are always notified
   * @return true unless the user switched them off
   */
  @Transactional(readOnly = true)
  public boolean wantsInApp(String username, String eventCode) {
    return channels(username, eventCode).inApp();
  }

  /**
   * Whether a user wants e-mails of an event.
   *
   * @param username user
   * @param eventCode event
   * @return the user's choice, else the event default (false for unknown events)
   */
  @Transactional(readOnly = true)
  public boolean wantsEmail(String username, String eventCode) {
    return channels(username, eventCode).email();
  }

  private Channels channels(String username, String eventCode) {
    if (eventCode == null) {
      return new Channels(true, false);
    }
    return preferences
        .findByUsernameIgnoreCaseAndEventCode(username, eventCode)
        .map(p -> new Channels(p.isInApp(), p.isEmail()))
        .orElseGet(
            () ->
                events
                    .findById(eventCode)
                    .map(e -> new Channels(e.isDefaultInApp(), e.isDefaultEmail()))
                    .orElse(new Channels(true, false)));
  }

  private NotificationEvent requireEvent(String code) {
    return events
        .findById(code)
        .orElseThrow(() -> new ResourceNotFoundException("Notification event", code));
  }

  /**
   * An event with the channels in force for a user.
   *
   * @param code event code
   * @param name event name
   * @param module module raising it
   * @param description description
   * @param inApp in-app notification
   * @param email e-mail
   * @param custom whether the user changed the defaults
   */
  public record EventChannels(
      String code,
      String name,
      String module,
      String description,
      boolean inApp,
      boolean email,
      boolean custom) {

    static EventChannels of(NotificationEvent e, NotificationPreference p) {
      return new EventChannels(
          e.getCode(),
          e.getName(),
          e.getModule(),
          e.getDescription(),
          p == null ? e.isDefaultInApp() : p.isInApp(),
          p == null ? e.isDefaultEmail() : p.isEmail(),
          p != null);
    }
  }
}
