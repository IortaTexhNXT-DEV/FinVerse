package com.iortatechnxt.brokerverse.receivables.api.dto;

import com.iortatechnxt.brokerverse.receivables.domain.BankStatementLine;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bank statement line.
 *
 * @param id id
 * @param statementId statement
 * @param lineNo line number
 * @param valueDate value date
 * @param description description
 * @param reference reference
 * @param debit withdrawal
 * @param credit deposit
 * @param balance running balance
 * @param matchId reconciliation match (null when unmatched)
 */
public record BankStatementLineResponse(
    Long id,
    Long statementId,
    int lineNo,
    LocalDate valueDate,
    String description,
    String reference,
    BigDecimal debit,
    BigDecimal credit,
    BigDecimal balance,
    Long matchId) {

  /**
   * Maps an entity.
   *
   * @param l line
   * @return response
   */
  public static BankStatementLineResponse from(BankStatementLine l) {
    return new BankStatementLineResponse(
        l.getId(),
        l.getStatementId(),
        l.getLineNo(),
        l.getValueDate(),
        l.getDescription(),
        l.getReference(),
        l.getDebit(),
        l.getCredit(),
        l.getBalance(),
        l.getMatchId());
  }
}
