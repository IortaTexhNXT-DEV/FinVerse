package com.iortatechnxt.brokerverse.subledger.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Manual matching request.
 *
 * @param debitItemId debit item
 * @param creditItemId credit item
 * @param amount amount (null = largest possible)
 * @param matchDate date
 */
public record MatchRequest(
    @NotNull Long debitItemId,
    @NotNull Long creditItemId,
    @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
    @NotNull LocalDate matchDate) {}
