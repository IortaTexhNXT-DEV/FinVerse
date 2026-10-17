package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.IncentiveBasis;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An early-remittance incentive rule (RMTID.023, PRCID.028): an insurer pays {@code rate} percent
 * of the basic premium when BDOI remits within {@code windowDays} of the inception or booking date,
 * optionally only for a product line and a segment. Rates are parked (OQ23); the table ships empty
 * in production.
 */
@Entity
@Table(name = "rem_incentive_rule")
public class EarlyIncentiveRule extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "insurer_code", nullable = false, length = 30)
  private String insurerCode;

  @Column(name = "product_line", length = 30)
  private String productLine;

  @Column(length = 40)
  private String segment;

  @Column(nullable = false, precision = 9, scale = 4)
  private BigDecimal rate;

  @Column(name = "window_days", nullable = false)
  private int windowDays;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IncentiveBasis basis;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Column(nullable = false)
  private boolean active = true;

  @Column(length = 250)
  private String description;

  protected EarlyIncentiveRule() {}

  /**
   * A rule.
   *
   * @param companyId company
   * @param terms terms
   */
  public EarlyIncentiveRule(Long companyId, Terms terms) {
    this.companyId = companyId;
    apply(terms);
  }

  /**
   * Changes the terms.
   *
   * @param terms terms
   */
  public void update(Terms terms) {
    apply(terms);
  }

  private void apply(Terms terms) {
    this.insurerCode = terms.insurerCode();
    this.productLine = blankToNull(terms.productLine());
    this.segment = blankToNull(terms.segment());
    this.rate = terms.rate();
    this.windowDays = terms.windowDays();
    this.basis = terms.basis();
    this.effectiveFrom = terms.effectiveFrom();
    this.effectiveTo = terms.effectiveTo();
    this.active = terms.active();
    this.description = terms.description();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * Whether the rule covers an invoice on a date: same insurer, product line and segment (when
   * set), active and effective.
   *
   * @param insurer insurer
   * @param line product line
   * @param invoiceSegment segment
   * @param on date
   * @return true when it applies
   */
  public boolean covers(String insurer, String line, String invoiceSegment, LocalDate on) {
    if (!active || !insurerCode.equals(insurer) || !isEffective(on)) {
      return false;
    }
    return matches(productLine, line) && matches(segment, invoiceSegment);
  }

  private static boolean matches(String ruleValue, String invoiceValue) {
    return ruleValue == null || ruleValue.equals(invoiceValue);
  }

  private boolean isEffective(LocalDate on) {
    return !on.isBefore(effectiveFrom) && (effectiveTo == null || !on.isAfter(effectiveTo));
  }

  /**
   * Whether a remittance on a date is early enough (RMTID.023: within the window).
   *
   * @param inception inception date
   * @param booking booking date
   * @param remittedOn remittance date
   * @return true when within the window
   */
  public boolean isEarly(LocalDate inception, LocalDate booking, LocalDate remittedOn) {
    LocalDate start = basis == IncentiveBasis.BOOKING ? booking : inception;
    return !remittedOn.isAfter(start.plusDays(windowDays));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getProductLine() {
    return productLine;
  }

  public String getSegment() {
    return segment;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public int getWindowDays() {
    return windowDays;
  }

  public IncentiveBasis getBasis() {
    return basis;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public boolean isActive() {
    return active;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Terms of a rule.
   *
   * @param insurerCode insurer
   * @param productLine product line, null for all
   * @param segment segment, null for all
   * @param rate percent of the basic premium
   * @param windowDays days after the basis date
   * @param basis inception or booking
   * @param effectiveFrom first day
   * @param effectiveTo last day, null for open
   * @param active active
   * @param description description
   */
  public record Terms(
      String insurerCode,
      String productLine,
      String segment,
      BigDecimal rate,
      int windowDays,
      IncentiveBasis basis,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active,
      String description) {}
}
