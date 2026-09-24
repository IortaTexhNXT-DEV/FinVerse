package com.iortatechnxt.brokerverse.tax.api;

import com.iortatechnxt.brokerverse.tax.api.dto.CalendarEntryResponse;
import com.iortatechnxt.brokerverse.tax.api.dto.WorksheetResponse;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.BirExportService;
import com.iortatechnxt.brokerverse.tax.service.BirExportService.ExportFile;
import com.iortatechnxt.brokerverse.tax.service.TaxCalendarService;
import com.iortatechnxt.brokerverse.tax.service.TaxWorksheetService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tax worksheets with drill-down, the filing calendar and the BIR list exports. */
@RestController
@RequestMapping("/api/v1/tax")
public class TaxWorksheetController {

  private final TaxWorksheetService worksheets;
  private final TaxCalendarService calendar;
  private final BirExportService exports;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param worksheets worksheets
   * @param calendar filing calendar
   * @param exports BIR list exports
   * @param clock clock
   */
  public TaxWorksheetController(
      TaxWorksheetService worksheets,
      TaxCalendarService calendar,
      BirExportService exports,
      Clock clock) {
    this.worksheets = worksheets;
    this.calendar = calendar;
    this.exports = exports;
    this.clock = clock;
  }

  /**
   * Computes a worksheet.
   *
   * @param kind VAT, EWT, DST, PREMIUM_TAX, LGT or FST
   * @param companyId company
   * @param from period start
   * @param to period end
   * @return worksheet
   */
  @GetMapping("/worksheets/{kind}")
  @PreAuthorize(TaxAccess.VIEW)
  public WorksheetResponse worksheet(
      @PathVariable WorksheetKind kind,
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return WorksheetResponse.from(worksheets.compute(companyId, kind, new TaxPeriod(from, to)));
  }

  /**
   * The filing calendar of a year, with due states as of today.
   *
   * @param companyId company
   * @param year year
   * @return entries
   */
  @GetMapping("/calendar")
  @PreAuthorize(TaxAccess.VIEW)
  public List<CalendarEntryResponse> calendar(
      @RequestParam Long companyId, @RequestParam int year) {
    return calendar.calendar(companyId, year, LocalDate.now(clock)).stream()
        .map(CalendarEntryResponse::from)
        .toList();
  }

  /**
   * BIR list export of a quarter: SLS (sales), SLP (purchases) or QAP (alphalist of payees).
   *
   * @param list sls, slp or qap
   * @param companyId company
   * @param year year
   * @param quarter quarter
   * @return CSV file
   */
  @GetMapping("/exports/{list}")
  @PreAuthorize(TaxAccess.VIEW)
  public ResponseEntity<byte[]> export(
      @PathVariable BirList list,
      @RequestParam Long companyId,
      @RequestParam int year,
      @RequestParam int quarter) {
    ExportFile file =
        switch (list) {
          case SLS -> exports.sales(companyId, year, quarter);
          case SLP -> exports.purchases(companyId, year, quarter);
          case QAP -> exports.alphalist(companyId, year, quarter);
        };
    return TaxAccess.file(file.fileName(), file.contentType(), file.content());
  }

  /** BIR lists available for export. */
  public enum BirList {
    /** Summary List of Sales. */
    SLS,
    /** Summary List of Purchases. */
    SLP,
    /** Quarterly Alphalist of Payees. */
    QAP
  }
}
