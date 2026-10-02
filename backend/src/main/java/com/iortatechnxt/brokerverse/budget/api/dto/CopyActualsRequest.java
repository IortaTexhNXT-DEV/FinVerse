package com.iortatechnxt.brokerverse.budget.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Fills a draft budget from the actuals of a prior fiscal year.
 *
 * @param sourceYear fiscal year whose actuals are copied
 * @param adjustmentPercent uplift (positive) or reduction (negative) in percent
 */
public record CopyActualsRequest(
    @Min(2000) @Max(2100) int sourceYear,
    @NotNull @DecimalMin("-100") @DecimalMax("1000") BigDecimal adjustmentPercent) {}
