package com.iortatechnxt.brokerverse.migration.archive.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.archive.api.dto.InquiryDtos.AccessLogResponse;
import com.iortatechnxt.brokerverse.migration.archive.api.dto.InquiryDtos.RecordDetail;
import com.iortatechnxt.brokerverse.migration.archive.api.dto.InquiryDtos.RecordSummary;
import com.iortatechnxt.brokerverse.migration.archive.domain.AccessLog;
import com.iortatechnxt.brokerverse.migration.archive.domain.ArchiveRecord;
import com.iortatechnxt.brokerverse.migration.archive.service.AccessLogFilter;
import com.iortatechnxt.brokerverse.migration.archive.service.ArchiveCriteria;
import com.iortatechnxt.brokerverse.migration.archive.service.LegacyInquiryService;
import com.iortatechnxt.brokerverse.migration.common.service.Workbooks;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Legacy Inquiry (DATA_MIGRATION_DESIGN section 16; screen Legacy Inquiry): search, read-only
 * detail, documents and Excel export of the legacy archive, every access logged with its reason;
 * and the access log for Compliance.
 */
@RestController
@RequestMapping("/api/v1/legacy-inquiry")
public class LegacyInquiryController {

  private static final String VIEW = "hasAuthority('LEGACY_INQUIRY_VIEW')";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final int MAX_PAGE = 200;
  private static final int DEFAULT_PAGE = 25;

  private final LegacyInquiryService inquiry;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param inquiry inquiry
   * @param downloads file responses
   */
  public LegacyInquiryController(LegacyInquiryService inquiry, FileDownloads downloads) {
    this.inquiry = inquiry;
    this.downloads = downloads;
  }

  /**
   * The settings of the screen: reason asked, export limit, read-only legacy applications.
   *
   * @return settings
   */
  @GetMapping("/settings")
  @PreAuthorize(VIEW)
  public LegacyInquiryService.Settings settings() {
    return inquiry.settings();
  }

  /**
   * Searches the archive.
   *
   * @param companyId company
   * @param params criteria (client, policyNo, invoiceNo, receiptNo, claimNo, recordType,
   *     sourceSystem, from, to), reason (reasonCode, reasonText) and page (page, size)
   * @return records
   */
  @GetMapping("/records")
  @PreAuthorize(VIEW)
  public PageResponse<RecordSummary> search(
      @RequestParam Long companyId, @RequestParam Map<String, String> params) {
    int page = number(params.get("page"), 0);
    int size = Math.min(number(params.get("size"), DEFAULT_PAGE), MAX_PAGE);
    return PageResponse.of(
        inquiry.search(companyId, criteria(params), reason(params), PageRequest.of(page, size)),
        RecordSummary::from);
  }

  /**
   * An archive record with its legacy columns and documents.
   *
   * @param id record
   * @param reasonCode reason
   * @param reasonText reason details
   * @return detail
   */
  @GetMapping("/records/{id}")
  @PreAuthorize(VIEW)
  public RecordDetail view(
      @PathVariable Long id,
      @RequestParam(required = false) String reasonCode,
      @RequestParam(required = false) String reasonText) {
    return RecordDetail.from(inquiry.view(id, new AccessLog.Reason(reasonCode, reasonText)));
  }

  /**
   * Downloads a legacy document.
   *
   * @param id record
   * @param documentId document
   * @param reasonCode reason
   * @param reasonText reason details
   * @param request request
   * @return the file
   */
  @GetMapping("/records/{id}/documents/{documentId}")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> download(
      @PathVariable Long id,
      @PathVariable Long documentId,
      @RequestParam(required = false) String reasonCode,
      @RequestParam(required = false) String reasonText,
      HttpServletRequest request) {
    return downloads.respond(
        inquiry.download(id, documentId, new AccessLog.Reason(reasonCode, reasonText)), request);
  }

  /**
   * Exports the records of a search to Excel.
   *
   * @param companyId company
   * @param params criteria and reason, as for the search
   * @param request request
   * @return the workbook
   */
  @GetMapping("/export")
  @PreAuthorize("hasAuthority('LEGACY_INQUIRY_EXPORT')")
  public ResponseEntity<byte[]> export(
      @RequestParam Long companyId,
      @RequestParam Map<String, String> params,
      HttpServletRequest request) {
    List<ArchiveRecord> found = inquiry.export(companyId, criteria(params), reason(params));
    List<List<String>> rows = new ArrayList<>();
    for (ArchiveRecord r : found) {
      rows.add(
          List.of(
              r.getSourceSystem(),
              r.getRecordType(),
              r.getLegacyKey(),
              Objects.toString(r.getClientKey(), ""),
              Objects.toString(r.getClientName(), ""),
              Objects.toString(r.getPolicyNo(), ""),
              Objects.toString(r.getInvoiceNo(), ""),
              Objects.toString(r.getReceiptNo(), ""),
              Objects.toString(r.getClaimNo(), ""),
              r.getDocumentDate().toString(),
              Objects.toString(r.getCurrency(), ""),
              r.getAmount() == null ? "" : r.getAmount().toPlainString(),
              Objects.toString(r.getStatus(), ""),
              r.getSummary().entrySet().stream()
                  .map(e -> e.getKey() + ": " + e.getValue())
                  .collect(Collectors.joining("; "))));
    }
    try (Workbooks book = Workbooks.create()) {
      book.sheet(
          "Legacy records",
          List.of(
              "System",
              "Record type",
              "Legacy key",
              "Client no.",
              "Client",
              "Policy / cover",
              "Invoice",
              "Receipt",
              "Claim",
              "Date",
              "Currency",
              "Amount",
              "Status",
              "Legacy details"),
          rows);
      return downloads.respond(
          FileDownload.inline("legacy_records.xlsx", XLSX, book.bytes()), request);
    }
  }

  /**
   * The access log of the legacy archive.
   *
   * @param companyId company
   * @param username user
   * @param action action
   * @param from from
   * @param to to
   * @param page page
   * @param size size
   * @return entries
   */
  @GetMapping("/access-log")
  @PreAuthorize("hasAuthority('LEGACY_ACCESS_LOG_VIEW')")
  @SuppressWarnings("java:S107") // request parameters
  public PageResponse<AccessLogResponse> accessLog(
      @RequestParam Long companyId,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        inquiry.accessLog(
            companyId,
            new AccessLogFilter(username, action, from, to),
            PageRequest.of(page, Math.min(size, MAX_PAGE))),
        AccessLogResponse::from);
  }

  private static ArchiveCriteria criteria(Map<String, String> p) {
    return new ArchiveCriteria(
        p.get("client"),
        p.get("policyNo"),
        p.get("invoiceNo"),
        p.get("receiptNo"),
        p.get("claimNo"),
        p.get("recordType"),
        p.get("sourceSystem"),
        date(p.get("from")),
        date(p.get("to")));
  }

  private static AccessLog.Reason reason(Map<String, String> p) {
    return new AccessLog.Reason(p.get("reasonCode"), p.get("reasonText"));
  }

  private static LocalDate date(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.strip());
    } catch (DateTimeParseException e) {
      throw new BusinessRuleException(
          "MIG_DATE_INVALID", "Give the date as yyyy-MM-dd: " + value, e);
    }
  }

  private static int number(String value, int fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Math.max(0, Integer.parseInt(value.strip()));
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
