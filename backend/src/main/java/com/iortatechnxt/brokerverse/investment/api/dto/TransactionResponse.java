package com.iortatechnxt.brokerverse.investment.api.dto;

import com.iortatechnxt.brokerverse.investment.domain.InvestmentTransaction;
import com.iortatechnxt.brokerverse.investment.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Holding transaction view.
 *
 * @param id id
 * @param holdingId holding
 * @param holdingNo holding number
 * @param runId month-end run (null for other transactions)
 * @param txnType type
 * @param txnDate value date
 * @param fromDate period start (accrual / amortization)
 * @param days days in the period
 * @param amount amount
 * @param cashAmount cash
 * @param finalTax final tax withheld
 * @param gainLoss realized gain / loss or income adjustment
 * @param carryingAfter carrying amount after
 * @param accruedAfter accrued interest after
 * @param batchNo journal
 * @param remarks remarks
 */
public record TransactionResponse(
    Long id,
    Long holdingId,
    String holdingNo,
    Long runId,
    TransactionType txnType,
    LocalDate txnDate,
    LocalDate fromDate,
    int days,
    BigDecimal amount,
    BigDecimal cashAmount,
    BigDecimal finalTax,
    BigDecimal gainLoss,
    BigDecimal carryingAfter,
    BigDecimal accruedAfter,
    String batchNo,
    String remarks) {

  /**
   * Maps an entity.
   *
   * @param t transaction
   * @return response
   */
  public static TransactionResponse from(InvestmentTransaction t) {
    return new TransactionResponse(
        t.getId(),
        t.getHolding().getId(),
        t.getHolding().getHoldingNo(),
        t.getRunId(),
        t.getTxnType(),
        t.getTxnDate(),
        t.getFromDate(),
        t.getDays(),
        t.getAmount(),
        t.getCashAmount(),
        t.getFinalTax(),
        t.getGainLoss(),
        t.getCarryingAfter(),
        t.getAccruedAfter(),
        t.getBatchNo(),
        t.getRemarks());
  }
}
