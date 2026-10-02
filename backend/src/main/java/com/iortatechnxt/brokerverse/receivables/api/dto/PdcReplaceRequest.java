package com.iortatechnxt.brokerverse.receivables.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Replacement of a post-dated cheque by a new one.
 *
 * @param date replacement date
 * @param chequeNo new cheque number
 * @param chequeDate new cheque date
 * @param draweeBank new drawee bank
 * @param amount new amount
 * @param remarks remarks
 */
public record PdcReplaceRequest(
    @NotNull LocalDate date,
    @NotBlank @Size(max = 40) String chequeNo,
    @NotNull LocalDate chequeDate,
    @NotBlank @Size(max = 120) String draweeBank,
    @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @Size(max = 200) String remarks) {}
