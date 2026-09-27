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
import com.iortatechnxt.brokerverse.reserves.domain.DevelopmentPeriod;
import com.iortatechnxt.brokerverse.reserves.domain.TriangleBasis;
import com.iortatechnxt.brokerverse.reserves.service.ReserveAnalysisService;
import com.iortatechnxt.brokerverse.reserves.service.Triangle;
import com.iortatechnxt.brokerverse.reserves.service.TriangleAnalysis;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * RSV-TRIANGLE IBNR Triangle Report: the cumulative development triangle (paid or incurred) of a
 * line of business by accident period and development age, with the chain-ladder cumulative factor,
 * ultimate, incurred to date and IBNR per accident period; the age-to-age factors are listed in the
 * notes.
 */
@Component
public class IbnrTriangleReport implements ReportDefinition {

  private static final String BASIS = "basis";
  private static final String PERIOD = "period";
  private static final String PARAMETER = "PARAMETER";
  private static final String AGE = "age";

  private final ReserveAnalysisService analysis;

  /**
   * Creates the report.
   *
   * @param analysis reserve analysis (triangles)
   */
  public IbnrTriangleReport(ReserveAnalysisService analysis) {
    this.analysis = analysis;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "RSV-TRIANGLE",
        "IBNR Development Triangle",
        ReportCategory.ACTUARIAL,
        "Chain-ladder claims development triangle, factors, ultimates and IBNR of a line",
        List.of(
            ParameterSpec.required(ReserveReportSupport.COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(
                ReserveReportSupport.CLASS, "Line of Business", ParameterType.BUSINESS_LINE),
            ParameterSpec.required(ReserveReportSupport.DATE, "Valuation Date", ParameterType.DATE)
                .withDefault("TODAY"),
            ParameterSpec.select(
                BASIS, "Triangle", List.of(PARAMETER, "PAID", "INCURRED"), PARAMETER),
            ParameterSpec.select(
                PERIOD, "Development Period", List.of(PARAMETER, "YEAR", "QUARTER"), PARAMETER)),
        Permission.RESERVE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String basis = p.text(BASIS);
    String period = p.text(PERIOD);
    TriangleAnalysis a =
        analysis.triangles(
            p.longValue(ReserveReportSupport.COMPANY),
            p.text(ReserveReportSupport.CLASS),
            p.date(ReserveReportSupport.DATE),
            PARAMETER.equals(basis) ? null : TriangleBasis.valueOf(basis),
            PARAMETER.equals(period) ? null : DevelopmentPeriod.valueOf(period),
            null);
    Triangle t = a.basis() == TriangleBasis.PAID ? a.paid() : a.incurred();
    int ages = t.developmentPeriods();
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text("accident", "Accident Period"));
    for (int k = 0; k < ages; k++) {
      columns.add(ReportColumn.amountNoTotal(AGE + k, "Age " + k));
    }
    columns.add(ReportColumn.text("cdf", "Factor to Ultimate"));
    columns.add(ReportColumn.amount("ultimate", "Ultimate"));
    columns.add(ReportColumn.amount("incurred", "Incurred to Date"));
    columns.add(ReportColumn.amount("ibnr", "IBNR"));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (int i = 0; i < t.rows().size(); i++) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("accident", t.accidentLabels().get(i));
      List<BigDecimal> values = t.rows().get(i);
      for (int k = 0; k < values.size(); k++) {
        row.put(AGE + k, values.get(k));
      }
      row.put("cdf", a.projection().cumulativeFactors().get(i).toPlainString());
      row.put("ultimate", a.projection().ultimates().get(i));
      row.put("incurred", a.incurredToDate().get(i));
      row.put("ibnr", a.ibnrByAccident().get(i));
      rows.add(row);
    }
    return TabularReportBuilder.of(p)
        .columns(columns)
        .rows(rows)
        .presorted()
        .note(
            a.basis()
                + " triangle by "
                + a.period()
                + ". Age-to-age factors: "
                + a.projection().factors().stream()
                    .map(BigDecimal::toPlainString)
                    .collect(Collectors.joining(", "))
                + ". IBNR of the line (floored at zero): "
                + a.ibnr().toPlainString())
        .build();
  }
}
