package com.iortatechnxt.finverse.closing.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Year-end close request.
 *
 * @param companyId company
 * @param fiscalYearId fiscal year to close
 */
public record YearEndRequest(@NotNull Long companyId, @NotNull Long fiscalYearId) {}
