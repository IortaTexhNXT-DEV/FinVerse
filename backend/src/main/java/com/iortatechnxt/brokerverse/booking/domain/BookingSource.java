package com.iortatechnxt.brokerverse.booking.domain;

/** How an invoice was booked (BRNB.036). */
public enum BookingSource {
  /** Individual booking after the pre-booking confirmation. */
  INDIVIDUAL,
  /** A batch confirmed by a user (queue or "Book now" on a selection). */
  BATCH,
  /** The end-of-day BOOKING_BATCH job (queued accounts and due multi-year years). */
  SCHEDULED,
  /** Bulk upload BOOKING_UPLOAD. */
  UPLOAD,
  /** Endorsement or cancellation posting. */
  ENDORSEMENT
}
