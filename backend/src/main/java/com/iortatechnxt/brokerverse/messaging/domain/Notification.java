package com.iortatechnxt.brokerverse.messaging.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** An in-app notification for one user (header bell), e.g. "Account returned by Processing". */
@Entity
@Table(name = "msg_notification")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50, updatable = false)
  private String recipient;

  @Column(nullable = false, length = 200, updatable = false)
  private String title;

  @Column(length = 1000, updatable = false)
  private String body;

  @Column(length = 300, updatable = false)
  private String link;

  @Column(name = "entity_type", length = 60, updatable = false)
  private String entityType;

  @Column(name = "entity_id", length = 60, updatable = false)
  private String entityId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "read_at")
  private Instant readAt;

  protected Notification() {}

  /**
   * Creates an unread notification.
   *
   * @param recipient user
   * @param notice content
   * @param createdAt time
   */
  public Notification(String recipient, Notice notice, Instant createdAt) {
    this.recipient = recipient;
    this.title = notice.title();
    this.body = notice.body();
    this.link = notice.link();
    this.entityType = notice.entityType();
    this.entityId = notice.entityId();
    this.createdAt = createdAt;
  }

  /**
   * Marks the notification read (idempotent).
   *
   * @param when time
   */
  public void markRead(Instant when) {
    if (readAt == null) {
      readAt = when;
    }
  }

  public Long getId() {
    return id;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public String getLink() {
    return link;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getReadAt() {
    return readAt;
  }
}
