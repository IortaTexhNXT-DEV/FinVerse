package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Individual booking of an account (pre-booking confirmation and booking, BRNB.027/036).
 *
 * @param arn Account Reference Number
 * @param bookingDate booking date, null for today
 * @param costCenter cost center, null for the account's
 * @param cwt2Percent CWT 2 % flag, null for the default
 * @param shares insurer shares (co-insurance), empty for the account's insurer at 100 %
 * @param insurerBillingNo insurer billing number (BRID-020): required for the lines of {@code
 *     BOOKING_BILLING_NO_LINES}, unique per insurer; null when none
 */
public record BookRequest(
    @NotBlank @Size(max = 30) String arn,
    LocalDate bookingDate,
    @Size(max = 20) String costCenter,
    Boolean cwt2Percent,
    List<@Valid ShareDto> shares,
    @Size(max = 60) String insurerBillingNo) {

  /**
   * The booking options.
   *
   * @return options
   */
  public BookingOptions toOptions() {
    return new BookingOptions(
        bookingDate,
        costCenter,
        cwt2Percent,
        shares == null ? List.of() : shares.stream().map(ShareDto::toShare).toList(),
        insurerBillingNo);
  }
}
