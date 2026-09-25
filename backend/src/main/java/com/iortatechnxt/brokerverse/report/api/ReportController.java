package com.iortatechnxt.brokerverse.report.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.report.api.dto.ReportCatalogueEntry;
import com.iortatechnxt.brokerverse.report.api.dto.ReportRunResponse;
import com.iortatechnxt.brokerverse.report.core.ExportOptions;
import com.iortatechnxt.brokerverse.report.core.ReportAccess;
import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.report.render.PrintOptions;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generic report API: catalogue, on-screen run and export (PDF / XLSX / CSV). Per-report
 * permissions are enforced by {@link ReportService}.
 */
@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAuthority('REPORT_VIEW')")
public class ReportController {

  private static final int MAX_PAGE_SIZE = 100;

  private final ReportService service;
  private final ReportArchiveService archive;

  /**
   * Creates the controller.
   *
   * @param service report service
   * @param archive archive of generated reports
   */
  public ReportController(ReportService service, ReportArchiveService archive) {
    this.service = service;
    this.archive = archive;
  }

  /**
   * Archived runs and exports of the reports the user may view (CSHID.018).
   *
   * @param code one report, optional
   * @param page page
   * @param size size
   * @return runs, newest first
   */
  @GetMapping("/runs")
  public PageResponse<ReportRunResponse> runs(
      @RequestParam(required = false) String code,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        archive.runs(code, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        ReportRunResponse::from);
  }

  /**
   * Downloads an archived export again; needs the report's export permission (CSHID.018).
   *
   * @param id archived export
   * @return file
   */
  @GetMapping("/runs/{id}/file")
  public ResponseEntity<byte[]> runFile(@PathVariable Long id) {
    RunFile file = archive.file(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.fileName()))
        .body(file.content());
  }

  /**
   * Lists reports available to the user.
   *
   * @return catalogue
   */
  @GetMapping
  public List<ReportCatalogueEntry> catalogue() {
    return service.catalogue().stream()
        .map(m -> ReportCatalogueEntry.from(m, ReportAccess.mayExport(m)))
        .toList();
  }

  /**
   * Runs a report for on-screen display.
   *
   * @param code report code
   * @param params parameter values
   * @return result
   */
  @PostMapping("/{code}/run")
  public ReportResult run(@PathVariable String code, @RequestBody Map<String, String> params) {
    return service.run(code, params);
  }

  /**
   * Exports a report as a file, with the PDF print options (FRBS 2.4.9) and the column filters of
   * the viewer (FRBS 2.4.4).
   *
   * @param code report code
   * @param format format
   * @param paper paper size (A4, LETTER, LEGAL, A3), PDF only
   * @param orientation AUTO, PORTRAIT or LANDSCAPE, PDF only
   * @param fitToWidth stretch the table to the page width, PDF only
   * @param filter column filters as {@code column:text}
   * @param params parameter values
   * @return file
   */
  @PostMapping("/{code}/export")
  @SuppressWarnings("java:S107") // request parameters of one endpoint
  public ResponseEntity<byte[]> export(
      @PathVariable String code,
      @RequestParam ExportFormat format,
      @RequestParam(required = false) String paper,
      @RequestParam(required = false) String orientation,
      @RequestParam(required = false) Boolean fitToWidth,
      @RequestParam(required = false) List<String> filter,
      @RequestBody Map<String, String> params) {
    var file =
        service.export(
            code,
            params,
            format,
            new ExportOptions(
                PrintOptions.of(paper, orientation, fitToWidth),
                ExportOptions.parseFilters(filter)));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.fileName()))
        .body(file.content());
  }
}
