package com.iortatechnxt.finverse.tax.domain;

import java.time.LocalDate;

/**
 * What the payer states when paying a return.
 *
 * @param paidOn payment (value) date
 * @param bankAccountCode company bank account paid from (payables bank account code), null when
 *     nothing is payable
 * @param reference bank / eFPS payment reference
 */
public record RemittanceFacts(LocalDate paidOn, String bankAccountCode, String reference) {}
