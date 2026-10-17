package com.iortatechnxt.brokerverse.investment.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fair value remeasurement of an FVOCI or FVPL holding.
 *
 * @param valuationDate valuation date
 * @param fairValue clean fair value of the whole holding
 * @param remarks price source / remarks
 */
public record FairValueRequest(
    @NotNull LocalDate valuationDate,
    @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal fairValue,
    @Size(max = 250) String remarks) {}
