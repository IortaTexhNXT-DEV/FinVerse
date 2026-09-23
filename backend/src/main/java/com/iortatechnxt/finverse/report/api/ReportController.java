package com.iortatechnxt.finverse.report.api;

import com.iortatechnxt.finverse.common.api.ContentDispositions;
import com.iortatechnxt.finverse.report.api.dto.ReportCatalogueEntry;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportService;
import com.iortatechnxt.finverse.report.render.ExportFormat;
import java.util.List;
import java.util.Map;
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

  private final ReportService service;

  /**
   * Creates the controller.
   *
   * @param service report service
   */
  public ReportController(ReportService service) {
    this.service = service;
  }

  /**
   * Lists reports available to the user.
   *
   * @return catalogue
   */
  @GetMapping
  public List<ReportCatalogueEntry> catalogue() {
    return service.catalogue().stream().map(ReportCatalogueEntry::from).toList();
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
   * Exports a report as a file.
   *
   * @param code report code
   * @param format format
   * @param params parameter values
   * @return file
   */
  @PostMapping("/{code}/export")
  public ResponseEntity<byte[]> export(
      @PathVariable String code,
      @RequestParam ExportFormat format,
      @RequestBody Map<String, String> params) {
    var file = service.export(code, params, format);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.fileName()))
        .body(file.content());
  }
}
