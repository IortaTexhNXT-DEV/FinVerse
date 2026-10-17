package com.iortatechnxt.brokerverse.productmaint.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.productmaint.domain.PlacementReport;
import com.iortatechnxt.brokerverse.productmaint.domain.PlacementReportRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.ProposalFile;
import com.iortatechnxt.brokerverse.productmaint.report.ConsolidatedPlacementReport;
import com.iortatechnxt.brokerverse.report.core.ExportFileNames;
import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Consolidated Placement Update Report of BDOI's FRS (FRPM.007.01): generated every week on the
 * configured day and time (Thursday 08:00 by default) by the job PLACEMENT_UPDATE_REPORT, or on
 * request, as {@code PlacementUpdate_MMDDYYYY.xlsx}; each file is kept in the repository with the
 * rows of its preview (the same rows as the Excel file) and can be downloaded again.
 */
@Service
@Transactional
public class PlacementUpdateService {

  /** Scheduled run. */
  public static final String SCHEDULED = "SCHEDULED";

  /** Run on request. */
  public static final String ON_REQUEST = "ON_REQUEST";

  /** Event of a new report. */
  public static final String EVENT = "PM_PLACEMENT_UPDATE";

  private static final TypeReference<List<Map<String, String>>> ROWS = new TypeReference<>() {};
  private static final int DEFAULT_DAYS = 7;
  private static final LocalTime DEFAULT_TIME = LocalTime.of(8, 0);

  private final ReportService reports;
  private final ReportArchiveService archive;
  private final PlacementUpdateQuery query;
  private final ConsolidatedPlacementReport report;
  private final PlacementReportRepository repository;
  private final SystemParameterService parameters;
  private final NoticeDelivery delivery;
  private final ObjectMapper json;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param reports report run and rendering
   * @param archive report archive (the files)
   * @param query report rows
   * @param report report definition (cells)
   * @param repository generated reports
   * @param parameters business parameters (schedule, period, scope)
   * @param delivery notices
   * @param json JSON of the rows
   * @param currentUser current user
   * @param clock clock
   */
  public PlacementUpdateService(
      ReportService reports,
      ReportArchiveService archive,
      PlacementUpdateQuery query,
      ConsolidatedPlacementReport report,
      PlacementReportRepository repository,
      SystemParameterService parameters,
      NoticeDelivery delivery,
      ObjectMapper json,
      CurrentUser currentUser,
      Clock clock) {
    this.reports = reports;
    this.archive = archive;
    this.query = query;
    this.report = report;
    this.repository = repository;
    this.parameters = parameters;
    this.delivery = delivery;
    this.json = json;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The schedule and scope in force.
   *
   * @return settings
   */
  @Transactional(readOnly = true)
  public Settings settings() {
    DayOfWeek day = day();
    LocalTime time = time();
    ZonedDateTime now = BusinessClock.now(clock);
    ZonedDateTime next = now.with(time);
    while (next.getDayOfWeek() != day || !next.isAfter(now)) {
      next = next.plusDays(1).with(time);
    }
    return new Settings(day.name(), time.toString(), days(), scope(), next.toLocalDateTime());
  }

  /**
   * Whether the scheduled report of a company is due now: the configured day, from the configured
   * time on, and not yet generated that day.
   *
   * @param companyId company
   * @param now business date and time
   * @return true when due
   */
  @Transactional(readOnly = true)
  public boolean due(Long companyId, ZonedDateTime now) {
    return now.getDayOfWeek() == day()
        && !now.toLocalTime().isBefore(time())
        && !repository.existsByCompanyIdAndReportDateAndTriggerType(
            companyId, now.toLocalDate(), SCHEDULED);
  }

  /**
   * Generates the report of a company for the period ending on the report date, archives the Excel
   * file and keeps it in the repository.
   *
   * @param companyId company
   * @param reportDate report date (last day of the period)
   * @param trigger SCHEDULED or ON_REQUEST
   * @return the report
   */
  public PlacementReport generate(Long companyId, LocalDate reportDate, String trigger) {
    LocalDate from = reportDate.minusDays(days() - 1L);
    String scope = scope();
    List<Map<String, String>> shown =
        query.rows(companyId, from, reportDate, scope).stream()
            .map(r -> text(report.cells(r)))
            .toList();
    Map<String, String> params = new LinkedHashMap<>();
    params.put("companyId", String.valueOf(companyId));
    params.put(ConsolidatedPlacementReport.FROM, from.toString());
    params.put(ConsolidatedPlacementReport.TO, reportDate.toString());
    params.put(ConsolidatedPlacementReport.SCOPE, scope);
    ReportResult result = reports.runForExport(ConsolidatedPlacementReport.CODE, params);
    byte[] xlsx = reports.render(result, ExportFormat.XLSX, null, companyId);
    String fileName =
        ExportFileNames.bdoi(parameters)
            ? ExportFileNames.dated("PlacementUpdate", reportDate, "xlsx")
            : ConsolidatedPlacementReport.CODE + ".xlsx";
    ReportRun run =
        archive.archiveGenerated(
            ConsolidatedPlacementReport.CODE,
            List.of(DisplayFormat.period(from, reportDate)),
            shown.size(),
            new RunFile(ExportFormat.XLSX.name(), fileName, ExportFormat.XLSX.contentType(), xlsx),
            null);
    PlacementReport saved =
        repository.save(
            new PlacementReport(
                new PlacementReport.Period(companyId, reportDate, from, reportDate, scope, trigger),
                new PlacementReport.FileRef(fileName, run.getId()),
                new PlacementReport.Rows(shown.size(), write(shown)),
                new ProposalFile.Generated(currentUser.username(), clock.instant())));
    delivery.toPermission(
        "PKG_REPORT_VIEW",
        new Notice(
            fileName + " generated",
            shown.size() + " request(s) of " + DisplayFormat.period(from, reportDate),
            "/product-maintenance/placement-reports",
            "PlacementReport",
            String.valueOf(saved.getId())),
        EVENT,
        false);
    return saved;
  }

  /**
   * The reports of a company, newest first.
   *
   * @param companyId company
   * @param page page
   * @return reports
   */
  @Transactional(readOnly = true)
  public Page<PlacementReport> list(Long companyId, Pageable page) {
    return repository.findByCompanyIdOrderByReportDateDescIdDesc(companyId, page);
  }

  /**
   * One report.
   *
   * @param id report
   * @return report
   */
  @Transactional(readOnly = true)
  public PlacementReport get(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Placement Update Report", id));
  }

  /**
   * The rows of a report as previewed (and exported).
   *
   * @param report report
   * @return rows by column key
   */
  public List<Map<String, String>> rows(PlacementReport report) {
    try {
      return json.readValue(report.getRowsJson(), ROWS);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unreadable placement report " + report.getId(), e);
    }
  }

  /**
   * The columns of the report.
   *
   * @return key and label of each column
   */
  public static List<ReportColumn> columns() {
    return ConsolidatedPlacementReport.COLUMNS;
  }

  private static Map<String, String> text(Map<String, Object> cells) {
    Map<String, String> out = new LinkedHashMap<>();
    cells.forEach(
        (k, v) ->
            out.put(
                k,
                v instanceof LocalDate d ? DisplayFormat.date(d) : v == null ? "" : v.toString()));
    return out;
  }

  private String write(List<Map<String, String>> rows) {
    try {
      return json.writeValueAsString(rows);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Placement report rows cannot be written", e);
    }
  }

  private DayOfWeek day() {
    try {
      return DayOfWeek.valueOf(
          parameters.text("PLACEMENT_UPDATE_DAY", "THURSDAY").strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return DayOfWeek.THURSDAY;
    }
  }

  private LocalTime time() {
    try {
      return LocalTime.parse(parameters.text("PLACEMENT_UPDATE_TIME", "08:00").strip());
    } catch (DateTimeParseException e) {
      return DEFAULT_TIME;
    }
  }

  private int days() {
    return Math.max(1, parameters.intValue("PLACEMENT_UPDATE_DAYS", DEFAULT_DAYS));
  }

  private String scope() {
    return PlacementUpdateQuery.QUOTATION_AND_PACKAGE.equals(
            parameters.text("PLACEMENT_UPDATE_SCOPE", PlacementUpdateQuery.QUOTATION).strip())
        ? PlacementUpdateQuery.QUOTATION_AND_PACKAGE
        : PlacementUpdateQuery.QUOTATION;
  }

  /**
   * The schedule and scope in force.
   *
   * @param day weekday of the run
   * @param time time of the run
   * @param periodDays length of the period
   * @param scope scope
   * @param nextRun next scheduled run (business time)
   */
  public record Settings(
      String day, String time, int periodDays, String scope, java.time.LocalDateTime nextRun) {}
}
