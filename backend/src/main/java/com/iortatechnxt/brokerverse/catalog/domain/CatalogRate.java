package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate.RateValidity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A tax or rating factor of Appendix A (DST, premium tax, VAT, FST, VAT on commission, motor
 * OD/Theft coverage factors) in percent, for every line or one product line, effective dated. A
 * line row overrides the all-lines row.
 */
@Entity
@Table(name = "cat_rate")
public class CatalogRate extends EffectiveDatedRecord {

  private static final String LABEL = "The rate";

  @Enumerated(EnumType.STRING)
  @Column(name = "rate_code", nullable = false, length = 30, updatable = false)
  private RateCode rateCode;

  @Column(name = "line_code", length = 30, updatable = false)
  private String lineCode;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal rate;

  protected CatalogRate() {}

  /**
   * Creates a rate row, pending authorization.
   *
   * @param rateCode tax or factor
   * @param lineCode product line, null for every line
   * @param validity rate and effectivity
   */
  public CatalogRate(RateCode rateCode, String lineCode, RateValidity validity) {
    super(validity.effectiveFrom(), validity.effectiveTo());
    this.rateCode = rateCode;
    this.lineCode = lineCode;
    this.rate = requirePercent(validity.rate(), LABEL);
  }

  /**
   * Changes the rate or its effectivity; it must be authorized again.
   *
   * @param validity rate and effectivity
   */
  public void update(RateValidity validity) {
    this.rate = requirePercent(validity.rate(), LABEL);
    setEffectivity(validity.effectiveFrom(), validity.effectiveTo());
    markModified();
  }

  @Override
  public String catalogReference() {
    return rateCode + " " + (lineCode == null ? "ALL" : lineCode) + " " + getEffectiveFrom();
  }

  @Override
  public String catalogDescription() {
    return rateCode + " " + rate.stripTrailingZeros().toPlainString() + " %";
  }

  public RateCode getRateCode() {
    return rateCode;
  }

  public String getLineCode() {
    return lineCode;
  }

  public BigDecimal getRate() {
    return rate;
  }
}
