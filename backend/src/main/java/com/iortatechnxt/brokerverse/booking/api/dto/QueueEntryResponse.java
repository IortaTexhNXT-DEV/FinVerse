package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.QueueEntry;
import com.iortatechnxt.brokerverse.booking.domain.QueueSource;
import com.iortatechnxt.brokerverse.booking.domain.QueueStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A booking queue entry.
 *
 * @param id id
 * @param arn account
 * @param status status
 * @param source who queued it
 * @param bookingDate booking date chosen
 * @param costCenter cost center chosen
 * @param queuedBy user
 * @param queuedAt time
 * @param lastError failure reason
 * @param invoiceNo invoice booked
 * @param batchRunNo batch run
 */
public record QueueEntryResponse(
    Long id,
    String arn,
    QueueStatus status,
    QueueSource source,
    LocalDate bookingDate,
    String costCenter,
    String queuedBy,
    Instant queuedAt,
    String lastError,
    String invoiceNo,
    String batchRunNo) {

  /**
   * Maps an entry.
   *
   * @param e entry
   * @return response
   */
  public static QueueEntryResponse from(QueueEntry e) {
    return new QueueEntryResponse(
        e.getId(),
        e.getArn(),
        e.getStatus(),
        e.getSource(),
        e.getBookingDate(),
        e.getCostCenter(),
        e.getQueuedBy(),
        e.getQueuedAt(),
        e.getLastError(),
        e.getInvoiceNo(),
        e.getBatchRunNo());
  }
}
