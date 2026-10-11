package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.productmaint.domain.PlacementReport;
import com.iortatechnxt.brokerverse.productmaint.service.PlacementUpdateService;
import com.iortatechnxt.brokerverse.productmaint.service.PlacementUpdateService.Settings;
import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
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
 * The repository of the Consolidated Placement Update Report (BDOI FRS FRPM.007.01): the reports
 * generated weekly or on request, their preview, the Excel file and the schedule in force.
 */
@RestController
@RequestMapping("/api/v1/product-maintenance/placement-reports")
@PreAuthorize("hasAuthority('PKG_REPORT_VIEW')")
public class PlacementUpdateController {

  private static final int MAX_PAGE = 100;

  private final PlacementUpdateService reports;
  private final ReportArchiveService archive;
  private final UserDirectory users;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param reports placement update reports
   * @param archive archived files
   * @param users user names
   * @param clock clock (report date of a run on request)
   */
  public PlacementUpdateController(
      PlacementUpdateService reports,
      ReportArchiveService archive,
      UserDirectory users,
      Clock clock) {
    this.reports = reports;
    this.archive = archive;
    this.users = users;
    this.clock = clock;
  }

  /**
   * The reports of a company, newest first.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return reports
   */
  @GetMapping
  public PageResponse<ReportView> list(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        reports.list(
            companyId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE))),
        this::view);
  }

  /**
   * The schedule and scope in force.
   *
   * @return settings
   */
  @GetMapping("/settings")
  public Settings settings() {
    return reports.settings();
  }

  /**
   * Generates the report of today on request.
   *
   * @param body company
   * @return the report
   */
  @PostMapping("/generate")
  public ReportView generate(@RequestBody GenerateBody body) {
    return view(
        reports.generate(
            body.companyId(), BusinessClock.today(clock), PlacementUpdateService.ON_REQUEST));
  }

  /**
   * The preview of a report: the rows of its Excel file.
   *
   * @param id report
   * @return columns and rows
   */
  @GetMapping("/{id}/preview")
  public Preview preview(@PathVariable Long id) {
    PlacementReport r = reports.get(id);
    return new Preview(
        view(r),
        PlacementUpdateService.columns().stream()
            .map(c -> new ColumnView(c.key(), c.label()))
            .toList(),
        reports.rows(r));
  }

  /**
   * The Excel file of a report.
   *
   * @param id report
   * @return file
   */
  @GetMapping("/{id}/file")
  public ResponseEntity<byte[]> file(@PathVariable Long id) {
    RunFile f = archive.file(reports.get(id).getRunId());
    return PackageRequestController.file(
        new MessageFile(f.fileName(), f.contentType(), f.content()));
  }

  private ReportView view(PlacementReport r) {
    return new ReportView(
        r.getId(),
        r.getReportDate(),
        r.getPeriodFrom(),
        r.getPeriodTo(),
        r.getFileName(),
        r.getRecordCount(),
        "QUOTATION".equals(r.getScope()) ? "Quotation requests" : "Quotation and package requests",
        PlacementUpdateService.SCHEDULED.equals(r.getTriggerType()) ? "Scheduled" : "On request",
        generatedBy(r.getGeneratedBy()),
        r.getGeneratedAt());
  }

  private String generatedBy(String username) {
    String name = users.displayName(username);
    return name == null || "SYSTEM".equals(name) ? "System (scheduled)" : name;
  }

  /**
   * Company of a run on request.
   *
   * @param companyId company
   */
  public record GenerateBody(@NotNull Long companyId) {}

  /**
   * A report of the repository.
   *
   * @param id report
   * @param reportDate report date
   * @param periodFrom first day of the period
   * @param periodTo last day of the period
   * @param fileName file name
   * @param recordCount rows
   * @param scope scope name
   * @param trigger Scheduled or On request
   * @param generatedBy name of the user (the system for the scheduled run)
   * @param generatedAt time
   */
  public record ReportView(
      Long id,
      LocalDate reportDate,
      LocalDate periodFrom,
      LocalDate periodTo,
      String fileName,
      int recordCount,
      String scope,
      String trigger,
      String generatedBy,
      Instant generatedAt) {}

  /**
   * A column of the preview.
   *
   * @param key key
   * @param label label
   */
  public record ColumnView(String key, String label) {}

  /**
   * The preview of a report.
   *
   * @param report report
   * @param columns columns
   * @param rows rows
   */
  public record Preview(
      ReportView report, List<ColumnView> columns, List<Map<String, String>> rows) {}
}
