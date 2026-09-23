package com.iortatechnxt.finverse.payables.domain;

/** How a payment is made. */
public enum PaymentMode {
  /** Current-dated cheque drawn on the bank account's cheque book. */
  CHEQUE,
  /** Electronic transfer; included in the payment notification file to the bank. */
  BANK_TRANSFER,
  /** Post-dated cheque: registered in the PDC-issued register and cleared on maturity. */
  PDC;

  /**
   * Whether the mode consumes a cheque leaf.
   *
   * @return true for cheques and PDCs
   */
  public boolean usesCheque() {
    return this != BANK_TRANSFER;
  }
}
