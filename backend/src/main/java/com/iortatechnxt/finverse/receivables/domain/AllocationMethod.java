package com.iortatechnxt.finverse.receivables.domain;

/** How a receipt is allocated to the payer's open debit items. */
public enum AllocationMethod {
  /** Items and amounts chosen by the user. */
  MANUAL,
  /** Oldest due items first, at approval. */
  FIFO,
  /** Nothing allocated: the whole amount is kept on account. */
  NONE
}
