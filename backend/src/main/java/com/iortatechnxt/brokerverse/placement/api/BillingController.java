package com.iortatechnxt.brokerverse.placement.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.ArnRequest;
import com.iortatechnxt.brokerverse.placement.api.dto.BillingBatchRequest;
import com.iortatechnxt.brokerverse.placement.api.dto.BillingBatchResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.BillingCandidateResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.PaymentReportResponse;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportKind;
import com.iortatechnxt.brokerverse.placement.service.BillingService;
import com.iortatechnxt.brokerverse.placement.service.BillingService.BillingFile;
import com.iortatechnxt.brokerverse.placement.service.PaymentReportConfirmation;
import com.iortatechnxt.brokerverse.placement.service.PaymentReportService;
import com.iortatechnxt.brokerverse.placement.service.PaymentReportService.ReportUpload;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * CLPC billing and payment report matching (BRNB.067/068): candidates, billing batches and their
 * file (.xlsx / .ods), payment report upload, match review, manual match, confirmation and discard.
 * Transport to and from CLPC is parked (Q28).
 */
@RestController
@RequestMapping("/api/v1/placement/billing")
@PreAuthorize("hasAuthority('BILLING_MANAGE')")
public class BillingController {

  private static final int MAX_PAGE = 100;

  private final BillingService billing;
  private final PaymentReportService reports;
  private final PaymentReportConfirmation confirmation;

  /**
   * Creates the controller.
   *
   * @param billing billing batches
   * @param reports payment reports
   * @param confirmation report confirmation
   */
  public BillingController(
      BillingService billing,
      PaymentReportService reports,
      PaymentReportConfirmation confirmation) {
    this.billing = billing;
    this.reports = reports;
    this.confirmation = confirmation;
  }

  /**
   * CBG Fire accounts awaiting payment not yet billed.
   *
   * @param companyId company
   * @return candidates
   */
  @GetMapping("/candidates")
  public List<BillingCandidateResponse> candidates(@RequestParam Long companyId) {
    return billing.candidates(companyId).stream().map(BillingCandidateResponse::from).toList();
  }

  /**
   * Billing batches, newest first.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return batches
   */
  @GetMapping("/batches")
  public PageResponse<BillingBatchResponse> batches(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        billing.batches(companyId, pageable(page, size)), BillingBatchResponse::from);
  }

  /**
   * Creates a billing batch.
   *
   * @param request company and accounts (empty = every candidate)
   * @return batch with its items
   */
  @PostMapping("/batches")
  @ResponseStatus(HttpStatus.CREATED)
  public BillingBatchResponse create(@Valid @RequestBody BillingBatchRequest request) {
    return BillingBatchResponse.detail(billing.create(request.companyId(), request.arns()));
  }

  /**
   * One batch with its items.
   *
   * @param id batch
   * @return batch
   */
  @GetMapping("/batches/{id}")
  public BillingBatchResponse batch(@PathVariable Long id) {
    return BillingBatchResponse.detail(billing.get(id));
  }

  /**
   * The billing file of a batch.
   *
   * @param id batch
   * @param format xlsx or ods
   * @return file
   */
  @GetMapping("/batches/{id}/file")
  public ResponseEntity<byte[]> file(
      @PathVariable Long id, @RequestParam(defaultValue = "XLSX") String format) {
    BillingFile file = billing.file(id, format);
    MediaType type =
        "ODS".equals(file.format())
            ? MediaType.parseMediaType("application/vnd.oasis.opendocument.spreadsheet")
            : MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    return ResponseEntity.ok()
        .contentType(type)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.fileName()))
        .body(file.content());
  }

  /**
   * Uploads a payment report and matches it.
   *
   * @param companyId company
   * @param kind CLPC or REFERENCE
   * @param batchId billing batch answered (CLPC)
   * @param file xlsx, csv or ods
   * @return report under review with its lines
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentReportResponse upload(
      @RequestParam Long companyId,
      @RequestParam PaymentReportKind kind,
      @RequestParam(required = false) Long batchId,
      @RequestParam MultipartFile file)
      throws IOException {
    return PaymentReportResponse.detail(
        reports.upload(
            new ReportUpload(
                companyId, kind, batchId, file.getOriginalFilename(), file.getBytes())));
  }

  /**
   * Payment reports, newest first.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return reports
   */
  @GetMapping("/reports")
  public PageResponse<PaymentReportResponse> reports(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        reports.reports(companyId, pageable(page, size)), PaymentReportResponse::from);
  }

  /**
   * One report with its lines (match review).
   *
   * @param id report
   * @return report
   */
  @GetMapping("/reports/{id}")
  public PaymentReportResponse report(@PathVariable Long id) {
    return PaymentReportResponse.detail(reports.get(id));
  }

  /**
   * Matches an ambiguous or unmatched line to an account.
   *
   * @param id report
   * @param lineId line
   * @param request account
   * @return report
   */
  @PostMapping("/reports/{id}/lines/{lineId}/match")
  public PaymentReportResponse match(
      @PathVariable Long id, @PathVariable Long lineId, @Valid @RequestBody ArnRequest request) {
    return PaymentReportResponse.detail(reports.resolveLine(id, lineId, request.arn()));
  }

  /**
   * Confirms a report: the payment gate opens for the matched, paid accounts.
   *
   * @param id report
   * @return report with the outcome per line
   */
  @PostMapping("/reports/{id}/confirm")
  public PaymentReportResponse confirm(@PathVariable Long id) {
    return PaymentReportResponse.detail(confirmation.confirm(id));
  }

  /**
   * Discards a report under review.
   *
   * @param id report
   * @return report
   */
  @PostMapping("/reports/{id}/discard")
  public PaymentReportResponse discard(@PathVariable Long id) {
    return PaymentReportResponse.detail(reports.discard(id));
  }

  private static PageRequest pageable(int page, int size) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE));
  }
}
