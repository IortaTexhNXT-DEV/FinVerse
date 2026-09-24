package com.iortatechnxt.brokerverse.messaging.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.messaging.api.dto.NotificationResponse;
import com.iortatechnxt.brokerverse.messaging.api.dto.PreferenceRequest;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationPreference.Channels;
import com.iortatechnxt.brokerverse.messaging.service.NotificationPreferenceService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationPreferenceService.EventChannels;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's notifications (header bell). */
@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

  private static final int MAX_PAGE_SIZE = 100;

  private final NotificationService notifications;
  private final NotificationPreferenceService preferences;

  /**
   * Creates the controller.
   *
   * @param notifications notification service
   * @param preferences notification preferences
   */
  public NotificationController(
      NotificationService notifications, NotificationPreferenceService preferences) {
    this.notifications = notifications;
    this.preferences = preferences;
  }

  /**
   * My channels per notification event (RMTID.034).
   *
   * @return events with my channels
   */
  @GetMapping("/preferences")
  public List<EventChannels> preferences() {
    return preferences.mine();
  }

  /**
   * Sets my channels for an event.
   *
   * @param eventCode event
   * @param request in-app and e-mail choice
   * @return the event with my channels
   */
  @PutMapping("/preferences/{eventCode}")
  public EventChannels updatePreference(
      @PathVariable String eventCode, @Valid @RequestBody PreferenceRequest request) {
    return preferences.update(
        eventCode,
        new Channels(Boolean.TRUE.equals(request.inApp()), Boolean.TRUE.equals(request.email())));
  }

  /**
   * My notifications.
   *
   * @param unreadOnly only unread
   * @param page page
   * @param size size
   * @return notifications
   */
  @GetMapping
  public PageResponse<NotificationResponse> mine(
      @RequestParam(defaultValue = "false") boolean unreadOnly,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        notifications.mine(unreadOnly, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        NotificationResponse::from);
  }

  /**
   * My unread count.
   *
   * @return {@code {"unread": n}}
   */
  @GetMapping("/unread-count")
  public Map<String, Long> unreadCount() {
    return Map.of("unread", notifications.unreadCount());
  }

  /**
   * Marks a notification read.
   *
   * @param id notification
   * @return notification
   */
  @PostMapping("/{id}/read")
  public NotificationResponse read(@PathVariable Long id) {
    return NotificationResponse.from(notifications.markRead(id));
  }

  /**
   * Marks all my notifications read.
   *
   * @return {@code {"updated": n}}
   */
  @PostMapping("/read-all")
  public Map<String, Integer> readAll() {
    return Map.of("updated", notifications.markAllRead());
  }
}
