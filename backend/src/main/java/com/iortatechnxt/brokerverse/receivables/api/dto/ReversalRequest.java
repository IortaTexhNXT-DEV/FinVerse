package com.iortatechnxt.brokerverse.receivables.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Dated action with a mandatory reason (reject, cancel, bounce, return).
 *
 * @param date effective date
 * @param reason reason
 */
public record ReversalRequest(@NotNull LocalDate date, @NotBlank @Size(max = 200) String reason) {}
