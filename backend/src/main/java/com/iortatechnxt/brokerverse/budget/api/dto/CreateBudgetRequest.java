package com.iortatechnxt.brokerverse.budget.api.dto;

import com.iortatechnxt.brokerverse.budget.domain.BudgetVersionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creates a budget version.
 *
 * @param companyId company
 * @param fiscalYear fiscal year code (must be defined in the calendar)
 * @param versionType ORIGINAL, or REVISED (copies the latest approved version unless {@code
 *     copyFromId} is given)
 * @param name description
 * @param copyFromId optional version whose lines are copied
 */
public record CreateBudgetRequest(
    @NotNull Long companyId,
    @Min(2000) @Max(2100) int fiscalYear,
    @NotNull BudgetVersionType versionType,
    @NotBlank @Size(max = 120) String name,
    Long copyFromId) {}
