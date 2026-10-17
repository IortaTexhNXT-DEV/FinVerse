package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One value of the Final Terms for Proposal column (BDOI FRS FRPM.009.01): the value of a field
 * that the proposal shows; every change is kept in {@link FinalTermChange}.
 */
@Entity
@Table(name = "pm_final_term")
public class FinalTerm extends BaseEntity {

  @Column(name = "record_type", nullable = false, length = 30, updatable = false)
  private String recordType;

  @Column(name = "record_id", nullable = false, updatable = false)
  private Long recordId;

  @Column(name = "field_key", nullable = false, length = 30, updatable = false)
  private String fieldKey;

  @Column(name = "field_value", length = 1000)
  private String fieldValue;

  /** For JPA. */
  protected FinalTerm() {}

  /**
   * Creates a value.
   *
   * @param record the record
   * @param fieldKey field
   */
  public FinalTerm(TermsRecord record, String fieldKey) {
    this.recordType = record.type();
    this.recordId = record.id();
    this.fieldKey = fieldKey;
  }

  /**
   * Changes the value.
   *
   * @param value new value
   */
  public void change(String value) {
    this.fieldValue = value;
  }

  public String getFieldKey() {
    return fieldKey;
  }

  public String getFieldValue() {
    return fieldValue;
  }
}
