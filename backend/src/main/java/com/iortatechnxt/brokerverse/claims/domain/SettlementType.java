package com.iortatechnxt.brokerverse.claims.domain;

/** Partial or final settlement; a final settlement releases the remaining reserve and closes. */
public enum SettlementType {
  /** Interim payment; the claim stays open. */
  PARTIAL,
  /** Last payment: the remaining outstanding reserve is released and the claim closed. */
  FINAL
}
