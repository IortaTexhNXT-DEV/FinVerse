package com.iortatechnxt.brokerverse.booking.domain;

/** What started a booking batch run (BRNB.036). */
public enum BatchTrigger {
  /** A user confirmed the queued batch. */
  MANUAL,
  /** The end-of-day BOOKING_BATCH job. */
  SCHEDULED,
  /** "Book now" on a selection of the workbench. */
  BOOK_NOW,
  /** Booking upload (BOOKING_UPLOAD). */
  UPLOAD
}
