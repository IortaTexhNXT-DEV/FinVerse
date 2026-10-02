package com.iortatechnxt.brokerverse.receivables.api.dto;

import com.iortatechnxt.brokerverse.receivables.domain.BankReconciliation;
import com.iortatechnxt.brokerverse.receivables.domain.ReconciliationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Saved bank reconciliation.
 *
 * @param id id
 * @param bankAccountCode bank GL account
 * @param asOfDate statement date
 * @param bookBalance book balance
 * @param bookDebitsNotInBank deposits in transit
 * @param bookCreditsNotInBank unpresented cheques
 * @param bankDebitsNotInBook bank debits not in book
 * @param bankCreditsNotInBook bank credits not in book
 * @param computedBankBalance balance per bank derived from the book
 * @param statementBalance balance per statement
 * @param difference unexplained difference
 * @param status status
 * @param finalizedBy user
 * @param finalizedAt time
 */
public record ReconciliationResponse(
    Long id,
    String bankAccountCode,
    LocalDate asOfDate,
    BigDecimal bookBalance,
    BigDecimal bookDebitsNotInBank,
    BigDecimal bookCreditsNotInBank,
    BigDecimal bankDebitsNotInBook,
    BigDecimal bankCreditsNotInBook,
    BigDecimal computedBankBalance,
    BigDecimal statementBalance,
    BigDecimal difference,
    ReconciliationStatus status,
    String finalizedBy,
    Instant finalizedAt) {

  /**
   * Maps an entity.
   *
   * @param r reconciliation
   * @return response
   */
  public static ReconciliationResponse from(BankReconciliation r) {
    return new ReconciliationResponse(
        r.getId(),
        r.getBankAccountCode(),
        r.getAsOfDate(),
        r.getBookBalance(),
        r.getBookDebitsNotInBank(),
        r.getBookCreditsNotInBank(),
        r.getBankDebitsNotInBook(),
        r.getBankCreditsNotInBook(),
        r.getComputedBankBalance(),
        r.getStatementBalance(),
        r.getDifference(),
        r.getStatus(),
        r.getFinalizedBy(),
        r.getFinalizedAt());
  }
}
