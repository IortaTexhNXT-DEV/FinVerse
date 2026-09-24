package com.iortatechnxt.brokerverse.bulk.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.bulk.api.dto.BulkHandlerResponse;
import com.iortatechnxt.brokerverse.bulk.api.dto.BulkJobResponse;
import com.iortatechnxt.brokerverse.bulk.api.dto.BulkRowResponse;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.bulk.service.BulkHandlerRegistry;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
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

/** Bulk uploads: types, templates, upload and validation, review, commit, reports. */
@RestController
@RequestMapping("/api/v1/bulk")
@PreAuthorize("hasAuthority('BULK_PROCESS')")
public class BulkController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  private static final int MAX_PAGE_SIZE = 500;
  private static final TypeReference<Map<String, String>> PARAMS = new TypeReference<>() {};

  private final BulkService bulk;
  private final BulkHandlerRegistry registry;
  private final ObjectMapper json;

  /**
   * Creates the controller.
   *
   * @param bulk bulk service
   * @param registry handlers
   * @param json JSON mapper (parameters)
   */
  public BulkController(BulkService bulk, BulkHandlerRegistry registry, ObjectMapper json) {
    this.bulk = bulk;
    this.registry = registry;
    this.json = json;
  }

  /**
   * Upload types the current user may use.
   *
   * @return handlers
   */
  @GetMapping("/handlers")
  public List<BulkHandlerResponse> handlers() {
    return registry.available().stream().map(BulkHandlerResponse::from).toList();
  }

  /**
   * One upload type.
   *
   * @param code handler
   * @return handler
   */
  @GetMapping("/handlers/{code}")
  public BulkHandlerResponse handler(@PathVariable String code) {
    return BulkHandlerResponse.from(registry.require(code));
  }

  /**
   * Template download.
   *
   * @param code handler
   * @return xlsx
   */
  @GetMapping("/handlers/{code}/template")
  public ResponseEntity<byte[]> template(@PathVariable String code) {
    return xlsx(code.toLowerCase(Locale.ROOT) + "_template.xlsx", bulk.template(code));
  }

  /**
   * Uploads and validates a file.
   *
   * @param companyId company
   * @param handler handler
   * @param parameters handler parameters as JSON object, optional
   * @param file xlsx, ods or csv
   * @return validated job
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public BulkJobResponse upload(
      @RequestParam Long companyId,
      @RequestParam String handler,
      @RequestParam(required = false) String parameters,
      @RequestParam MultipartFile file)
      throws IOException {
    return BulkJobResponse.from(
        bulk.upload(
            new BulkUpload(
                companyId,
                handler,
                file.getOriginalFilename(),
                file.getBytes(),
                params(parameters))));
  }

  private Map<String, String> params(String parameters) {
    if (parameters == null || parameters.isBlank()) {
      return Map.of();
    }
    try {
      return json.readValue(parameters, PARAMS);
    } catch (JsonProcessingException e) {
      throw new BusinessRuleException("BULK_PARAMETERS_INVALID", "Invalid upload parameters", e);
    }
  }

  /**
   * Uploads of a company.
   *
   * @param companyId company
   * @param handler handler filter
   * @param page page
   * @param size size
   * @return jobs
   */
  @GetMapping("/jobs")
  public PageResponse<BulkJobResponse> jobs(
      @RequestParam Long companyId,
      @RequestParam(required = false) String handler,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        bulk.jobs(companyId, handler, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        BulkJobResponse::from);
  }

  /**
   * One upload.
   *
   * @param id job
   * @return job
   */
  @GetMapping("/jobs/{id}")
  public BulkJobResponse job(@PathVariable Long id) {
    return BulkJobResponse.from(bulk.job(id));
  }

  /**
   * Rows of an upload.
   *
   * @param id job
   * @param status status filter
   * @param page page
   * @param size size
   * @return rows
   */
  @GetMapping("/jobs/{id}/rows")
  public PageResponse<BulkRowResponse> rows(
      @PathVariable Long id,
      @RequestParam(required = false) BulkRowStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {
    return PageResponse.of(
        bulk.rows(id, status, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        r -> BulkRowResponse.from(r, bulk.values(r)));
  }

  /**
   * Commits the valid rows.
   *
   * @param id job
   * @return completed job
   */
  @PostMapping("/jobs/{id}/commit")
  public BulkJobResponse commit(@PathVariable Long id) {
    return BulkJobResponse.from(bulk.commit(id));
  }

  /**
   * Discards an upload.
   *
   * @param id job
   * @return cancelled job
   */
  @PostMapping("/jobs/{id}/cancel")
  public BulkJobResponse cancel(@PathVariable Long id) {
    return BulkJobResponse.from(bulk.cancel(id));
  }

  /**
   * Result report.
   *
   * @param id job
   * @return xlsx
   */
  @GetMapping("/jobs/{id}/report")
  public ResponseEntity<byte[]> report(@PathVariable Long id) {
    return xlsx(bulk.job(id).getJobNo() + "_result.xlsx", bulk.report(id));
  }

  private static ResponseEntity<byte[]> xlsx(String fileName, byte[] content) {
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(fileName))
        .body(content);
  }
}
