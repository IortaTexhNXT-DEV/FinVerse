package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.EndorsementResult;
import java.util.List;

/**
 * Outcome of an endorsement posting.
 *
 * @param endorsementNo endorsement number
 * @param invoiceNo invoice booked, null for a non-financial endorsement
 * @param journalBatches journal batches posted
 */
public record EndorsementResultResponse(
    String endorsementNo, String invoiceNo, List<String> journalBatches) {

  /**
   * Maps a result.
   *
   * @param r result
   * @return response
   */
  public static EndorsementResultResponse from(EndorsementResult r) {
    return new EndorsementResultResponse(r.endorsementNo(), r.invoiceNo(), r.journalBatches());
  }
}
