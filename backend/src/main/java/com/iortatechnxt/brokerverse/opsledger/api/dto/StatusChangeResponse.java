package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceStatusChange;
import java.time.Instant;

/**
 * A status, flag or lock change of an invoice (RMTID.032/040).
 *
 * @param id id
 * @param field field
 * @param from old value
 * @param to new value
 * @param module module
 * @param reason reason
 * @param changedAt time
 * @param changedBy user
 */
public record StatusChangeResponse(
    Long id,
    String field,
    String from,
    String to,
    String module,
    String reason,
    Instant changedAt,
    String changedBy) {

  /**
   * Maps a change.
   *
   * @param c change
   * @return response
   */
  public static StatusChangeResponse from(OpsInvoiceStatusChange c) {
    return new StatusChangeResponse(
        c.getId(),
        c.getField(),
        c.getFromValue(),
        c.getToValue(),
        c.getModule(),
        c.getReason(),
        c.getChangedAt(),
        c.getChangedBy());
  }
}
