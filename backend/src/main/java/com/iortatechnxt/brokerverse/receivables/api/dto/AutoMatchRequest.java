package com.iortatechnxt.brokerverse.receivables.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Automatic matching run.
 *
 * @param companyId company
 * @param bankAccountCode GL bank account
 * @param asOf consider items dated on or before this date
 * @param dateWindowDays maximum days between book and bank date (default 7)
 */
public record AutoMatchRequest(
    @NotNull Long companyId,
    @NotBlank String bankAccountCode,
    @NotNull LocalDate asOf,
    @Min(0) @Max(60) Integer dateWindowDays) {}
