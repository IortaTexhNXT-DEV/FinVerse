package com.iortatechnxt.brokerverse.nbadmin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One event in the history of an access request with its remarks (BRD 1.002.1.1.4-5 "add / save
 * remarks", 1.006, 1.008, 2.002.7). Insert-only: a database trigger rejects update, delete and
 * truncate (V1062).
 */
@Entity
@Table(name = "nba_access_request_event")
public class AccessRequestEvent {

  private static final int STATUS_LENGTH = 20;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_id", nullable = false, updatable = false)
  private Long requestId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = STATUS_LENGTH)
  private AccessRequestAction action;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", updatable = false, length = STATUS_LENGTH)
  private AccessRequestStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, updatable = false, length = STATUS_LENGTH)
  private AccessRequestStatus toStatus;

  @Column(updatable = false, length = 1000)
  private String remarks;

  @Column(nullable = false, updatable = false, length = 50)
  private String actor;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected AccessRequestEvent() {}

  /**
   * Records an event.
   *
   * @param requestId request
   * @param action what happened
   * @param fromStatus status before, null for a new request
   * @param toStatus status after
   * @param remarks remarks, null for none
   * @param actor user (or SYSTEM)
   * @param occurredAt time
   */
  public AccessRequestEvent(
      Long requestId,
      AccessRequestAction action,
      AccessRequestStatus fromStatus,
      AccessRequestStatus toStatus,
      String remarks,
      String actor,
      Instant occurredAt) {
    this.requestId = requestId;
    this.action = action;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.remarks = remarks;
    this.actor = actor;
    this.occurredAt = occurredAt;
  }

  public Long getId() {
    return id;
  }

  public Long getRequestId() {
    return requestId;
  }

  public AccessRequestAction getAction() {
    return action;
  }

  public AccessRequestStatus getFromStatus() {
    return fromStatus;
  }

  public AccessRequestStatus getToStatus() {
    return toStatus;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getActor() {
    return actor;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
