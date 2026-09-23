package com.iortatechnxt.finverse.payables.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entered values of a petty cash disbursement voucher.
 *
 * @param date disbursement date
 * @param payee person paid
 * @param expenseAccountCode expense account
 * @param costCenter cost centre
 * @param description description
 * @param receiptRef receipt / OR number
 * @param amount amount
 */
public record PettyCashDisbursementValues(
    LocalDate date,
    String payee,
    String expenseAccountCode,
    String costCenter,
    String description,
    String receiptRef,
    BigDecimal amount) {}
