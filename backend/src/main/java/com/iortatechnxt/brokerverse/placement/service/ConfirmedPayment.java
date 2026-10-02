package com.iortatechnxt.brokerverse.placement.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A payment confirmed by a {@link PaymentConfirmationSource} for one account.
 *
 * @param arn Account Reference Number
 * @param reference unique reference within the source (report line, receipt application), at most
 *     80 characters
 * @param amount amount applied to the account, may be null
 * @param paidOn payment date, may be null
 * @param description short description shown with the evidence
 */
public record ConfirmedPayment(
    String arn, String reference, BigDecimal amount, LocalDate paidOn, String description) {}
