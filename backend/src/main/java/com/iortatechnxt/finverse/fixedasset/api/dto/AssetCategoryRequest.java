package com.iortatechnxt.finverse.fixedasset.api.dto;

import com.iortatechnxt.finverse.fixedasset.domain.DepreciationMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Create / update asset category request ({@code companyId} and {@code code} are immutable).
 *
 * @param companyId company
 * @param code category code
 * @param name name
 * @param assetAccount GL account for cost
 * @param accumulatedDepreciationAccount GL account for accumulated depreciation
 * @param depreciationExpenseAccount GL account for the depreciation charge
 * @param depreciationMethod default method
 * @param usefulLifeMonths default useful life in months
 * @param residualPercent residual value as % of cost
 */
public record AssetCategoryRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9\\-_]+") String code,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 30) String assetAccount,
    @NotBlank @Size(max = 30) String accumulatedDepreciationAccount,
    @NotBlank @Size(max = 30) String depreciationExpenseAccount,
    @NotNull DepreciationMethod depreciationMethod,
    @Min(1) @Max(1200) int usefulLifeMonths,
    @NotNull @DecimalMin("0") @DecimalMax(value = "100", inclusive = false)
        BigDecimal residualPercent) {}
