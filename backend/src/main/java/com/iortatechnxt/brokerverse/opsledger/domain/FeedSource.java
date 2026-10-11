package com.iortatechnxt.brokerverse.opsledger.domain;

/** How an invoice reached the ledger. */
public enum FeedSource {
  /** From booking's {@code InvoiceBooked} event after commit. */
  EVENT,
  /** Replayed from {@code BookingQueryService} (job, endpoint or seed start-up). */
  REPLAY,
  /** An open legacy invoice created by the legacy invoice intake (Data Migration, F01). */
  MIGRATION
}
