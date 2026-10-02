package com.iortatechnxt.brokerverse.closing.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Posts the FX revaluation of a period.
 *
 * @param companyId company
 * @param periodId accounting period (revalued as of its last day)
 * @param autoReverse reverse on the first day of the next period
 * @param gainLossAccount unrealized FX gain/loss account (default 4602)
 */
public record FxRevaluationRequest(
    @NotNull Long companyId,
    @NotNull Long periodId,
    boolean autoReverse,
    @Size(max = 30) String gainLossAccount) {}
