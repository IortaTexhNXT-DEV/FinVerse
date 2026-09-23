package com.iortatechnxt.finverse.subledger.domain;

/**
 * Direction of an open item from the company's point of view.
 *
 * <ul>
 *   <li>DEBIT: the party owes the company (debit note, invoice to client, payment made to a
 *       supplier, amount recoverable from a reinsurer).
 *   <li>CREDIT: the company owes the party or has received money (receipt, supplier invoice,
 *       commission payable, claim payable).
 * </ul>
 *
 * A DEBIT item is settled by matching CREDIT items of the same party and currency.
 */
public enum ItemDirection {
  DEBIT,
  CREDIT;

  /**
   * The direction that settles this one.
   *
   * @return opposite direction
   */
  public ItemDirection opposite() {
    return this == DEBIT ? CREDIT : DEBIT;
  }
}
