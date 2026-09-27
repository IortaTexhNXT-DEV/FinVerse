package com.iortatechnxt.brokerverse.booking.api;

/** Security expressions of the booking endpoints. */
final class BookingAccess {

  /** Reading bookings: Processing (booking) and Adjustment. */
  static final String VIEW = "hasAnyAuthority('BOOKING_PROCESS', 'BOOKING_ADJUST')";

  /** Booking, queue and batches, positive endorsements, service invoices (Processing). */
  static final String PROCESS = "hasAuthority('BOOKING_PROCESS')";

  /** Negative endorsements, cancellations and service invoice credits (Adjustment). */
  static final String ADJUST = "hasAuthority('BOOKING_ADJUST')";

  /** Reading the booking setup. */
  static final String SETUP_VIEW =
      "hasAnyAuthority('BOOKING_PROCESS', 'BOOKING_ADJUST', 'MASTER_VIEW')";

  /** Maintaining the booking setup (Business Administrator). */
  static final String SETUP_MAINTAIN = "hasAuthority('MASTER_MAINTAIN')";

  /** Largest page served. */
  static final int MAX_PAGE = 200;

  private BookingAccess() {}
}
