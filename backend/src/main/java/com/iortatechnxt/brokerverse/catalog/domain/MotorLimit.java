package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Motor excess BI / PD limit and its premium (Appendix A: "Excess BI / PD premium amounts per limit
 * table"), effective dated.
 */
@Entity
@Table(name = "cat_motor_limit")
public class MotorLimit extends EffectiveDatedRecord {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 2, updatable = false)
  private MotorCoverage coverage;

  @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal limitAmount;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal premium;

  protected MotorLimit() {}

  /**
   * Creates a limit row, pending authorization.
   *
   * @param coverage BI or PD
   * @param limitAmount limit of liability
   * @param price premium and effectivity
   */
  public MotorLimit(MotorCoverage coverage, BigDecimal limitAmount, LimitPrice price) {
    super(price.effectiveFrom(), price.effectiveTo());
    if (limitAmount == null || limitAmount.signum() <= 0) {
      throw new BusinessRuleException("LIMIT_INVALID", "The limit must be greater than zero");
    }
    this.coverage = coverage;
    this.limitAmount = limitAmount;
    this.premium = requirePremium(price.premium());
  }

  /**
   * Changes the premium or its effectivity; it must be authorized again.
   *
   * @param price premium and effectivity
   */
  public void update(LimitPrice price) {
    this.premium = requirePremium(price.premium());
    setEffectivity(price.effectiveFrom(), price.effectiveTo());
    markModified();
  }

  private static BigDecimal requirePremium(BigDecimal premium) {
    if (premium == null || premium.signum() < 0) {
      throw new BusinessRuleException("LIMIT_PREMIUM_INVALID", "The premium cannot be negative");
    }
    return premium;
  }

  @Override
  public String catalogReference() {
    return coverage + " " + limitAmount.toPlainString() + " " + getEffectiveFrom();
  }

  @Override
  public String catalogDescription() {
    return coverage + " limit " + limitAmount.toPlainString() + ": " + premium.toPlainString();
  }

  public MotorCoverage getCoverage() {
    return coverage;
  }

  public BigDecimal getLimitAmount() {
    return limitAmount;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  /**
   * Premium of a limit with its validity.
   *
   * @param premium premium amount
   * @param effectiveFrom first valid date
   * @param effectiveTo last valid date, null when open ended
   */
  public record LimitPrice(BigDecimal premium, LocalDate effectiveFrom, LocalDate effectiveTo) {}
}
