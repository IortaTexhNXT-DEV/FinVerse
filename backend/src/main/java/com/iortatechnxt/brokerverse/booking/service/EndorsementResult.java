package com.iortatechnxt.brokerverse.booking.service;

import java.util.List;

/**
 * Outcome of {@link EndorsementPostingService#post}.
 *
 * @param endorsementNo endorsement number ({@code EN-<yyyy>-n})
 * @param invoiceNo invoice booked for a financial endorsement or cancellation, null otherwise
 * @param journalBatches journal batches posted (none for a non-financial endorsement)
 * @param invoice the booked invoice as published, null for a non-financial endorsement
 */
public record EndorsementResult(
    String endorsementNo, String invoiceNo, List<String> journalBatches, InvoiceBooked invoice) {

  /** Defensive copy. */
  public EndorsementResult {
    journalBatches = List.copyOf(journalBatches);
  }
}
