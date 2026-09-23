package com.iortatechnxt.finverse.payables.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One payment advised to the bank.
 *
 * @param voucherNo payment voucher number
 * @param chequeNo cheque number (blank for transfers)
 * @param paymentDate payment date
 * @param vendorCode payee party code
 * @param vendorName payee name
 * @param vendorAccountNo payee bank account number
 * @param currency currency
 * @param amount amount
 */
public record NotificationRecord(
    String voucherNo,
    String chequeNo,
    LocalDate paymentDate,
    String vendorCode,
    String vendorName,
    String vendorAccountNo,
    String currency,
    BigDecimal amount) {}
