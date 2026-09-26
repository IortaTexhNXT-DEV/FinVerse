package com.iortatechnxt.brokerverse.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Commission the insurer pays BDOI on a product (percent of net premium), effective dated. A row
 * without product applies to every product of the insurer; a product row wins over it.
 */
@Entity
@Table(name = "cat_commission_rate")
public class CommissionRate extends EffectiveDatedRecord {

  private static final String LABEL = "The commission rate";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "product_code", length = 20, updatable = false)
  private String productCode;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal rate;

  protected CommissionRate() {}

  /**
   * Creates a rate, pending authorization.
   *
   * @param companyId company
   * @param insurerCode insurer party code
   * @param productCode product, null for every product
   * @param validity rate and effectivity
   */
  public CommissionRate(
      Long companyId, String insurerCode, String productCode, RateValidity validity) {
    super(validity.effectiveFrom(), validity.effectiveTo());
    this.companyId = companyId;
    this.insurerCode = insurerCode;
    this.productCode = productCode;
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
    return insurerCode
        + " "
        + (productCode == null ? "ALL" : productCode)
        + " "
        + getEffectiveFrom();
  }

  @Override
  public String catalogDescription() {
    return "Commission " + rate.stripTrailingZeros().toPlainString() + " %";
  }

  @Override
  public Long catalogCompanyId() {
    return companyId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getProductCode() {
    return productCode;
  }

  public BigDecimal getRate() {
    return rate;
  }

  /**
   * A rate with its validity.
   *
   * @param rate rate in percent
   * @param effectiveFrom first valid date
   * @param effectiveTo last valid date, null when open ended
   */
  public record RateValidity(BigDecimal rate, LocalDate effectiveFrom, LocalDate effectiveTo) {}
}
