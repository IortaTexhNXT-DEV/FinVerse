package com.iortatechnxt.brokerverse.booking.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Confirmation of the queued batch.
 *
 * @param companyId company
 * @param entryIds entries to book, empty for every queued account
 * @param businessDate business date (default booking date), null for today
 */
public record BatchConfirmRequest(
    @NotNull Long companyId, List<Long> entryIds, LocalDate businessDate) {}
