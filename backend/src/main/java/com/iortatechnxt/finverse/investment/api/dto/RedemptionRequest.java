package com.iortatechnxt.finverse.investment.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Maturity or sale of a holding. Interest and amortization are first brought up to the date; the
 * realized gain or loss is proceeds + final tax + recycled FVOCI reserve - carrying amount -
 * accrued interest.
 *
 * @param valueDate maturity or sale date
 * @param proceeds cash received, including interest (net of final tax)
 * @param finalTax final tax withheld on the interest
 * @param remarks remarks
 */
public record RedemptionRequest(
    @NotNull LocalDate valueDate,
    @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal proceeds,
    @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal finalTax,
    @Size(max = 250) String remarks) {}
