package com.iortatechnxt.brokerverse.consolidation.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Business values of an inter-company transaction.
 *
 * @param type CHARGE or SETTLEMENT
 * @param creditorCompanyId company owed the money (due-from side)
 * @param debtorCompanyId company owing the money (due-to side)
 * @param valueDate value date of both journals
 * @param currency transaction currency
 * @param amount positive amount
 * @param creditorAccount creditor's counter account (income, bank...)
 * @param debtorAccount debtor's counter account (expense, bank...)
 * @param narration narration
 */
public record IntercompanyValues(
    IntercompanyTransactionType type,
    Long creditorCompanyId,
    Long debtorCompanyId,
    LocalDate valueDate,
    String currency,
    BigDecimal amount,
    String creditorAccount,
    String debtorAccount,
    String narration) {}
