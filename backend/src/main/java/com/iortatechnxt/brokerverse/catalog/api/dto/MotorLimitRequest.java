package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.MotorCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.MotorLimit.LimitPrice;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * New or changed motor limit row.
 *
 * @param coverage BI or PD (ignored on update)
 * @param limitAmount limit (ignored on update)
 * @param premium premium
 * @param effectiveFrom first valid date
 * @param effectiveTo last valid date
 */
public record MotorLimitRequest(
    @NotNull MotorCoverage coverage,
    @NotNull @DecimalMin("0.01") BigDecimal limitAmount,
    @NotNull @DecimalMin("0") BigDecimal premium,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo) {

  /**
   * Premium and validity.
   *
   * @return price
   */
  public LimitPrice price() {
    return new LimitPrice(premium, effectiveFrom, effectiveTo);
  }
}
