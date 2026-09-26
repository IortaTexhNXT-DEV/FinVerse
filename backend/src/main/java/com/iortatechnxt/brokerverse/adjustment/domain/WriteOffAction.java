package com.iortatechnxt.brokerverse.adjustment.domain;

/** What the minimal balance file does with an invoice balance (ADJID.026, OQ11). */
public enum WriteOffAction {
  /** Debit balance (premium still receivable) written off to expense. */
  WRITE_OFF,
  /** Credit balance (client overpaid) taken to other income. */
  CREDIT
}
