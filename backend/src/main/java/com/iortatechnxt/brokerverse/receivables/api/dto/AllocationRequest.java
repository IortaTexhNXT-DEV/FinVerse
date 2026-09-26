package com.iortatechnxt.brokerverse.receivables.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Amount of a receipt applied to one debit open item.
 *
 * @param debitItemId debit open item (debit note)
 * @param amount amount in receipt currency
 */
public record AllocationRequest(
    @NotNull Long debitItemId,
    @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {}
