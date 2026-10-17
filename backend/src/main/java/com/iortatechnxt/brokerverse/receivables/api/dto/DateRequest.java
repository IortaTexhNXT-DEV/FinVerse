package com.iortatechnxt.brokerverse.receivables.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Dated action with optional remarks (deposit confirmation, PDC clearing, PDC deposit).
 *
 * @param date effective date
 * @param remarks remarks
 */
public record DateRequest(@NotNull LocalDate date, @Size(max = 200) String remarks) {}
