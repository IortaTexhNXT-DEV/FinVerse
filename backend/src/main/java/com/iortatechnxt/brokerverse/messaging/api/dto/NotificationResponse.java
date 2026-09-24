package com.iortatechnxt.brokerverse.messaging.api.dto;

import com.iortatechnxt.brokerverse.messaging.domain.Notification;
import java.time.Instant;

/**
 * An in-app notification.
 *
 * @param id id
 * @param title title
 * @param body detail
 * @param link route to open
 * @param createdAt time
 * @param read whether read
 */
public record NotificationResponse(
    Long id, String title, String body, String link, Instant createdAt, boolean read) {

  /**
   * Maps a notification.
   *
   * @param n notification
   * @return response
   */
  public static NotificationResponse from(Notification n) {
    return new NotificationResponse(
        n.getId(), n.getTitle(), n.getBody(), n.getLink(), n.getCreatedAt(), n.getReadAt() != null);
  }
}
