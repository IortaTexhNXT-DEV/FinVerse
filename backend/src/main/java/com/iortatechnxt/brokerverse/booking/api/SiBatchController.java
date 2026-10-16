package com.iortatechnxt.brokerverse.booking.api;

import com.iortatechnxt.brokerverse.booking.api.dto.ServiceInvoiceResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.SiBatchResponse;
import com.iortatechnxt.brokerverse.booking.service.SiBatchService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Daily service-invoice batches per insurer (G09): the batches of a period with their delivery
 * status, the service invoices of a batch, and sending a failed batch again.
 */
@RestController
@RequestMapping("/api/v1/booking/si-batches")
public class SiBatchController {

  private static final long MAX_DAYS = 366;

  private final SiBatchService batches;

  /**
   * Creates the controller.
   *
   * @param batches batches
   */
  public SiBatchController(SiBatchService batches) {
    this.batches = batches;
  }

  /**
   * Batches of a company over a period (at most a year).
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @return batches, newest first
   */
  @GetMapping
  @PreAuthorize(BookingAccess.VIEW)
  public List<SiBatchResponse> list(
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
      throw new BusinessRuleException(
          "DATE_RANGE", "The end date must be on or after the start date, within a year");
    }
    return batches.list(companyId, from, to).stream().map(SiBatchResponse::from).toList();
  }

  /**
   * The service invoices of a batch.
   *
   * @param id batch
   * @return service invoices
   */
  @GetMapping("/{id}/items")
  @PreAuthorize(BookingAccess.VIEW)
  public List<ServiceInvoiceResponse> items(@PathVariable Long id) {
    return batches.items(id).stream().map(ServiceInvoiceResponse::from).toList();
  }

  /**
   * Sends a failed batch again.
   *
   * @param id batch
   * @return batch
   */
  @PostMapping("/{id}/resend")
  @PreAuthorize(BookingAccess.PROCESS)
  public SiBatchResponse resend(@PathVariable Long id) {
    return SiBatchResponse.from(batches.resend(id));
  }
}
