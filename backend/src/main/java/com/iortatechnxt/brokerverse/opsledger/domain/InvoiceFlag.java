package com.iortatechnxt.brokerverse.opsledger.domain;

/** Flags on an Operations invoice, set by the module that owns the condition. */
public enum InvoiceFlag {
  /** On hold: excluded from remittance extraction (remittance, MKTID.003, RMTID.020/031). */
  HOLD,
  /** A negative adjustment is pending (adjustment, RMTID.020/035). */
  PENDING_NEG_ADJ,
  /** Written off (adjustment, ADJID.026). */
  WRITTEN_OFF,
  /** Cancelled (adjustment / booking cancellation). */
  CANCELLED,
  /** Estimated item awaiting the actual figures (commission, CMRID). */
  ESTIMATED
}
