package com.iortatechnxt.finverse.currency.api.dto;

import com.iortatechnxt.finverse.currency.domain.RateType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Exchange rate maintenance request.
 *
 * @param currencyCode currency
 * @param rateType rate type
 * @param effectiveDate effective date
 * @param rate base units per foreign unit
 */
public record ExchangeRateRequest(
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currencyCode,
    @NotNull RateType rateType,
    @NotNull LocalDate effectiveDate,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal rate) {}
