package com.iortatechnxt.brokerverse.reserves.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.reserves.service.ReserveAnalysisService;
import com.iortatechnxt.brokerverse.reserves.service.ReserveSummary;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * RSV-SUMMARY Technical Reserves Summary: UPR, DAC, UCR, OSLR, IBNR, ULAE, MfAD and premium
 * deficiency, gross, reinsurers' share and net, per line of business, for the valuation of the
 * month compared with the previous posted valuation.
 */
@Component
public class TechnicalReservesSummaryReport implements ReportDefinition {

  private final ReserveAnalysisService analysis;

  /**
   * Creates the report.
   *
   * @param analysis reserve analysis
   */
  public TechnicalReservesSummaryReport(ReserveAnalysisService analysis) {
    this.analysis = analysis;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "RSV-SUMMARY",
        "Technical Reserves Summary",
        ReportCategory.ACTUARIAL,
        "Technical reserves gross, RI share and net per line: current vs previous valuation",
        List.of(
            ParameterSpec.required(ReserveReportSupport.COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(ReserveReportSupport.DATE, "Valuation Date", ParameterType.DATE)
                .withDefault("TODAY")),
        Permission.RESERVE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    ReserveSummary summary =
        analysis.summary(
            p.longValue(ReserveReportSupport.COMPANY), p.date(ReserveReportSupport.DATE));
    List<Map<String, Object>> rows =
        summary.rows().stream()
            .map(
                r -> {
                  Map<String, Object> row = new LinkedHashMap<>();
                  row.put("reserve", r.reserve());
                  row.put(ReserveReportSupport.K_CLASS, r.businessLine());
                  row.put("gross", r.current().gross());
                  row.put("ri", r.current().ri());
                  row.put("net", r.current().net());
                  row.put("prevGross", r.previous().gross());
                  row.put("prevRi", r.previous().ri());
                  row.put("prevNet", r.previous().net());
                  row.put("change", r.netChange());
                  return row;
                })
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(ReserveReportSupport.K_CLASS, "Line of Business"),
            ReportColumn.amount("gross", "Gross"),
            ReportColumn.amount("ri", "RI Share"),
            ReportColumn.amount("net", "Net"),
            ReportColumn.amount("prevGross", "Previous Gross"),
            ReportColumn.amount("prevRi", "Previous RI Share"),
            ReportColumn.amount("prevNet", "Previous Net"),
            ReportColumn.amount("change", "Change in Net"))
        .groupBy("reserve", "Reserve")
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .note(
            "Current: "
                + (summary.currentDate() == null
                    ? "no valuation run"
                    : summary.currentDate() + " (" + summary.currentStatus() + ")")
                + "; previous posted: "
                + (summary.previousDate() == null ? "none" : summary.previousDate())
                + ". UCR = unearned reinsurance commission (RI side of DAC).")
        .build();
  }
}
