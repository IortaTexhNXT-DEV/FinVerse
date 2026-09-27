package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * One rule of a {@link DecisionMatrix} (BRRN.034): criteria (blank = any) and the proposed
 * disposition with AUTO or MANUAL and a letter hint. The claims condition TOTAL_LOSS is kept but
 * never matches until the Claims module defines a total-loss indicator (CLQ28).
 */
@Entity
@Table(name = "rnw_decision_rule")
public class DecisionRule extends BaseEntity {

  @Column(nullable = false)
  private int priority;

  @Column(length = 40)
  private String segment;

  @Column(name = "line_code", length = 30)
  private String lineCode;

  @Column(name = "product_code", length = 20)
  private String productCode;

  @Column(name = "mortgaged")
  private Boolean mortgaged;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Bucket bucket;

  @Column(name = "claims_condition", length = 20)
  private String claimsCondition;

  @Column(name = "endorsement_condition", length = 20)
  private String endorsementCondition;

  @Column(name = "payment_condition", length = 20)
  private String paymentCondition;

  @Column(name = "days_from")
  private Integer daysFrom;

  @Column(name = "days_to")
  private Integer daysTo;

  @Enumerated(EnumType.STRING)
  @Column(name = "outcome_disposition", nullable = false, length = 20)
  private RenewalDisposition outcome;

  @Column(nullable = false, length = 10)
  private String automation;

  @Column(name = "letter_hint", length = 10)
  private String letterHint;

  protected DecisionRule() {}

  /**
   * Creates a rule.
   *
   * @param data rule data
   */
  public DecisionRule(Data data) {
    this.priority = data.priority();
    this.segment = blank(data.criteria().segment());
    this.lineCode = blank(data.criteria().lineCode());
    this.productCode = blank(data.criteria().productCode());
    this.mortgaged = data.criteria().mortgaged();
    this.bucket = data.criteria().bucket();
    this.claimsCondition = blank(data.criteria().claims());
    this.endorsementCondition = blank(data.criteria().endorsement());
    this.paymentCondition = blank(data.criteria().payment());
    this.daysFrom = data.criteria().daysFrom();
    this.daysTo = data.criteria().daysTo();
    this.outcome = data.outcome();
    this.automation = data.automation();
    this.letterHint = blank(data.letterHint());
  }

  private static String blank(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  public int getPriority() {
    return priority;
  }

  /**
   * The criteria of the rule.
   *
   * @return criteria
   */
  public Criteria criteria() {
    return new Criteria(
        segment,
        lineCode,
        productCode,
        mortgaged,
        bucket,
        claimsCondition,
        endorsementCondition,
        paymentCondition,
        daysFrom,
        daysTo);
  }

  public RenewalDisposition getOutcome() {
    return outcome;
  }

  public String getAutomation() {
    return automation;
  }

  public String getLetterHint() {
    return letterHint;
  }

  /**
   * Criteria of a rule; a null value matches anything.
   *
   * @param segment market segment
   * @param lineCode product line
   * @param productCode risk code
   * @param mortgaged mortgaged flag
   * @param bucket CLEAN or REVIEW
   * @param claims NONE, OPEN, PAID or TOTAL_LOSS
   * @param endorsement NONE, POSTED_IN_TERM or PENDING
   * @param payment PAID, OUTSTANDING or DP
   * @param daysFrom days to expiry from
   * @param daysTo days to expiry to
   */
  public record Criteria(
      String segment,
      String lineCode,
      String productCode,
      Boolean mortgaged,
      Bucket bucket,
      String claims,
      String endorsement,
      String payment,
      Integer daysFrom,
      Integer daysTo) {}

  /**
   * Data of a rule.
   *
   * @param priority priority (lowest first)
   * @param criteria criteria
   * @param outcome proposed disposition
   * @param automation AUTO or MANUAL
   * @param letterHint RA, NFR or NAL, may be null
   */
  public record Data(
      int priority,
      Criteria criteria,
      RenewalDisposition outcome,
      String automation,
      String letterHint) {}
}
