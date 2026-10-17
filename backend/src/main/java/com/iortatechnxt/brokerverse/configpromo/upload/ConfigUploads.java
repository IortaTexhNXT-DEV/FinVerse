package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.bulk.service.BulkUploadApprovals;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * The uploads of the configuration screens, on top of the bulk upload framework: each upload type
 * is open to the holders of its maintenance permission (template, export, upload, submit) and of
 * its approval permission (approve or reject); nothing else of the framework is reachable here.
 */
@Component
public class ConfigUploads {

  private final List<ConfigUploadHandler> handlers;
  private final BulkService bulk;
  private final BulkUploadApprovals approvals;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param handlers the uploads of the configuration screens
   * @param bulk bulk uploads
   * @param approvals approval of the uploads
   * @param currentUser current user
   */
  public ConfigUploads(
      List<ConfigUploadHandler> handlers,
      BulkService bulk,
      BulkUploadApprovals approvals,
      CurrentUser currentUser) {
    this.handlers =
        handlers.stream().sorted(Comparator.comparing(ConfigUploadHandler::templateId)).toList();
    this.bulk = bulk;
    this.approvals = approvals;
    this.currentUser = currentUser;
  }

  /**
   * The upload types the user may upload or approve.
   *
   * @return handlers
   */
  public List<ConfigUploadHandler> available() {
    return handlers.stream().filter(h -> mayUpload(h) || mayApprove(h)).toList();
  }

  /**
   * Whether the user may upload files of a type.
   *
   * @param handler upload type
   * @return true when allowed
   */
  public boolean mayUpload(ConfigUploadHandler handler) {
    return currentUser.hasAuthority(handler.permission());
  }

  /**
   * Whether the user may approve uploads of a type.
   *
   * @param handler upload type
   * @return true when allowed
   */
  public boolean mayApprove(ConfigUploadHandler handler) {
    return currentUser.hasAuthority(handler.approvePermission());
  }

  /**
   * An upload type the user may see.
   *
   * @param code handler code
   * @return handler
   */
  public ConfigUploadHandler handler(String code) {
    ConfigUploadHandler handler =
        find(code).orElseThrow(() -> new ResourceNotFoundException("Upload type", code));
    if (!mayUpload(handler) && !mayApprove(handler)) {
      throw new AccessDeniedException("No access to the uploads of " + handler.title());
    }
    return handler;
  }

  private Optional<ConfigUploadHandler> find(String code) {
    return handlers.stream().filter(h -> h.code().equals(code)).findFirst();
  }

  private ConfigUploadHandler uploader(String code) {
    ConfigUploadHandler handler = handler(code);
    if (!mayUpload(handler)) {
      throw new AccessDeniedException(
          "Uploads of " + handler.title() + " need " + handler.permission());
    }
    return handler;
  }

  /**
   * The template of an upload type, in the layout of its tab of the workbook.
   *
   * @param code handler
   * @param companyId company (examples in its base currency)
   * @return xlsx
   */
  public byte[] template(String code, Long companyId) {
    return bulk.template(uploader(code).code(), companyId);
  }

  /**
   * The current data of an upload type, in the layout of the template.
   *
   * @param code handler
   * @param companyId company
   * @return xlsx
   */
  public byte[] export(String code, Long companyId) {
    return bulk.export(uploader(code).code(), companyId);
  }

  /**
   * Uploads and validates a file; nothing changes until the upload is approved.
   *
   * @param companyId company
   * @param code handler
   * @param fileName file name
   * @param content file
   * @return the validated upload
   */
  public BulkJob upload(Long companyId, String code, String fileName, byte[] content) {
    uploader(code);
    return bulk.upload(new BulkUpload(companyId, code, fileName, content, Map.of()));
  }

  /**
   * The uploads of a type.
   *
   * @param companyId company
   * @param code handler
   * @param pageable page
   * @return uploads
   */
  public Page<BulkJob> jobs(Long companyId, String code, Pageable pageable) {
    return bulk.jobs(companyId, handler(code).code(), pageable);
  }

  /**
   * An upload of a configuration screen.
   *
   * @param id job
   * @return job
   */
  public BulkJob job(Long id) {
    BulkJob job = bulk.job(id);
    handler(job.getHandlerCode());
    return job;
  }

  /**
   * The upload type of an upload.
   *
   * @param job job
   * @return handler
   */
  public ConfigUploadHandler handlerOf(BulkJob job) {
    return handler(job.getHandlerCode());
  }

  /**
   * Rows of an upload.
   *
   * @param id job
   * @param status filter
   * @param pageable page
   * @return rows
   */
  public Page<BulkRowRecord> rows(Long id, BulkRowStatus status, Pageable pageable) {
    job(id);
    return bulk.rows(id, status, pageable);
  }

  /**
   * The values of a row.
   *
   * @param row row
   * @return values by header
   */
  public Map<String, String> values(BulkRowRecord row) {
    return bulk.values(row);
  }

  /**
   * Submits a validated upload for approval.
   *
   * @param id job
   * @return job
   */
  public BulkJob submit(Long id) {
    uploader(job(id).getHandlerCode());
    return approvals.submit(id);
  }

  /**
   * Approves an upload: its valid rows are applied.
   *
   * @param id job
   * @param note remarks
   * @return job
   */
  public BulkJob approve(Long id, String note) {
    job(id);
    return approvals.approve(id, note);
  }

  /**
   * Rejects an upload.
   *
   * @param id job
   * @param note reason
   * @return job
   */
  public BulkJob reject(Long id, String note) {
    job(id);
    if (note == null || note.isBlank()) {
      throw new BusinessRuleException("BULK_REJECT_REASON", "Enter why the upload is rejected");
    }
    return approvals.reject(id, note);
  }

  /**
   * Discards a validated upload.
   *
   * @param id job
   * @return job
   */
  public BulkJob cancel(Long id) {
    uploader(job(id).getHandlerCode());
    return bulk.cancel(id);
  }

  /**
   * The result report.
   *
   * @param id job
   * @return xlsx
   */
  public byte[] report(Long id) {
    job(id);
    return bulk.report(id);
  }

  /**
   * The rows not applied with their errors, in the template layout.
   *
   * @param id job
   * @return xlsx
   */
  public byte[] errorFile(Long id) {
    job(id);
    return bulk.errorFile(id);
  }

  /**
   * Uploads of the configuration screens waiting for the user's approval.
   *
   * @return jobs
   */
  public List<BulkJob> waiting() {
    return approvals.waiting().stream().filter(j -> find(j.getHandlerCode()).isPresent()).toList();
  }
}
