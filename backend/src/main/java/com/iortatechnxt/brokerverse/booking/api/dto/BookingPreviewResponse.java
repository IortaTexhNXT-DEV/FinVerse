package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.BookingPreviewService.BookingPreview;
import java.time.LocalDate;
import java.util.List;

/**
 * Pre-booking confirmation (BRNB.036): invoices (the first booked now, later policy years
 * scheduled) and the journal of the first.
 *
 * @param bookingDate booking date
 * @param invoices invoices
 * @param journal journal lines
 */
public record BookingPreviewResponse(
    LocalDate bookingDate, List<InvoiceDraftResponse> invoices, List<PreviewLineResponse> journal) {

  /**
   * Maps a preview.
   *
   * @param p preview
   * @return response
   */
  public static BookingPreviewResponse from(BookingPreview p) {
    return new BookingPreviewResponse(
        p.bookingDate(),
        p.invoices().stream().map(InvoiceDraftResponse::from).toList(),
        p.journal().stream().map(PreviewLineResponse::from).toList());
  }
}
