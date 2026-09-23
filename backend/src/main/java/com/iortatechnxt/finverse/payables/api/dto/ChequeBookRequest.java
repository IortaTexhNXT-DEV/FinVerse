package com.iortatechnxt.finverse.payables.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * New cheque book.
 *
 * @param firstNo first leaf number
 * @param lastNo last leaf number
 * @param receivedOn date received
 */
public record ChequeBookRequest(
    @Positive long firstNo, @Positive long lastNo, @NotNull LocalDate receivedOn) {}
