package com.iortatechnxt.brokerverse.period.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request to create a fiscal year.
 *
 * @param companyId company
 * @param yearCode year in which the fiscal year starts
 */
public record CreateFiscalYearRequest(
    @NotNull Long companyId, @Min(2000) @Max(2100) int yearCode) {}
