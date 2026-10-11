package com.iortatechnxt.brokerverse.configpromo.api;

import com.iortatechnxt.brokerverse.bulk.api.dto.BulkJobResponse;
import com.iortatechnxt.brokerverse.bulk.api.dto.BulkRowResponse;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.ConfigUploadJobResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.ConfigUploadTypeResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.DecisionRequest;
import com.iortatechnxt.brokerverse.configpromo.upload.ConfigUploadHandler;
import com.iortatechnxt.brokerverse.configpromo.upload.ConfigUploads;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
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
 * Uploads of the configuration screens: the template in the layout of the tab of the master data
 * and configuration workbook, the current data in the same layout, the upload with its validation
 * and preview, and the approval by a second user before the rows are applied. Each upload type
 * checks its own maintenance and approval permissions.
 */
@RestController
@RequestMapping("/api/v1/config-uploads")
@PreAuthorize("isAuthenticated()")
public class ConfigUploadController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  private static final int MAX_PAGE_SIZE = 500;

  private final ConfigUploads uploads;

  /**
   * Creates the controller.
   *
   * @param uploads uploads of the configuration screens
   */
  public ConfigUploadController(ConfigUploads uploads) {
    this.uploads = uploads;
  }

  /**
   * The upload types the user may upload or approve.
   *
   * @return types
   */
  @GetMapping("/types")
  public List<ConfigUploadTypeResponse> types() {
    return uploads.available().stream().map(this::type).toList();
  }

  /**
   * One upload type.
   *
   * @param code type
   * @return type
   */
  @GetMapping("/types/{code}")
  public ConfigUploadTypeResponse type(@PathVariable String code) {
    return type(uploads.handler(code));
  }

  private ConfigUploadTypeResponse type(ConfigUploadHandler h) {
    return ConfigUploadTypeResponse.from(h, uploads.mayUpload(h), uploads.mayApprove(h));
  }

  /**
   * The template of an upload type.
   *
   * @param code type
   * @param companyId company
   * @return xlsx
   */
  @GetMapping("/types/{code}/template")
  public ResponseEntity<byte[]> template(
      @PathVariable String code, @RequestParam(required = false) Long companyId) {
    ConfigUploadHandler h = uploads.handler(code);
    return xlsx(h.templateId() + " " + h.title() + ".xlsx", uploads.template(code, companyId));
  }

  /**
   * The current data of an upload type in the layout of its template.
   *
   * @param code type
   * @param companyId company
   * @return xlsx
   */
  @GetMapping("/types/{code}/export")
  public ResponseEntity<byte[]> export(@PathVariable String code, @RequestParam Long companyId) {
    ConfigUploadHandler h = uploads.handler(code);
    return xlsx(
        h.templateId() + " " + h.title() + " (current).xlsx", uploads.export(code, companyId));
  }

  /**
   * Uploads and validates a file.
   *
   * @param companyId company
   * @param type upload type
   * @param file xlsx, ods or csv in the layout of the template
   * @return the validated upload
   * @throws IOException when the file cannot be read
   */
  @PostMapping(value = "/jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public ConfigUploadJobResponse upload(
      @RequestParam Long companyId, @RequestParam String type, @RequestParam MultipartFile file)
      throws IOException {
    return job(uploads.upload(companyId, type, file.getOriginalFilename(), file.getBytes()));
  }

  /**
   * The uploads of a type.
   *
   * @param companyId company
   * @param type upload type
   * @param page page
   * @param size size
   * @return uploads
   */
  @GetMapping("/jobs")
  public PageResponse<BulkJobResponse> jobs(
      @RequestParam Long companyId,
      @RequestParam String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return PageResponse.of(
        uploads.jobs(companyId, type, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        BulkJobResponse::from);
  }

  /**
   * The uploads waiting for the approval of the user.
   *
   * @return uploads
   */
  @GetMapping("/waiting")
  public List<ConfigUploadJobResponse> waiting() {
    return uploads.waiting().stream().map(this::job).toList();
  }

  /**
   * One upload.
   *
   * @param id upload
   * @return upload
   */
  @GetMapping("/jobs/{id}")
  public ConfigUploadJobResponse job(@PathVariable Long id) {
    return job(uploads.job(id));
  }

  private ConfigUploadJobResponse job(BulkJob job) {
    return new ConfigUploadJobResponse(BulkJobResponse.from(job), type(uploads.handlerOf(job)));
  }

  /**
   * The rows of an upload with their preview (add or update) and messages.
   *
   * @param id upload
   * @param status filter
   * @param page page
   * @param size size
   * @return rows
   */
  @GetMapping("/jobs/{id}/rows")
  public PageResponse<BulkRowResponse> rows(
      @PathVariable Long id,
      @RequestParam(required = false) BulkRowStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        uploads.rows(id, status, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        r -> BulkRowResponse.from(r, uploads.values(r)));
  }

  /**
   * Submits the upload for approval.
   *
   * @param id upload
   * @return upload
   */
  @PostMapping("/jobs/{id}/submit")
  public ConfigUploadJobResponse submit(@PathVariable Long id) {
    return job(uploads.submit(id));
  }

  /**
   * Approves the upload; its valid rows are applied.
   *
   * @param id upload
   * @param request remarks
   * @return upload
   */
  @PostMapping("/jobs/{id}/approve")
  public ConfigUploadJobResponse approve(
      @PathVariable Long id, @RequestBody(required = false) DecisionRequest request) {
    return job(uploads.approve(id, request == null ? null : request.note()));
  }

  /**
   * Rejects the upload.
   *
   * @param id upload
   * @param request reason
   * @return upload
   */
  @PostMapping("/jobs/{id}/reject")
  public ConfigUploadJobResponse reject(
      @PathVariable Long id, @RequestBody DecisionRequest request) {
    return job(uploads.reject(id, request.note()));
  }

  /**
   * Discards a validated upload.
   *
   * @param id upload
   * @return upload
   */
  @PostMapping("/jobs/{id}/cancel")
  public ConfigUploadJobResponse cancel(@PathVariable Long id) {
    return job(uploads.cancel(id));
  }

  /**
   * The result report.
   *
   * @param id upload
   * @return xlsx
   */
  @GetMapping("/jobs/{id}/report")
  public ResponseEntity<byte[]> report(@PathVariable Long id) {
    return xlsx(uploads.job(id).getJobNo() + " result.xlsx", uploads.report(id));
  }

  /**
   * The rows not applied with their errors, in the template layout.
   *
   * @param id upload
   * @return xlsx
   */
  @GetMapping("/jobs/{id}/error-file")
  public ResponseEntity<byte[]> errorFile(@PathVariable Long id) {
    return xlsx(uploads.job(id).getJobNo() + " errors.xlsx", uploads.errorFile(id));
  }

  private static ResponseEntity<byte[]> xlsx(String fileName, byte[] content) {
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDispositions.attachment(fileName.toLowerCase(Locale.ROOT).replace(' ', '_')))
        .body(content);
  }
}
