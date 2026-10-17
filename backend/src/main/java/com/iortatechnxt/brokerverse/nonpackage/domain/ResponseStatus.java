package com.iortatechnxt.brokerverse.nonpackage.domain;

/** Status of an insurer's response to a quotation slip (BRNB.009). */
public enum ResponseStatus {
  /** Quotation slip sent, no answer yet. */
  PENDING("Awaiting terms"),
  /** Terms received. */
  RECEIVED("Terms received"),
  /** The insurer declined to quote. */
  DECLINED("Declined");

  private final String label;

  ResponseStatus(String label) {
    this.label = label;
  }

  /**
   * The status as shown in documents.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
