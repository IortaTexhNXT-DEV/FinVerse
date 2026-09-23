package com.iortatechnxt.finverse.currency.api.dto;

import com.iortatechnxt.finverse.currency.domain.ExchangeRate;
import com.iortatechnxt.finverse.currency.domain.RateType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Exchange rate view.
 *
 * @param id id
 * @param currencyCode currency
 * @param rateType rate type
 * @param effectiveDate effective date
 * @param rate rate
 * @param createdBy maintained by
 */
public record ExchangeRateResponse(
    Long id,
    String currencyCode,
    RateType rateType,
    LocalDate effectiveDate,
    BigDecimal rate,
    String createdBy) {

  /**
   * Maps an entity.
   *
   * @param r rate
   * @return response
   */
  public static ExchangeRateResponse from(ExchangeRate r) {
    return new ExchangeRateResponse(
        r.getId(),
        r.getCurrencyCode(),
        r.getRateType(),
        r.getEffectiveDate(),
        r.getRate(),
        r.getCreatedBy());
  }
}
