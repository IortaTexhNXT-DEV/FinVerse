package com.iortatechnxt.finverse.payables.domain;

/** Status of a post-dated cheque issued. */
public enum PdcStatus {
  /** Issued to the payee; cheque date in the future. */
  ISSUED,
  /** Cheque date reached, not yet presented. */
  DUE,
  /** Presented by the payee and confirmed: PDC liability cleared against the bank. */
  PRESENTED,
  /** Presentation matched with the bank statement. */
  CLEARED,
  /** Stopped before presentation; the payment was reversed. */
  CANCELLED,
  /** Replaced by another cheque for the same payment. */
  REPLACED;

  /**
   * Whether the cheque is still outstanding (the bank will be debited later).
   *
   * @return true for ISSUED and DUE
   */
  public boolean isOutstanding() {
    return this == ISSUED || this == DUE;
  }
}
