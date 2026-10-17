package com.iortatechnxt.brokerverse.booking.api;

import com.iortatechnxt.brokerverse.booking.api.dto.ServiceInvoiceResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.SiBatchResponse;
import com.iortatechnxt.brokerverse.booking.domain.SiBatch;
import com.iortatechnxt.brokerverse.booking.service.SiBatchFiles;
import com.iortatechnxt.brokerverse.booking.service.SiBatchService;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.OwnedRecord;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@OwnedRecord(SiBatch.class)
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
   * Runs a supplementary batch for a date: the service invoices issued after the day's batch of
   * their insurer, still in no batch, form one more batch per insurer.
   *
   * @param companyId company
   * @param date business date of the batch
   * @return batches generated
   */
  @PostMapping("/supplementary")
  @PreAuthorize(BookingAccess.PROCESS)
  public List<SiBatchResponse> supplementary(
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return batches.supplementary(companyId, date).stream().map(SiBatchResponse::from).toList();
  }

  /**
   * The service invoices of a batch in one PDF.
   *
   * @param id batch
   * @return PDF
   */
  @GetMapping("/{id}/pdf")
  @PreAuthorize(BookingAccess.VIEW)
  public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
    SiBatchFiles.Files files = batches.files(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(files.pdfName()))
        .body(files.pdf());
  }

  /**
   * The manifest of a batch (CSV).
   *
   * @param id batch
   * @return manifest
   */
  @GetMapping("/{id}/manifest")
  @PreAuthorize(BookingAccess.VIEW)
  public ResponseEntity<byte[]> manifest(@PathVariable Long id) {
    SiBatchFiles.Files files = batches.files(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(SiBatchFiles.CSV))
        .header(
            HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(files.manifestName()))
        .body(files.manifest());
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
