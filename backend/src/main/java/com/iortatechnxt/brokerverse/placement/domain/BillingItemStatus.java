package com.iortatechnxt.brokerverse.placement.domain;

/** Payment outcome of a billed account (BRNB.067). */
public enum BillingItemStatus {
  /** Billed; no payment report yet. */
  BILLED,
  /** Reported paid by CLPC. */
  PAID,
  /** Reported unpaid by CLPC. */
  UNPAID
}
