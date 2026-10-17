package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate.RateValidity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * New or changed short-period row.
 *
 * @param monthsCovered months covered (ignored on update)
 * @param percentOfAnnual percent of annual premium
 * @param effectiveFrom first valid date
 * @param effectiveTo last valid date
 */
public record ShortPeriodRequest(
    @Min(1) @Max(12) int monthsCovered,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal percentOfAnnual,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo) {

  /**
   * Percentage and validity.
   *
   * @return validity
   */
  public RateValidity validity() {
    return new RateValidity(percentOfAnnual, effectiveFrom, effectiveTo);
  }
}
