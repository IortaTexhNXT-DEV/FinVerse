package com.iortatechnxt.finverse.receivables.domain;

/** Payment instrument of a receipt. */
public enum ReceiptMode {
  CASH,
  CHEQUE,
  BANK_TRANSFER,
  CARD,
  /** Post-dated cheque converted into a receipt at maturity (see the PDC register). */
  PDC;

  /**
   * Whether the money must still be taken to the bank on a deposit slip.
   *
   * @return true for cash and (current dated) cheques
   */
  public boolean requiresDeposit() {
    return this == CASH || this == CHEQUE;
  }

  /**
   * Whether the instrument is a cheque that can bounce.
   *
   * @return true for cheques and post-dated cheques
   */
  public boolean isCheque() {
    return this == CHEQUE || this == PDC;
  }
}
