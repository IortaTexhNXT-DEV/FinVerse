package com.iortatechnxt.finverse.audit.domain;

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
 * Immutable audit trail record of a financial or non-financial action.
 *
 * <p>Captures originator, modifier and authorizer activity with timestamps, as required by the GL
 * audit trail control. Rows are insert-only (no update/delete API exists).
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  @Column(nullable = false, updatable = false, length = 50)
  private String username;

  @Column(name = "entity_type", nullable = false, updatable = false, length = 60)
  private String entityType;

  @Column(name = "entity_id", updatable = false, length = 60)
  private String entityId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 30)
  private AuditAction action;

  @Column(nullable = false, updatable = false, length = 500)
  private String summary;

  protected AuditLog() {}

  /**
   * Creates an audit record.
   *
   * @param occurredAt timestamp
   * @param username acting user
   * @param entityType affected entity type
   * @param entityId affected entity id or business key
   * @param action action performed
   * @param summary human readable description
   */
  public AuditLog(
      Instant occurredAt,
      String username,
      String entityType,
      String entityId,
      AuditAction action,
      String summary) {
    this.occurredAt = occurredAt;
    this.username = username;
    this.entityType = entityType;
    this.entityId = entityId;
    this.action = action;
    this.summary = summary;
  }

  public Long getId() {
    return id;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getUsername() {
    return username;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public AuditAction getAction() {
    return action;
  }

  public String getSummary() {
    return summary;
  }
}
