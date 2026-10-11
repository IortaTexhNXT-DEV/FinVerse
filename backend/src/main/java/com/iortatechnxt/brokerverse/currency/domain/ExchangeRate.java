package com.iortatechnxt.brokerverse.currency.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Exchange rate: base-currency units per one unit of {@code currencyCode}, effective from a date
 * until superseded by a later rate of the same type.
 */
@Entity
@Table(name = "cur_exchange_rate")
public class ExchangeRate extends BaseEntity {

  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "rate_type", nullable = false, length = 20)
  private RateType rateType;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal rate;

  protected ExchangeRate() {}

  /**
   * Creates a rate.
   *
   * @param currencyCode foreign currency
   * @param rateType rate type
   * @param effectiveDate effective date
   * @param rate base units per foreign unit
   */
  public ExchangeRate(
      String currencyCode, RateType rateType, LocalDate effectiveDate, BigDecimal rate) {
    this.currencyCode = currencyCode;
    this.rateType = rateType;
    this.effectiveDate = effectiveDate;
    this.rate = rate;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public RateType getRateType() {
    return rateType;
  }

  public LocalDate getEffectiveDate() {
    return effectiveDate;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public void setRate(BigDecimal rate) {
    this.rate = rate;
  }
}
