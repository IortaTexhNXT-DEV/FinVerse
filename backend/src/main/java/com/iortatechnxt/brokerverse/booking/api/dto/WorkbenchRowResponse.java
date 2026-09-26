package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.BookingWorkbenchService.Row;
import java.time.LocalDate;

/**
 * One row of the Booking Workbench.
 *
 * @param id queue entry (queued / failed), account (ready) or invoice (booked)
 * @param accountId account
 * @param invoiceId invoice (booked)
 * @param clientName client name
 * @param clientCode client code
 * @param arn proposal number (ARN)
 * @param invoiceNo invoice number
 * @param status status
 * @param lineCode product line
 * @param productCode product
 * @param department sales department
 * @param bookingDate booking date
 * @param message failure reason, or the invoice kind on the booked tab
 * @param source queue or booking source
 * @param costCenter cost center
 */
public record WorkbenchRowResponse(
    Long id,
    Long accountId,
    Long invoiceId,
    String clientName,
    String clientCode,
    String arn,
    String invoiceNo,
    String status,
    String lineCode,
    String productCode,
    String department,
    LocalDate bookingDate,
    String message,
    String source,
    String costCenter) {

  /**
   * Maps a workbench row.
   *
   * @param r row
   * @return response
   */
  public static WorkbenchRowResponse from(Row r) {
    return new WorkbenchRowResponse(
        r.id(),
        r.accountId(),
        r.invoiceId(),
        r.clientName(),
        r.clientCode(),
        r.arn(),
        r.invoiceNo(),
        r.status(),
        r.lineCode(),
        r.productCode(),
        r.department(),
        r.bookingDate(),
        r.message(),
        r.source(),
        r.costCenter());
  }
}
