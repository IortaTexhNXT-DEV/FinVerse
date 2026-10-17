package com.iortatechnxt.brokerverse.csf.cases.report;

import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseViews.CaseRow;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Case Register ({@value #CODE}; FR-CSF-059, FRS section 6.1.3): one row per case of the period,
 * sorted by creation time, with its categories, owner, status, due date, age, SLA status and
 * escalation.
 */
@Component
public class CsfCaseRegisterReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "CSF-CASES";

  private final CsfCaseStatistics statistics;

  /**
   * Creates the report.
   *
   * @param statistics case rows
   */
  public CsfCaseRegisterReport(CsfCaseStatistics statistics) {
    this.statistics = statistics;
  }

  @Override
  public ReportMetadata metadata() {
    return CaseReportSupport.metadata(
        CODE,
        "Case Register",
        "Cases of the period with their categories, owner, status, due date, age and SLA status",
        true);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows =
        statistics.rows(CaseReportSupport.criteria(p)).stream()
            .map(CsfCaseRegisterReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("case_no", "Case ID"),
            ReportColumn.text("created", "Created"),
            ReportColumn.text("client", "Client / Caller"),
            ReportColumn.text("account", "Account / Policy"),
            ReportColumn.text("case_type", "Case Type"),
            ReportColumn.text("nature", "Nature of Transaction"),
            ReportColumn.text("segment", "Market Segment"),
            ReportColumn.text("unit", "Fulfilment Unit"),
            ReportColumn.text("owner", "Owner"),
            ReportColumn.text("priority", "Priority"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("due", "Due Date"),
            ReportColumn.count("age", "Age (working days)"),
            ReportColumn.text("sla", "SLA Status"),
            ReportColumn.text("escalated", "Escalated"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  private static Map<String, Object> row(CaseRow r) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("case_no", r.caseNo());
    m.put("created", CaseReportSupport.time(r.createdAt()));
    m.put("client", r.client());
    m.put(
        "account",
        r.arn() == null ? null : r.arn() + (r.policyNo() == null ? "" : " / " + r.policyNo()));
    m.put("case_type", r.caseType().label() + " (" + r.tatDays() + " days)");
    m.put("nature", r.nature().label());
    m.put("segment", r.marketSegment().label());
    m.put("unit", r.fulfilmentUnit().label());
    m.put("owner", r.owner());
    m.put("priority", r.priority().label());
    m.put("status", r.statusLabel());
    m.put("due", CaseReportSupport.day(r.dueDate()));
    m.put("age", r.sla().ageDays());
    m.put("sla", r.sla().label());
    m.put(
        "escalated",
        r.escalated() == null
            ? null
            : r.escalated().reason().label() + " " + CaseReportSupport.time(r.escalated().at()));
    return m;
  }
}
