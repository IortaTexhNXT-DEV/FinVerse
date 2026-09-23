package com.iortatechnxt.finverse.journal.api.dto;

import com.iortatechnxt.finverse.journal.domain.JournalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Manual journal create / update request.
 *
 * @param companyId company
 * @param branchId originating branch
 * @param journalType MANUAL, ADJUSTMENT or ACCRUAL
 * @param valueDate accounting date
 * @param currency header currency
 * @param narration narration
 * @param reference reference
 * @param lines lines (at least two for submission; drafts may be incomplete)
 */
public record JournalRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotNull JournalType journalType,
    @NotNull LocalDate valueDate,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotBlank @Size(max = 500) String narration,
    @Size(max = 60) String reference,
    @NotEmpty @Size(max = 500) List<@Valid JournalLineRequest> lines) {}
