package com.iortatechnxt.brokerverse.payrequest.domain;

/** Where a cash-advance liquidation stands (Appendix D, AQ18). */
public enum LiquidationStatus {
  /** Being prepared by the employee. */
  DRAFT,
  /** Submitted for checking. */
  SUBMITTED,
  /** Checked and posted (event PRQ_CA_LIQUIDATION). */
  POSTED
}
