package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.BookingWorkbenchService.Counts;

/**
 * Tiles of the Booking Workbench.
 *
 * @param readyToBook issued accounts not queued
 * @param queued queued for batch
 * @param bookedToday invoices booked today
 * @param failed failed queue entries
 */
public record WorkbenchCountsResponse(
    long readyToBook, long queued, long bookedToday, long failed) {

  /**
   * Maps the counts.
   *
   * @param c counts
   * @return response
   */
  public static WorkbenchCountsResponse from(Counts c) {
    return new WorkbenchCountsResponse(c.readyToBook(), c.queued(), c.bookedToday(), c.failed());
  }
}
