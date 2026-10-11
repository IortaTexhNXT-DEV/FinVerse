package com.iortatechnxt.brokerverse.coa.api;

import com.iortatechnxt.brokerverse.bulk.api.dto.BulkJobResponse;
import com.iortatechnxt.brokerverse.bulk.api.dto.BulkRowResponse;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.coa.service.CoaUploadHandler;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.io.IOException;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Chart of accounts upload (FRBS 2.3.1) under the chart's own permission {@code COA_UPLOAD}: the GL
 * Team Lead uploads the chart without the broking bulk-processing role. The upload runs on the
 * shared bulk framework (handler {@link CoaUploadHandler#CODE}): template, validation of every row,
 * review, commit and result report.
 */
@RestController
@RequestMapping("/api/v1/coa/uploads")
@PreAuthorize("hasAuthority('COA_UPLOAD')")
public class CoaUploadController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  private static final int MAX_PAGE_SIZE = 200;

  private final BulkService bulk;

  /**
   * Creates the controller.
   *
   * @param bulk bulk framework
   */
  public CoaUploadController(BulkService bulk) {
    this.bulk = bulk;
  }

  /**
   * Template download.
   *
   * @return xlsx
   */
  @GetMapping("/template")
  public ResponseEntity<byte[]> template() {
    return xlsx("coa_accounts_template.xlsx", bulk.template(CoaUploadHandler.CODE));
  }

  /**
   * Uploads and validates a chart file.
   *
   * @param companyId company
   * @param file xlsx, ods or csv
   * @return validated upload
   * @throws IOException when the file cannot be read
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public BulkJobResponse upload(@RequestParam Long companyId, @RequestParam MultipartFile file)
      throws IOException {
    return BulkJobResponse.from(
        bulk.upload(
            new BulkUpload(
                companyId,
                CoaUploadHandler.CODE,
                file.getOriginalFilename(),
                file.getBytes(),
                Map.of())));
  }

  /**
   * Chart uploads of a company.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return uploads, newest first
   */
  @GetMapping
  public PageResponse<BulkJobResponse> uploads(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        bulk.jobs(
            companyId, CoaUploadHandler.CODE, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        BulkJobResponse::from);
  }

  /**
   * Rows of an upload with their validation or commit result.
   *
   * @param id upload
   * @param status status filter
   * @param page page
   * @param size size
   * @return rows
   */
  @GetMapping("/{id}/rows")
  public PageResponse<BulkRowResponse> rows(
      @PathVariable Long id,
      @RequestParam(required = false) BulkRowStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {
    requireChartJob(id);
    return PageResponse.of(
        bulk.rows(id, status, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        r -> BulkRowResponse.from(r, bulk.values(r)));
  }

  /**
   * Creates the accounts of the valid rows (pending authorization).
   *
   * @param id upload
   * @return completed upload
   */
  @PostMapping("/{id}/commit")
  public BulkJobResponse commit(@PathVariable Long id) {
    requireChartJob(id);
    return BulkJobResponse.from(bulk.commit(id));
  }

  /**
   * Discards an upload.
   *
   * @param id upload
   * @return cancelled upload
   */
  @PostMapping("/{id}/cancel")
  public BulkJobResponse cancel(@PathVariable Long id) {
    requireChartJob(id);
    return BulkJobResponse.from(bulk.cancel(id));
  }

  /**
   * Result report.
   *
   * @param id upload
   * @return xlsx
   */
  @GetMapping("/{id}/report")
  public ResponseEntity<byte[]> report(@PathVariable Long id) {
    BulkJob job = requireChartJob(id);
    return xlsx(job.getJobNo() + "_result.xlsx", bulk.report(id));
  }

  private BulkJob requireChartJob(Long id) {
    BulkJob job = bulk.job(id);
    if (!CoaUploadHandler.CODE.equals(job.getHandlerCode())) {
      throw new ResourceNotFoundException("Chart upload", id);
    }
    return job;
  }

  private static ResponseEntity<byte[]> xlsx(String fileName, byte[] content) {
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(fileName))
        .body(content);
  }
}
