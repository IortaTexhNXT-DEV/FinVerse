package com.iortatechnxt.brokerverse.underwriting.api.dto;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.underwriting.domain.IterationValues;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Figures of a quotation iteration at 100 %.
 *
 * @param sumInsured sum insured
 * @param grossPremium gross premium
 * @param discount discount amount
 * @param loading loading amount
 * @param charges other charges
 * @param remarks remarks
 */
public record IterationRequest(
    @NotNull @DecimalMin("0") BigDecimal sumInsured,
    @NotNull @DecimalMin("0") BigDecimal grossPremium,
    @DecimalMin("0") BigDecimal discount,
    @DecimalMin("0") BigDecimal loading,
    @DecimalMin("0") BigDecimal charges,
    @Size(max = 300) String remarks) {

  /**
   * Domain values.
   *
   * @return values
   */
  public IterationValues toValues() {
    return new IterationValues(
        sumInsured,
        grossPremium,
        Money.nz(discount),
        Money.nz(loading),
        Money.nz(charges),
        remarks);
  }
}
