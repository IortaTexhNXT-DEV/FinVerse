package com.iortatechnxt.brokerverse.closing.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Scheduled month-end close (FRBS 2.6.0).
 *
 * @param companyId company
 * @param periodId period to close
 * @param scheduledAt when; null for the proposal (2nd banking day, 17:00 Manila)
 */
public record CloseScheduleRequest(
    @NotNull Long companyId, @NotNull Long periodId, Instant scheduledAt) {}
