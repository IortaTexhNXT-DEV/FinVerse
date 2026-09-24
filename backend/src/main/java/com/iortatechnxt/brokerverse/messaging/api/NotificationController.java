package com.iortatechnxt.brokerverse.messaging.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.messaging.api.dto.NotificationResponse;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  /**
   * Creates the controller.
   *
   * @param notifications notification service
   */
  public NotificationController(NotificationService notifications) {
    this.notifications = notifications;
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
