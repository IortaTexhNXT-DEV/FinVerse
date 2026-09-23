package com.iortatechnxt.finverse.claims.domain;

/** Kind of a claim movement line: a change of the estimate, or an amount actually settled. */
public enum MovementKind {
  /** Approved change of the payment or recovery estimate. */
  ESTIMATE,
  /** Approved settlement (payment side) or recovery received (recovery side). */
  PAID
}
