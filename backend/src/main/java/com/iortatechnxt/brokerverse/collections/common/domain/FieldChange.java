package com.iortatechnxt.brokerverse.collections.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One field change of a Collections record with its "from" and "to" values, user, time and source
 * IP (BRCLXN.043, NFR audit logging); immutable. Bulk changes carry their bulk reference
 * (BRCLXN.051).
 */
@Entity
@Table(name = "clx_field_change")
public class FieldChange {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 40, updatable = false)
  private String entity;

  @Column(name = "entity_id", nullable = false, length = 60, updatable = false)
  private String entityId;

  @Column(name = "item_id", updatable = false)
  private Long itemId;

  @Column(nullable = false, length = 60, updatable = false)
  private String field;

  @Column(name = "old_value", length = 1000, updatable = false)
  private String oldValue;

  @Column(name = "new_value", length = 1000, updatable = false)
  private String newValue;

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  @Column(name = "source_ip", length = 45, updatable = false)
  private String sourceIp;

  @Column(name = "bulk_ref", length = 40, updatable = false)
  private String bulkRef;

  protected FieldChange() {}

  /**
   * Records a change.
   *
   * @param record the changed record
   * @param field field name
   * @param values old and new value
   * @param who user, time, source IP and bulk reference
   */
  public FieldChange(Target record, String field, Values values, Origin who) {
    this.companyId = record.companyId();
    this.entity = record.entity();
    this.entityId = record.entityId();
    this.itemId = record.itemId();
    this.field = field;
    this.oldValue = clip(values.oldValue());
    this.newValue = clip(values.newValue());
    this.username = who.username();
    this.changedAt = who.at();
    this.sourceIp = who.sourceIp();
    this.bulkRef = who.bulkRef();
  }

  private static String clip(String value) {
    final int max = 1000;
    return value == null || value.length() <= max ? value : value.substring(0, max);
  }

  public Long getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getEntity() {
    return entity;
  }

  public String getEntityId() {
    return entityId;
  }

  public Long getItemId() {
    return itemId;
  }

  public String getField() {
    return field;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public String getUsername() {
    return username;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public String getSourceIp() {
    return sourceIp;
  }

  public String getBulkRef() {
    return bulkRef;
  }

  /**
   * The record a change belongs to.
   *
   * @param companyId company
   * @param entity record type (e.g. CollectionItem, AssignmentRule)
   * @param entityId record key
   * @param itemId collection item, null when the record is not an item's
   */
  public record Target(Long companyId, String entity, String entityId, Long itemId) {}

  /**
   * Old and new value, as text.
   *
   * @param oldValue before
   * @param newValue after
   */
  public record Values(String oldValue, String newValue) {}

  /**
   * Who changed it, when, from where.
   *
   * @param username user
   * @param at time
   * @param sourceIp client IP address, null for background work
   * @param bulkRef bulk reference, null for a single change
   */
  public record Origin(String username, Instant at, String sourceIp, String bulkRef) {}
}
