package com.iortatechnxt.finverse.accounting.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Sample event for previewing the journal an accounting rule would produce.
 *
 * @param companyId company
 * @param branchId branch
 * @param eventType event type
 * @param valueDate value date
 * @param currency currency
 * @param businessLine line of business
 * @param partyCode party
 * @param amounts amount components
 * @param accounts account roles
 */
public record SimulationRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotBlank String eventType,
    @NotNull LocalDate valueDate,
    @NotBlank String currency,
    String businessLine,
    String partyCode,
    @NotEmpty Map<String, BigDecimal> amounts,
    Map<String, String> accounts) {}
