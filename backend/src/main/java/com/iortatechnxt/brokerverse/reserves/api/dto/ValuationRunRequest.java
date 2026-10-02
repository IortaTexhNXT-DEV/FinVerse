package com.iortatechnxt.brokerverse.reserves.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request to calculate a valuation run.
 *
 * @param companyId company
 * @param valuationDate any date of the valuation month (the run uses the month end)
 */
public record ValuationRunRequest(@NotNull Long companyId, @NotNull LocalDate valuationDate) {}
