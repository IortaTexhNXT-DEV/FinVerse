package com.iortatechnxt.brokerverse.currency.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Monthly revaluation rate (FRBS 2.2.0).
 *
 * @param currencyCode foreign currency
 * @param month month whose end the rate applies to
 * @param rate base units per foreign unit
 */
public record RevaluationRateRequest(
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currencyCode,
    @NotNull YearMonth month,
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 8)
        BigDecimal rate) {}
