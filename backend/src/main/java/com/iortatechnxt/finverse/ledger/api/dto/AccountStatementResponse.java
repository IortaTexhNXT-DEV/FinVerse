package com.iortatechnxt.finverse.ledger.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Account statement (GL inquiry).
 *
 * @param accountId account
 * @param accountCode code
 * @param accountName name
 * @param from start date
 * @param to end date
 * @param openingBalance opening balance (debit positive)
 * @param totalDebit total debit
 * @param totalCredit total credit
 * @param closingBalance closing balance (debit positive)
 * @param lines lines
 */
public record AccountStatementResponse(
    Long accountId,
    String accountCode,
    String accountName,
    LocalDate from,
    LocalDate to,
    BigDecimal openingBalance,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    BigDecimal closingBalance,
    List<StatementLineResponse> lines) {}
