package com.iortatechnxt.finverse.journal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request to reverse a posted journal.
 *
 * @param reversalDate value date of the reversal (must be in an open period)
 * @param reason reason recorded in the reversal narration
 */
public record ReverseJournalRequest(
    @NotNull LocalDate reversalDate, @NotBlank @Size(max = 200) String reason) {}
