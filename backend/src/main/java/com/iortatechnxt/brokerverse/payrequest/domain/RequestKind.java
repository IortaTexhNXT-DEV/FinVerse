package com.iortatechnxt.brokerverse.payrequest.domain;

/**
 * Kind of a Marketing request (MKT 1.2.0, 1.19.0; ACCOUNTING_DISBURSEMENT_DESIGN 5.2), with its
 * workflow (V890) and number prefix.
 */
public enum RequestKind {
  /** Refund Request Form (RRF) to a client, one line per AR. */
  REFUND("PRQ_REFUND", "RRF"),
  /** Request for Payment (RFP) of an employee cash advance. */
  CASH_ADVANCE("PRQ_CASH_ADVANCE", "RFP"),
  /** Cancellation of a disbursed check of an earlier request. */
  CHECK_CANCELLATION("PRQ_CHECK_CANCEL", "CCR");

  private final String workflow;
  private final String prefix;

  RequestKind(String workflow, String prefix) {
    this.workflow = workflow;
    this.prefix = prefix;
  }

  /**
   * Workflow of the kind.
   *
   * @return workflow code
   */
  public String workflow() {
    return workflow;
  }

  /**
   * Number prefix ({@code RRF-<yyyy>}, {@code RFP-<yyyy>}, {@code CCR-<yyyy>}).
   *
   * @return prefix
   */
  public String prefix() {
    return prefix;
  }

  /**
   * Whether the kind pays someone through Disbursement.
   *
   * @return true for refunds and cash advances
   */
  public boolean pays() {
    return this != CHECK_CANCELLATION;
  }
}
