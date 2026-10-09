package com.iortatechnxt.brokerverse.audit.domain;

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

  /** Longest old or new value kept on an entry. */
  public static final int MAX_VALUE = 500;

  private static final int MAX_REMARKS = 1000;

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

  @Column(name = "ip_address", updatable = false, length = ActorContext.MAX_ADDRESS)
  private String ipAddress;

  @Column(name = "role_names", updatable = false, length = ActorContext.MAX_ROLES)
  private String roleNames;

  @Column(name = "old_value", updatable = false, length = MAX_VALUE)
  private String oldValue;

  @Column(name = "new_value", updatable = false, length = MAX_VALUE)
  private String newValue;

  @Column(updatable = false, length = MAX_REMARKS)
  private String remarks;

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

  /**
   * Adds where the action came from: the source address and the roles of the actor at the time.
   *
   * @param address source (IP) address, may be null
   * @param roles role names of the actor, may be null
   * @return this entry
   */
  public AuditLog from(String address, String roles) {
    this.ipAddress = ActorContext.clip(address, ActorContext.MAX_ADDRESS);
    this.roleNames = ActorContext.clip(roles, ActorContext.MAX_ROLES);
    return this;
  }

  /**
   * Adds the value before and after the change.
   *
   * @param before old value, may be null
   * @param after new value, may be null
   * @return this entry
   */
  public AuditLog values(String before, String after) {
    this.oldValue = ActorContext.clip(before, MAX_VALUE);
    this.newValue = ActorContext.clip(after, MAX_VALUE);
    return this;
  }

  /**
   * Adds the remarks of the action (the comment of an approval, a return or a rejection).
   *
   * @param text remarks, may be null
   * @return this entry
   */
  public AuditLog remarks(String text) {
    this.remarks =
        text == null || text.isBlank() ? null : ActorContext.clip(text.strip(), MAX_REMARKS);
    return this;
  }

  public String getRemarks() {
    return remarks;
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

  public String getIpAddress() {
    return ipAddress;
  }

  public String getRoleNames() {
    return roleNames;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }
}
