package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate.RateValidity;
import com.iortatechnxt.brokerverse.catalog.domain.RateCode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * New or changed tax / rating factor row.
 *
 * @param rateCode tax or factor (ignored on update)
 * @param lineCode product line, empty for every line (ignored on update)
 * @param rate rate %
 * @param effectiveFrom first valid date
 * @param effectiveTo last valid date
 */
public record RateRequest(
    @NotNull RateCode rateCode,
    String lineCode,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal rate,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo) {

  /**
   * Rate and validity.
   *
   * @return validity
   */
  public RateValidity validity() {
    return new RateValidity(rate, effectiveFrom, effectiveTo);
  }
}
