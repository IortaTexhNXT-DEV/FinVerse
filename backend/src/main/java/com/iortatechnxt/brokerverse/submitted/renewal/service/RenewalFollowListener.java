package com.iortatechnxt.brokerverse.submitted.renewal.service;

import com.iortatechnxt.brokerverse.account.service.AccountStatusChanged;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Follows the renewal accounts of submitted policies (BRIDSP-26, 27; design sections 2.1 and 3.5):
 * the status changes of the account and its booking update the masterlist record in the same
 * transaction. Accounts that are not renewals of a submitted policy are ignored.
 */
@Component
public class RenewalFollowListener {

  private final RenewalFollowService follow;

  /**
   * Creates the listener.
   *
   * @param follow renewal follow-up
   */
  public RenewalFollowListener(RenewalFollowService follow) {
    this.follow = follow;
  }

  /**
   * An account changed status.
   *
   * @param event status change
   */
  @EventListener
  public void on(AccountStatusChanged event) {
    follow.accountChanged(event.arn(), event.to());
  }

  /**
   * An invoice was booked.
   *
   * @param event booking
   */
  @EventListener
  public void on(InvoiceBooked event) {
    if (event.arn() != null && event.endorsementNo() == null) {
      follow.booked(event.arn(), event.invoiceNo(), event.bookingDate());
    }
  }
}
