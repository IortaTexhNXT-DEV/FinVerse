package com.iortatechnxt.brokerverse.payables.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Action with an effective date and an optional reason (present, clear, void, cancel, establish).
 *
 * @param date effective (value) date
 * @param reason reason
 */
public record DatedReasonRequest(@NotNull LocalDate date, @Size(max = 200) String reason) {}
