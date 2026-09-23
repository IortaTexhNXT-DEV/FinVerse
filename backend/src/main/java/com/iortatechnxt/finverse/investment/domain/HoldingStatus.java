package com.iortatechnxt.finverse.investment.domain;

/** Life cycle of an investment holding. */
public enum HoldingStatus {
  /** Captured by a maker; the purchase is posted when a checker approves it. */
  PENDING_APPROVAL,
  /** Held; accrues interest and amortizes. */
  ACTIVE,
  /** Redeemed at maturity. */
  MATURED,
  /** Sold before maturity. */
  SOLD
}
