package com.iortatechnxt.brokerverse.screening.cases.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The answer to one template field of a case review (SNSRP-501): text (text, long text, list and
 * check box fields), number (number and amount), date or attachment.
 */
@Entity
@Table(name = "scr_case_answer")
public class CaseAnswer extends BaseEntity {

  @Column(name = "review_id", nullable = false, updatable = false)
  private Long reviewId;

  @Column(name = "field_code", nullable = false, length = 40, updatable = false)
  private String fieldCode;

  @Column(name = "value_text", length = 4000)
  private String valueText;

  @Column(name = "value_number", precision = 19, scale = 4)
  private BigDecimal valueNumber;

  @Column(name = "value_date")
  private LocalDate valueDate;

  @Column(name = "attachment_id")
  private Long attachmentId;

  /** For JPA. */
  protected CaseAnswer() {}

  /**
   * Creates an empty answer.
   *
   * @param reviewId the review
   * @param fieldCode the template field code
   */
  public CaseAnswer(Long reviewId, String fieldCode) {
    this.reviewId = reviewId;
    this.fieldCode = fieldCode;
  }

  /**
   * Replaces the value.
   *
   * @param value the typed value
   */
  public void set(AnswerValue value) {
    this.valueText = value.text();
    this.valueNumber = value.number();
    this.valueDate = value.date();
    this.attachmentId = value.attachmentId();
  }

  /**
   * The typed value.
   *
   * @return value
   */
  public AnswerValue value() {
    return new AnswerValue(valueText, valueNumber, valueDate, attachmentId);
  }

  public Long getReviewId() {
    return reviewId;
  }

  public String getFieldCode() {
    return fieldCode;
  }

  /**
   * A typed answer; exactly one part is set for a filled field, none for a blank one.
   *
   * @param text text, list code or TRUE / FALSE
   * @param number number or amount
   * @param date date
   * @param attachmentId attachment
   */
  public record AnswerValue(String text, BigDecimal number, LocalDate date, Long attachmentId) {

    /** A blank answer. */
    public static final AnswerValue BLANK = new AnswerValue(null, null, null, null);

    /**
     * Whether the answer holds a value.
     *
     * @return true when filled
     */
    public boolean filled() {
      return text != null && !text.isBlank()
          || number != null
          || date != null
          || attachmentId != null;
    }
  }
}
