package com.iortatechnxt.finverse.budget.domain;

/**
 * Header values of a new budget version.
 *
 * @param companyId company
 * @param fiscalYear fiscal year code
 * @param versionNo version number within company and year
 * @param versionType ORIGINAL or REVISED
 * @param name description
 * @param currency budget currency (company base currency)
 * @param basedOnId version this one was copied from, if any
 */
public record BudgetHeader(
    Long companyId,
    int fiscalYear,
    int versionNo,
    BudgetVersionType versionType,
    String name,
    String currency,
    Long basedOnId) {}
