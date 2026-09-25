package com.iortatechnxt.brokerverse.report.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.report.api.dto.ReportBatchRequest;
import com.iortatechnxt.brokerverse.report.api.dto.ReportBatchResponse;
import com.iortatechnxt.brokerverse.report.core.ReportBatchService;
import com.iortatechnxt.brokerverse.report.domain.ReportBatch;
import jakarta.validation.Valid;
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

/**
 * Report batches (FRBS 2.4.5 / 2.4.7): select several reports, run them with shared parameters and
 * download one ZIP or one merged PDF. Per-report permissions are enforced by the report service.
 */
@RestController
@RequestMapping("/api/v1/reports/batches")
@PreAuthorize("hasAuthority('REPORT_VIEW')")
public class ReportBatchController {

  private static final int MAX_PAGE_SIZE = 100;

  private final ReportBatchService service;

  /**
   * Creates the controller.
   *
   * @param service batch service
   */
  public ReportBatchController(ReportBatchService service) {
    this.service = service;
  }

  /**
   * Runs a batch.
   *
   * @param request reports, parameters and options
   * @return completed batch with the outcome of each report
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReportBatchResponse run(@Valid @RequestBody ReportBatchRequest request) {
    return ReportBatchResponse.from(service.run(request.toRequest()));
  }

  /**
   * The current user's batches.
   *
   * @param page page
   * @param size size
   * @return batches, newest first
   */
  @GetMapping
  public PageResponse<ReportBatchResponse> mine(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        service.mine(PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        ReportBatchResponse::summary);
  }

  /**
   * One batch.
   *
   * @param id batch
   * @return batch with items
   */
  @GetMapping("/{id}")
  public ReportBatchResponse get(@PathVariable Long id) {
    return ReportBatchResponse.from(service.get(id));
  }

  /**
   * The batch file (ZIP or merged PDF).
   *
   * @param id batch
   * @return file
   */
  @GetMapping("/{id}/file")
  public ResponseEntity<byte[]> file(@PathVariable Long id) {
    ReportBatch batch = service.get(id);
    byte[] content = batch.getContent();
    if (content == null) {
      throw new BusinessRuleException(
          "REPORT_BATCH_EMPTY", "Batch " + batch.getBatchNo() + " produced no file");
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(batch.getContentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(batch.getFileName()))
        .body(content);
  }
}
