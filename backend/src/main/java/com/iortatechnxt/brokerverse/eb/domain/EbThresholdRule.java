package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A value threshold above which a comparative needs Management approval (BRID-016; FR-EB-042): per
 * benefit line or all lines, on the TSI or the annual premium of the recommended (or chosen)
 * proposal, with the approver permission and level and the effective dates. Maintained with
 * maker-checker; the values are BDOI's to give (EBQ11).
 */
@Entity
@Table(name = "eb_threshold_rule")
public class EbThresholdRule extends AuthorizableEntity {

  /** What a rule measures. */
  public enum Measure {
    /** Total sum insured of the line. */
    TSI,
    /** Annual premium of the line. */
    ANNUAL_PREMIUM
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "benefit_line", length = 30)
  private String benefitLine;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Measure measure;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "approver_permission", nullable = false, length = 60)
  private String approverPermission;

  @Column(name = "approval_level", nullable = false)
  private int approvalLevel;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Column(length = 300)
  private String description;

  protected EbThresholdRule() {}

  /**
   * Creates a rule pending authorisation.
   *
   * @param companyId company
   * @param data rule data
   */
  public EbThresholdRule(Long companyId, Data data) {
    this.companyId = companyId;
    apply(data);
  }

  /**
   * Changes the rule; it must be authorised again.
   *
   * @param data rule data
   */
  public void update(Data data) {
    apply(data);
    markModified();
  }

  private void apply(Data data) {
    this.benefitLine = data.benefitLine();
    this.measure = data.measure();
    this.amount = data.amount();
    this.currency = data.currency();
    this.approverPermission = data.approverPermission();
    this.approvalLevel = data.approvalLevel();
    this.effectiveFrom = data.effectiveFrom();
    this.effectiveTo = data.effectiveTo();
    this.description = data.description();
  }

  /**
   * Whether the rule applies to a line on a date.
   *
   * @param line benefit line
   * @param date date
   * @return true when active, effective and for the line or every line
   */
  public boolean appliesTo(String line, LocalDate date) {
    return isActive()
        && (benefitLine == null || benefitLine.equals(line))
        && !date.isBefore(effectiveFrom)
        && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  /**
   * Whether a value reaches the rule's amount.
   *
   * @param value TSI or annual premium
   * @return true when at or above the amount
   */
  public boolean isMetBy(BigDecimal value) {
    return value != null && value.compareTo(amount) >= 0;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBenefitLine() {
    return benefitLine;
  }

  public Measure getMeasure() {
    return measure;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public String getApproverPermission() {
    return approverPermission;
  }

  public int getApprovalLevel() {
    return approvalLevel;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Data of a rule.
   *
   * @param benefitLine benefit line, null for every line
   * @param measure TSI or ANNUAL_PREMIUM
   * @param amount amount, greater than zero
   * @param currency currency
   * @param approverPermission permission of the approvers
   * @param approvalLevel level, from 1
   * @param effectiveFrom effective from
   * @param effectiveTo effective to, may be null
   * @param description description, may be null
   */
  public record Data(
      String benefitLine,
      Measure measure,
      BigDecimal amount,
      String currency,
      String approverPermission,
      int approvalLevel,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      String description) {}
}
