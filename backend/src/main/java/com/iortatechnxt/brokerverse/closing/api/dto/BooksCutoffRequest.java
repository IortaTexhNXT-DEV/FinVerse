package com.iortatechnxt.brokerverse.closing.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Close or reopen the broking books of a period (FRBS 3.4.0).
 *
 * @param companyId company
 * @param periodId period
 * @param reason reason (mandatory to reopen)
 */
public record BooksCutoffRequest(
    @NotNull Long companyId, @NotNull Long periodId, @Size(max = 300) String reason) {}
