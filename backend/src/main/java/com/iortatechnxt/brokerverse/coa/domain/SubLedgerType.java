package com.iortatechnxt.brokerverse.coa.domain;

/** Sub-ledger controlled by a control account. */
public enum SubLedgerType {
  NONE,
  POLICYHOLDER,
  INTERMEDIARY,
  REINSURER,
  COINSURER,
  BANK,
  VENDOR,
  /** Panel insurers of the broker (premium payable, commission receivable). */
  INSURER
}
