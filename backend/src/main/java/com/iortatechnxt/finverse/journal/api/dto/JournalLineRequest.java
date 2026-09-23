package com.iortatechnxt.finverse.journal.api.dto;

import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Journal line input.
 *
 * @param accountCode GL account code
 * @param side DEBIT or CREDIT
 * @param amount positive amount in line currency
 * @param currency line currency (defaults to header currency)
 * @param exchangeRate explicit rate (defaults to the SPOT rate on the value date)
 * @param branchId line branch for inter-branch entries (defaults to header branch)
 * @param costCenter cost centre code
 * @param businessLine line of business code
 * @param partyCode sub-ledger party code
 * @param reference reference
 * @param narration narration
 */
public record JournalLineRequest(
    @NotBlank @Size(max = 30) String accountCode,
    @NotNull BalanceSide side,
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @Pattern(regexp = "[A-Z]{3}") String currency,
    @DecimalMin(value = "0", inclusive = false) BigDecimal exchangeRate,
    Long branchId,
    @Size(max = 20) String costCenter,
    @Size(max = 20) String businessLine,
    @Size(max = 30) String partyCode,
    @Size(max = 60) String reference,
    @Size(max = 250) String narration) {}
