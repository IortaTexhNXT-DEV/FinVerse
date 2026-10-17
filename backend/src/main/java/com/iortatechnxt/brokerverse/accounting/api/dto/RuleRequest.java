package com.iortatechnxt.brokerverse.accounting.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Accounting rule maintenance request.
 *
 * @param companyId company
 * @param eventType event type code
 * @param name name
 * @param businessLine optional line-of-business condition
 * @param currency optional currency condition
 * @param priority tie breaker (lower wins)
 * @param effectiveFrom first effective date
 * @param effectiveTo optional last effective date
 * @param lines debit/credit instructions
 */
public record RuleRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 40) String eventType,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 20) String businessLine,
    @Pattern(regexp = "^$|[A-Z]{3}") String currency,
    @Min(1) int priority,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo,
    @NotEmpty List<@Valid RuleLineDto> lines) {}
