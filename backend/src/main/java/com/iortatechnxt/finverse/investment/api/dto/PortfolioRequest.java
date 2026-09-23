package com.iortatechnxt.finverse.investment.api.dto;

import com.iortatechnxt.finverse.investment.domain.Classification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / update investment portfolio request ({@code companyId}, {@code code} and {@code
 * classification} are immutable).
 *
 * @param companyId company
 * @param code code
 * @param name name
 * @param classification PFRS 9 classification
 * @param investmentAccount GL account carrying the holdings
 * @param accruedInterestAccount GL account for accrued interest receivable
 * @param interestIncomeAccount GL account for interest (or dividend) income
 * @param realizedGainAccount GL account for realized gains and losses
 * @param fairValueAccount GL account for fair value changes (FVOCI reserve or FVPL income)
 */
public record PortfolioRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9\\-_]+") String code,
    @NotBlank @Size(max = 120) String name,
    @NotNull Classification classification,
    @NotBlank @Size(max = 30) String investmentAccount,
    @NotBlank @Size(max = 30) String accruedInterestAccount,
    @NotBlank @Size(max = 30) String interestIncomeAccount,
    @NotBlank @Size(max = 30) String realizedGainAccount,
    @Size(max = 30) String fairValueAccount) {}
