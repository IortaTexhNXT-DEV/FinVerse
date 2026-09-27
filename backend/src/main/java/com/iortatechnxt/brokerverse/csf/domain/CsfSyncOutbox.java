package com.iortatechnxt.brokerverse.csf.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One contact change to send to one legacy system (FR-CSF-022, CSQ01): the payload is kept with
 * every row, so the outbox can be replayed once the interface is specified.
 */
@Entity
@Table(name = "csf_sync_outbox")
public class CsfSyncOutbox extends BaseEntity {

  private static final int MAX_ERROR = 500;

  @Column(name = "change_id", nullable = false, updatable = false)
  private Long changeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_system", nullable = false, length = 10, updatable = false)
  private TargetSystem targetSystem;

  @Column(nullable = false, columnDefinition = "text", updatable = false)
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OutboxStatus status;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "last_error", length = MAX_ERROR)
  private String lastError;

  @Column(name = "last_attempt_at")
  private Instant lastAttemptAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  protected CsfSyncOutbox() {}

  /**
   * A row of the outbox.
   *
   * @param changeId contact change
   * @param targetSystem legacy system
   * @param payload what is sent
   * @param status QUEUED, or NOT_CONFIGURED while the sync is disabled
   */
  public CsfSyncOutbox(
      Long changeId, TargetSystem targetSystem, String payload, OutboxStatus status) {
    this.changeId = changeId;
    this.targetSystem = targetSystem;
    this.payload = payload;
    this.status = status;
  }

  /**
   * Records a successful sending.
   *
   * @param when time
   */
  public void sent(Instant when) {
    this.attempts++;
    this.lastAttemptAt = when;
    this.sentAt = when;
    this.lastError = null;
    this.status = OutboxStatus.SENT;
  }

  /**
   * Records a failed sending.
   *
   * @param error error text
   * @param when time
   */
  public void failed(String error, Instant when) {
    this.attempts++;
    this.lastAttemptAt = when;
    String text = error == null ? "Sending failed" : error;
    this.lastError = text.length() > MAX_ERROR ? text.substring(0, MAX_ERROR) : text;
    this.status = OutboxStatus.FAILED;
  }

  /** Queues a row kept while the sync was disabled (replay). */
  public void requeue() {
    if (status == OutboxStatus.NOT_CONFIGURED) {
      this.status = OutboxStatus.QUEUED;
    }
  }

  public Long getChangeId() {
    return changeId;
  }

  public TargetSystem getTargetSystem() {
    return targetSystem;
  }

  public String getPayload() {
    return payload;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getLastAttemptAt() {
    return lastAttemptAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }
}
