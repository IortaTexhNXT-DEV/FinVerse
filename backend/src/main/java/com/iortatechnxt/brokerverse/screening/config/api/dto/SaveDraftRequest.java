package com.iortatechnxt.brokerverse.screening.config.api.dto;

import com.iortatechnxt.brokerverse.screening.config.service.ConfigContent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Saves a draft: header and rows (FR-SS-010 to 017).
 *
 * @param effectiveFrom effective date (today or later)
 * @param changeNote change note
 * @param content rows of the draft's type
 */
public record SaveDraftRequest(
    LocalDate effectiveFrom, @Size(max = 1000) String changeNote, @NotNull ConfigContent content) {}
