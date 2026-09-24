package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.InsurerShare;
import java.time.LocalDate;
import java.util.List;

/**
 * Choices made when booking an account (pre-booking confirmation, queue edit, upload); every value
 * is optional.
 *
 * @param bookingDate booking date, null for today
 * @param costCenter cost center, null for the account's (default from the sales organisation,
 *     BRNB.108)
 * @param cwt2Percent CWT 2 % flag, null for the segment default
 * @param shares insurer shares for co-insurance, null or empty for the account's insurer at 100 %
 */
public record BookingOptions(
    LocalDate bookingDate, String costCenter, Boolean cwt2Percent, List<InsurerShare> shares) {

  /** No choices: defaults everywhere. */
  public static final BookingOptions DEFAULTS = new BookingOptions(null, null, null, null);

  /** Defensive copy. */
  public BookingOptions {
    shares = shares == null ? List.of() : List.copyOf(shares);
  }

  /**
   * Options with a booking date and cost center (queue entries, uploads).
   *
   * @param date booking date
   * @param center cost center
   * @return options
   */
  public static BookingOptions of(LocalDate date, String center) {
    return new BookingOptions(date, center, null, null);
  }
}
