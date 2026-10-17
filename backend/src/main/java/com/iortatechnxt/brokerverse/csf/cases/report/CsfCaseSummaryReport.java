package com.iortatechnxt.brokerverse.csf.cases.report;

import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics.Count;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics.Dashboard;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Case Summary ({@value #CODE}; FR-CSF-059, FRS section 6.1.5): the figures of the Case Dashboard
 * for the same filters - volume, average ageing, SLA compliance rate, breached and not breached,
 * and the distribution by status, case type, nature of transaction and fulfilment unit.
 */
@Component
public class CsfCaseSummaryReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "CSF-CASE-SUMMARY";

  private static final String SECTION = "section";
  private static final String MEASURE = "measure";
  private static final String VALUE = "value";

  private final CsfCaseStatistics statistics;

  /**
   * Creates the report.
   *
   * @param statistics dashboard figures
   */
  public CsfCaseSummaryReport(CsfCaseStatistics statistics) {
    this.statistics = statistics;
  }

  @Override
  public ReportMetadata metadata() {
    return CaseReportSupport.metadata(
        CODE,
        "Case Summary",
        "Case volume, ageing, SLA compliance rate and the distribution of the cases, as on the"
            + " Case Dashboard",
        true);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Dashboard d = statistics.dashboard(CaseReportSupport.criteria(p));
    List<Map<String, Object>> rows = new ArrayList<>();
    String totals = "Totals";
    rows.add(row(totals, "Cases created", String.valueOf(d.created())));
    rows.add(row(totals, "Cases closed", String.valueOf(d.closed())));
    rows.add(row(totals, "Cases open", String.valueOf(d.open())));
    rows.add(
        row(
            totals,
            "Average age of closed cases (working days)",
            d.averageAgeClosed() == null ? "-" : d.averageAgeClosed().toPlainString()));
    rows.add(
        row(
            totals,
            "SLA compliance rate (%)",
            d.slaCompliance() == null ? "-" : d.slaCompliance().toPlainString()));
    rows.add(row(totals, "Breached", String.valueOf(d.breached())));
    rows.add(row(totals, "Not breached", String.valueOf(d.notBreached())));
    rows.add(row(totals, "Escalated open cases", String.valueOf(d.escalated())));
    add(rows, "By status", d.byStatus());
    add(rows, "By case type", d.byType());
    add(rows, "By nature of transaction", d.byNature());
    add(rows, "By fulfilment unit", d.byUnit());
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(SECTION, "Section"),
            ReportColumn.text(MEASURE, "Measure"),
            ReportColumn.text(VALUE, "Value"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  private static void add(List<Map<String, Object>> rows, String section, List<Count> counts) {
    counts.forEach(c -> rows.add(row(section, c.label(), String.valueOf(c.count()))));
  }

  private static Map<String, Object> row(String section, String measure, String value) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(SECTION, section);
    m.put(MEASURE, measure);
    m.put(VALUE, value);
    return m;
  }
}
