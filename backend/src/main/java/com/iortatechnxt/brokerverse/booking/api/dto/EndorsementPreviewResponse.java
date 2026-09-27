package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.EndorsementPostingService.EndorsementPreview;
import java.util.List;

/**
 * Live calculation and journal preview of an endorsement.
 *
 * @param policyYear policy year concerned
 * @param invoice invoice that would be booked, null for a non-financial endorsement
 * @param journal journal lines
 */
public record EndorsementPreviewResponse(
    int policyYear, InvoiceDraftResponse invoice, List<PreviewLineResponse> journal) {

  /**
   * Maps a preview.
   *
   * @param p preview
   * @return response
   */
  public static EndorsementPreviewResponse from(EndorsementPreview p) {
    return new EndorsementPreviewResponse(
        p.policyYear(),
        p.invoice() == null ? null : InvoiceDraftResponse.from(p.invoice()),
        p.journal().stream().map(PreviewLineResponse::from).toList());
  }
}
