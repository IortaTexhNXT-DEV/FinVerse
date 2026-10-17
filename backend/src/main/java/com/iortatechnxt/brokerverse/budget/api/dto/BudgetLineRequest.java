package com.iortatechnxt.brokerverse.budget.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * One row of the budget grid.
 *
 * @param accountCode income or expense account
 * @param costCenter optional cost centre
 * @param months twelve monthly amounts, natural sign
 */
public record BudgetLineRequest(
    @NotBlank @Size(max = 30) String accountCode,
    @Size(max = 20) String costCenter,
    @NotNull @Size(min = 12, max = 12)
        List<@NotNull @Digits(integer = 17, fraction = 2) BigDecimal> months) {}
