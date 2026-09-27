package com.iortatechnxt.brokerverse.report.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.report.api.dto.ReportBatchRequest;
import com.iortatechnxt.brokerverse.report.api.dto.ReportBatchResponse;
import com.iortatechnxt.brokerverse.report.core.ReportBatchService;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
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
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param service batch service
   * @param downloads file download answers
   */
  public ReportBatchController(ReportBatchService service, FileDownloads downloads) {
    this.downloads = downloads;
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
   * The batch file (ZIP or merged PDF): a redirect to its presigned link, or the bytes of a batch
   * completed before ST1.
   *
   * @param id batch
   * @param request HTTP request (client address of the link audit)
   * @return redirect or file
   */
  @GetMapping("/{id}/file")
  public ResponseEntity<byte[]> file(@PathVariable Long id, HttpServletRequest request) {
    return downloads.respond(service.file(id), request);
  }
}
