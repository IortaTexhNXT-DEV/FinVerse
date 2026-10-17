package com.iortatechnxt.brokerverse.receivables.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Post-dated cheque received.
 *
 * @param companyId company
 * @param branchId receiving branch (division)
 * @param receivedDate date received
 * @param partyCode payer party
 * @param department department dimension code
 * @param chequeNo cheque number
 * @param chequeDate cheque (due) date
 * @param draweeBank payer's bank
 * @param currency currency
 * @param amount amount
 * @param bankAccountCode GL bank account the cheque will be deposited in
 * @param debitItemId linked debit note (optional)
 * @param narration narration
 */
public record PdcRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotNull LocalDate receivedDate,
    @NotBlank @Size(max = 30) String partyCode,
    @Size(max = 20) String department,
    @NotBlank @Size(max = 40) String chequeNo,
    @NotNull LocalDate chequeDate,
    @NotBlank @Size(max = 120) String draweeBank,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @NotBlank @Size(max = 30) String bankAccountCode,
    Long debitItemId,
    @Size(max = 250) String narration) {}
