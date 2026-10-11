package com.iortatechnxt.brokerverse.renewal.audit.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.renewal.audit.service.RenewalAuditLogService;
import com.iortatechnxt.brokerverse.renewal.audit.service.RenewalAuditLogService.Entry;
import com.iortatechnxt.brokerverse.renewal.audit.service.RenewalAuditLogService.Query;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit Logs of the Renewal module (BDOI Renewal FRS FRRN.043.01) and of one renewal account
 * (FRRN.043.02): the list with its filters and the CSV or Excel export. Read only.
 */
@RestController
@RequestMapping("/api/v1/renewal/audit-logs")
@PreAuthorize("hasAnyAuthority('RNW_AUDIT_VIEW','RNW_VIEW')")
public class RenewalAuditLogController {

  private final RenewalAuditLogService logs;

  /**
   * Creates the controller.
   *
   * @param logs audit logs
   */
  public RenewalAuditLogController(RenewalAuditLogService logs) {
    this.logs = logs;
  }

  /**
   * The entries of the filters (the menu needs the Audit Logs function; one renewal's entries need
   * only the view function).
   *
   * @param companyId company
   * @param filter filters
   * @return entries, latest first
   */
  @GetMapping
  public List<Entry> list(@RequestParam Long companyId, @ModelAttribute Filter filter) {
    return logs.entries(companyId, filter.query());
  }

  /**
   * The entries of the filters as CSV or Excel.
   *
   * @param companyId company
   * @param format CSV or XLSX
   * @param filter filters
   * @return file named Audit Logs_MMDDYYYY
   */
  @GetMapping("/export")
  @PreAuthorize("hasAnyAuthority('RNW_AUDIT_VIEW','RNW_EXPORT')")
  public ResponseEntity<byte[]> export(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "XLSX") String format,
      @ModelAttribute Filter filter) {
    MessageFile f = logs.export(companyId, filter.query(), "CSV".equals(format));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(f.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(f.fileName()))
        .body(f.content());
  }

  /**
   * Filters of the audit logs.
   *
   * @param from first date
   * @param to last date
   * @param actionType action type
   * @param ref reference number
   * @param user user name
   */
  public record Filter(
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      String actionType,
      String ref,
      String user) {

    Query query() {
      return new Query(from, to, actionType, ref, user);
    }
  }
}
