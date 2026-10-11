package com.iortatechnxt.brokerverse.renewal.channel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A status change of a channel message, kept for the delivery history (append-only). */
@Entity
@Table(name = "rnw_channel_event")
public class ChannelEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "message_id", nullable = false, updatable = false)
  private Long messageId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private ChannelStatus status;

  @Column(length = 1000, updatable = false)
  private String detail;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, length = 50, updatable = false)
  private String createdBy;

  protected ChannelEvent() {}

  /**
   * An event.
   *
   * @param messageId message
   * @param status status reached
   * @param detail detail (reference, error)
   * @param createdAt time
   * @param createdBy user or SYSTEM
   */
  public ChannelEvent(
      Long messageId, ChannelStatus status, String detail, Instant createdAt, String createdBy) {
    this.messageId = messageId;
    this.status = status;
    this.detail = detail;
    this.createdAt = createdAt;
    this.createdBy = createdBy;
  }

  public Long getId() {
    return id;
  }

  public Long getMessageId() {
    return messageId;
  }

  public ChannelStatus getStatus() {
    return status;
  }

  public String getDetail() {
    return detail;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }
}
