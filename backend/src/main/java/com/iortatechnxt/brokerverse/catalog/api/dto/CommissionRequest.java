package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate.RateValidity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * New or changed commission rate.
 *
 * @param productCode product, empty for every product (ignored on update)
 * @param rate commission %
 * @param effectiveFrom first valid date
 * @param effectiveTo last valid date
 */
public record CommissionRequest(
    @Size(max = 20) String productCode,
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
