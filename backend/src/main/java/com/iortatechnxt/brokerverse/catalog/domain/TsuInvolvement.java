package com.iortatechnxt.brokerverse.catalog.domain;

/** Whether an account of a product needs TSU clearance (BRNB.098). */
public enum TsuInvolvement {
  /** Always cleared by TSU. */
  ALWAYS,
  /** Never routed to TSU. */
  NEVER,
  /** Decided by the TSU routing rules. */
  BY_RULES
}
