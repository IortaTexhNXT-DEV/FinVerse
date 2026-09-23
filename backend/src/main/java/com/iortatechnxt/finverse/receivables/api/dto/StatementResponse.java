package com.iortatechnxt.finverse.receivables.api.dto;

import com.iortatechnxt.finverse.receivables.domain.BankStatement;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Imported bank statement.
 *
 * @param id id
 * @param bankAccountCode bank GL account
 * @param statementRef reference
 * @param periodFrom first value date
 * @param periodTo last value date
 * @param openingBalance opening balance
 * @param closingBalance closing balance
 * @param totalDebit withdrawals
 * @param totalCredit deposits
 * @param lineCount lines
 * @param fileName file name
 * @param createdBy importer
 * @param createdAt import time
 */
public record StatementResponse(
    Long id,
    String bankAccountCode,
    String statementRef,
    LocalDate periodFrom,
    LocalDate periodTo,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    int lineCount,
    String fileName,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps an entity.
   *
   * @param s statement
   * @return response
   */
  public static StatementResponse from(BankStatement s) {
    return new StatementResponse(
        s.getId(),
        s.getBankAccountCode(),
        s.getStatementRef(),
        s.getPeriodFrom(),
        s.getPeriodTo(),
        s.getOpeningBalance(),
        s.getClosingBalance(),
        s.getTotalDebit(),
        s.getTotalCredit(),
        s.getLineCount(),
        s.getFileName(),
        s.getCreatedBy(),
        s.getCreatedAt());
  }
}
