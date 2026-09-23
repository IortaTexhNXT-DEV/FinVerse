package com.iortatechnxt.finverse.finreport.service;

import java.math.BigDecimal;

/**
 * Aggregated opening balance and period movement of one account for one combination of dimensions.
 * Dimensions not produced by the query are null.
 *
 * @param accountId account
 * @param branchId branch (division)
 * @param costCenter cost centre (department)
 * @param businessLine line of business (activity)
 * @param partyCode sub-ledger party
 * @param currency transaction currency
 * @param openBase net base balance before the period (debit positive)
 * @param openFc net transaction-currency balance before the period
 * @param debitBase period debits in base currency
 * @param creditBase period credits in base currency
 * @param debitFc period debits in transaction currency
 * @param creditFc period credits in transaction currency
 */
public record MovementRow(
    Long accountId,
    Long branchId,
    String costCenter,
    String businessLine,
    String partyCode,
    String currency,
    BigDecimal openBase,
    BigDecimal openFc,
    BigDecimal debitBase,
    BigDecimal creditBase,
    BigDecimal debitFc,
    BigDecimal creditFc) {

  /**
   * Closing net base balance.
   *
   * @return opening + debits - credits
   */
  public BigDecimal closeBase() {
    return openBase.add(debitBase).subtract(creditBase);
  }

  /**
   * Closing net transaction-currency balance.
   *
   * @return opening + debits - credits (FC)
   */
  public BigDecimal closeFc() {
    return openFc.add(debitFc).subtract(creditFc);
  }

  /**
   * Adds the amounts of another row; the dimensions of this row are kept.
   *
   * @param o other row
   * @return sum
   */
  public MovementRow plus(MovementRow o) {
    return new MovementRow(
        accountId,
        branchId,
        costCenter,
        businessLine,
        partyCode,
        currency,
        openBase.add(o.openBase),
        openFc.add(o.openFc),
        debitBase.add(o.debitBase),
        creditBase.add(o.creditBase),
        debitFc.add(o.debitFc),
        creditFc.add(o.creditFc));
  }

  /**
   * Whether the row carries no balance and no movement.
   *
   * @return true when everything is zero
   */
  public boolean isEmpty() {
    return openBase.signum() == 0
        && debitBase.signum() == 0
        && creditBase.signum() == 0
        && openFc.signum() == 0;
  }
}
