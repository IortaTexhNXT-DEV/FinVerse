package com.iortatechnxt.finverse.receivables.domain;

/** Whether the money of a receipt has been banked. */
public enum DepositStatus {
  /** Transfers and card payments arrive directly in the bank. */
  NOT_REQUIRED,
  UNDEPOSITED,
  IN_SLIP,
  DEPOSITED
}
