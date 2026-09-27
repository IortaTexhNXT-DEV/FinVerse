package com.iortatechnxt.brokerverse.organization.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / update company request.
 *
 * @param code company code (ignored on update)
 * @param name legal name
 * @param baseCurrency ISO currency (ignored on update)
 * @param taxId tax identification number
 * @param address registered address
 * @param fiscalYearStartMonth 1-12
 * @param backValueDays allowed back-dated days for journals
 * @param forwardValueDays allowed forward-dated days for journals
 * @param retainedEarningsAccount account code receiving year-end profit/loss
 */
public record CompanyRequest(
    @NotBlank @Size(max = 10) @Pattern(regexp = "[A-Z0-9]+") String code,
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String baseCurrency,
    @Size(max = 30) String taxId,
    @Size(max = 300) String address,
    @Min(1) @Max(12) int fiscalYearStartMonth,
    @Min(0) @Max(366) int backValueDays,
    @Min(0) @Max(366) int forwardValueDays,
    @Size(max = 30) String retainedEarningsAccount) {}
