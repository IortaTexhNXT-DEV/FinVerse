package com.iortatechnxt.brokerverse.booking.domain;

/** Status of a booked invoice. */
public enum InvoiceStatus {
  /** A later policy year of a multi-year account, booked when the year starts (BRNB.112). */
  SCHEDULED,
  /** Booked: numbered, posted to the GL and recorded in the sub-ledger. */
  BOOKED,
  /** A scheduled policy year that will not be booked because the account was cancelled. */
  CANCELLED
}
