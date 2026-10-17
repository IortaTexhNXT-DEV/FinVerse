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
 * @param insurerBillingNo the insurer's billing number (BRID-020): required for the product lines
 *     of parameter {@code BOOKING_BILLING_NO_LINES}, unique per company and insurer; null when none
 */
public record BookingOptions(
    LocalDate bookingDate,
    String costCenter,
    Boolean cwt2Percent,
    List<InsurerShare> shares,
    String insurerBillingNo) {

  /** No choices: defaults everywhere. */
  public static final BookingOptions DEFAULTS = new BookingOptions(null, null, null, null, null);

  /** Defensive copy; a blank billing number is none. */
  public BookingOptions {
    shares = shares == null ? List.of() : List.copyOf(shares);
    insurerBillingNo =
        insurerBillingNo == null || insurerBillingNo.isBlank() ? null : insurerBillingNo.strip();
  }

  /**
   * Options without an insurer billing number.
   *
   * @param bookingDate booking date, null for today
   * @param costCenter cost center, null for the account's
   * @param cwt2Percent CWT 2 % flag, null for the default
   * @param shares insurer shares, null or empty for 100 % of the account's insurer
   */
  public BookingOptions(
      LocalDate bookingDate, String costCenter, Boolean cwt2Percent, List<InsurerShare> shares) {
    this(bookingDate, costCenter, cwt2Percent, shares, null);
  }

  /**
   * Options with a booking date and cost center (queue entries, uploads).
   *
   * @param date booking date
   * @param center cost center
   * @return options
   */
  public static BookingOptions of(LocalDate date, String center) {
    return new BookingOptions(date, center, null, null, null);
  }

  /**
   * The same options with an insurer billing number (booking upload, BRID-020).
   *
   * @param billingNo billing number, null or blank for none
   * @return options
   */
  public BookingOptions withBillingNo(String billingNo) {
    return new BookingOptions(bookingDate, costCenter, cwt2Percent, shares, billingNo);
  }
}
