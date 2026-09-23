package com.iortatechnxt.finverse.coa.domain;

/** Sub-ledger controlled by a control account. */
public enum SubLedgerType {
  NONE,
  POLICYHOLDER,
  INTERMEDIARY,
  REINSURER,
  COINSURER,
  BANK,
  VENDOR
}
