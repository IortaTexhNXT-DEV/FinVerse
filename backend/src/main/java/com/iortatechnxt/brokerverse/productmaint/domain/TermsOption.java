package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One quotation option of an insurer in the comparative table (BDOI FRS FRPM.006.02 and
 * FRPM.012.02): an insurer may give several options (coverage, premium, commission, deductibles,
 * terms, warranties, clauses, extensions, other details), each with the insurer response Approved,
 * Not Covered or Others (with the insurer's own wording). The values are kept as JSON by field.
 */
@Entity
@Table(name = "pm_terms_option")
public class TermsOption extends BaseEntity {

  @Column(name = "record_type", nullable = false, length = 30, updatable = false)
  private String recordType;

  @Column(name = "record_id", nullable = false, updatable = false)
  private Long recordId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "insurer_name", nullable = false, length = 200)
  private String insurerName;

  @Column(name = "option_no", nullable = false, updatable = false)
  private int optionNo;

  @Column(nullable = false, length = 20)
  private String answer;

  @Column(name = "other_answer", length = 500)
  private String otherAnswer;

  @Column(name = "option_values", nullable = false, columnDefinition = "text")
  private String optionValues;

  /** For JPA. */
  protected TermsOption() {}

  /**
   * Creates an option.
   *
   * @param record the record of the comparative table
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param optionNo option number of the insurer
   */
  public TermsOption(TermsRecord record, String insurerCode, String insurerName, int optionNo) {
    this.recordType = record.type();
    this.recordId = record.id();
    this.insurerCode = insurerCode;
    this.insurerName = insurerName;
    this.optionNo = optionNo;
    this.answer = "APPROVED";
    this.optionValues = "{}";
  }

  /**
   * Changes the response and the values.
   *
   * @param answer APPROVED, NOT_COVERED or OTHERS
   * @param other the insurer's own response (Others)
   * @param values the values as JSON
   */
  public void change(String answer, String other, String values) {
    this.answer = answer;
    this.otherAnswer = other == null || other.isBlank() ? null : other.strip();
    this.optionValues = values;
  }

  public String getRecordType() {
    return recordType;
  }

  public Long getRecordId() {
    return recordId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getInsurerName() {
    return insurerName;
  }

  public int getOptionNo() {
    return optionNo;
  }

  public String getAnswer() {
    return answer;
  }

  public String getOtherAnswer() {
    return otherAnswer;
  }

  public String getOptionValues() {
    return optionValues;
  }
}
