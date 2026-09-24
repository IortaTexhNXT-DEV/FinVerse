package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.BookingQueueService.EnqueueResult;

/**
 * Outcome of queuing one account.
 *
 * @param arn account
 * @param queued true when queued
 * @param message why it was not queued
 */
public record EnqueueResultResponse(String arn, boolean queued, String message) {

  /**
   * Maps a result.
   *
   * @param r result
   * @return response
   */
  public static EnqueueResultResponse from(EnqueueResult r) {
    return new EnqueueResultResponse(r.arn(), r.queued(), r.message());
  }
}
