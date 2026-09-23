package com.iortatechnxt.finverse.payables.api.dto;

import com.iortatechnxt.finverse.payables.domain.IssuedPdcEvent;
import com.iortatechnxt.finverse.payables.domain.IssuedPdcStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Status history entry of a post-dated cheque.
 *
 * @param eventDate business date
 * @param fromStatus previous status
 * @param toStatus new status
 * @param batchNo journal posted
 * @param remarks remarks
 * @param createdBy user
 * @param createdAt timestamp
 */
public record PdcEventResponse(
    LocalDate eventDate,
    IssuedPdcStatus fromStatus,
    IssuedPdcStatus toStatus,
    String batchNo,
    String remarks,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps an entity.
   *
   * @param e event
   * @return response
   */
  public static PdcEventResponse from(IssuedPdcEvent e) {
    return new PdcEventResponse(
        e.getEventDate(),
        e.getFromStatus(),
        e.getToStatus(),
        e.getBatchNo(),
        e.getRemarks(),
        e.getCreatedBy(),
        e.getCreatedAt());
  }
}
