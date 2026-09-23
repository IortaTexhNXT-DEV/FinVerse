package com.iortatechnxt.finverse.journal.api.dto;

import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.journal.domain.RecurrenceFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Recurring journal template create / update request.
 *
 * @param companyId company
 * @param branchId branch
 * @param name unique name within the company
 * @param journalType MANUAL, ADJUSTMENT or ACCRUAL
 * @param currency header currency
 * @param narration narration of generated journals
 * @param reference reference of generated journals
 * @param frequency MONTHLY, QUARTERLY or ANNUALLY
 * @param dayOfMonth day of month (31 = month end)
 * @param startDate first possible occurrence
 * @param endDate last possible occurrence (optional)
 * @param autoReverse also generate a reversing draft on the first day of the next period
 * @param autoSubmit submit generated journals for approval instead of leaving drafts
 * @param lines balanced lines
 */
public record RecurringTemplateRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotBlank @Size(max = 120) String name,
    @NotNull JournalType journalType,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotBlank @Size(max = 450) String narration,
    @Size(max = 60) String reference,
    @NotNull RecurrenceFrequency frequency,
    @Min(1) @Max(31) int dayOfMonth,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    boolean autoReverse,
    boolean autoSubmit,
    @NotNull @Size(min = 2, max = 200) List<@Valid JournalLineRequest> lines) {}
