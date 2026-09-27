package com.iortatechnxt.brokerverse.nonpackage.domain;

/** Status of an insurer's response to a quotation slip (BRNB.009). */
public enum ResponseStatus {
  /** Quotation slip sent, no answer yet. */
  PENDING,
  /** Terms received. */
  RECEIVED,
  /** The insurer declined to quote. */
  DECLINED
}
