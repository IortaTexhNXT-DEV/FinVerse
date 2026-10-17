package com.iortatechnxt.brokerverse.productmaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A change of a Final Terms for Proposal value (history, BDOI FRS FRPM.009.01). */
@Entity
@Table(name = "pm_final_term_history")
public class FinalTermChange {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "record_type", nullable = false, length = 30, updatable = false)
  private String recordType;

  @Column(name = "record_id", nullable = false, updatable = false)
  private Long recordId;

  @Column(name = "field_key", nullable = false, length = 30, updatable = false)
  private String fieldKey;

  @Column(name = "old_value", length = 1000, updatable = false)
  private String oldValue;

  @Column(name = "new_value", length = 1000, updatable = false)
  private String newValue;

  @Column(name = "changed_by", nullable = false, length = 50, updatable = false)
  private String changedBy;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  /** For JPA. */
  protected FinalTermChange() {}

  /**
   * Records a change.
   *
   * @param record the record
   * @param fieldKey field
   * @param values value before and after
   * @param changedBy user
   * @param changedAt time
   */
  public FinalTermChange(
      TermsRecord record, String fieldKey, String[] values, String changedBy, Instant changedAt) {
    this.recordType = record.type();
    this.recordId = record.id();
    this.fieldKey = fieldKey;
    this.oldValue = values[0];
    this.newValue = values[1];
    this.changedBy = changedBy;
    this.changedAt = changedAt;
  }

  public Long getId() {
    return id;
  }

  public String getFieldKey() {
    return fieldKey;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }
}
