package com.iortatechnxt.brokerverse.payables.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Replacement of a post-dated cheque by a new leaf.
 *
 * @param chequeDate date of the new cheque
 * @param date replacement date
 * @param reason reason
 */
public record ReplaceChequeRequest(
    @NotNull LocalDate chequeDate,
    @NotNull LocalDate date,
    @NotBlank @Size(max = 200) String reason) {}
