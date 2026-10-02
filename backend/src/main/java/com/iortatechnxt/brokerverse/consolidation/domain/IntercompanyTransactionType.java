package com.iortatechnxt.brokerverse.consolidation.domain;

/**
 * Kind of inter-company transaction.
 *
 * <ul>
 *   <li>CHARGE: the creditor company raises a receivable (due-from) against its counter account
 *       (income, bank...), the debtor company records the payable (due-to).
 *   <li>SETTLEMENT: the debtor pays; both balances are reduced.
 * </ul>
 */
public enum IntercompanyTransactionType {
  CHARGE,
  SETTLEMENT
}
