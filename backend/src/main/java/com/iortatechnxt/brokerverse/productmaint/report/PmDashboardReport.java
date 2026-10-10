package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.productmaint.service.PmDashboardQuery;
import com.iortatechnxt.brokerverse.productmaint.service.PmDashboardQuery.DrillRow;
import com.iortatechnxt.brokerverse.productmaint.service.PmDashboardQuery.Kpi;
import com.iortatechnxt.brokerverse.report.core.NamedExport;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
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
 * Export of the Product Maintenance dashboard (BDOI FRS FRPM.001.01, PM-DASHBOARD): the drill-down
 * rows of one KPI or of every KPI for the filters on screen and what the user may see, in Excel,
 * PDF or CSV.
 */
@Component
public class PmDashboardReport implements ReportDefinition, NamedExport {

  /** Report code. */
  public static final String CODE = "PM-DASHBOARD";

  private static final String FROM = "from";
  private static final String TO = "to";
  private static final String OFFICER = "tsuOfficer";
  private static final String LINE = "lineCode";
  private static final String TYPE = "packageType";
  private static final String KPI = "kpi";
  private static final String PACKAGE = "PACKAGE";
  private static final String NON_PACKAGE = "NON_PACKAGE";

  private final PmDashboardQuery query;
  private final UserDisplayNames names;

  /**
   * Creates the report.
   *
   * @param query dashboard reads
   * @param names user names
   */
  public PmDashboardReport(PmDashboardQuery query, UserDisplayNames names) {
    this.query = query;
    this.names = names;
  }

  @Override
  public String exportName() {
    return "Product Maintenance Dashboard";
  }

  @Override
  public ReportMetadata metadata() {
    List<String> kpis = new ArrayList<>(List.of(PmReportSupport.ALL));
    PmDashboardQuery.KPIS.forEach(k -> kpis.add(k.name()));
    return PmReportSupport.metadata(
        CODE,
        "Product Maintenance Dashboard",
        "Requests behind the dashboard figures for the period, TSU officer, line and package type",
        ParameterSpec.optional(FROM, "Period From", ParameterType.DATE),
        ParameterSpec.optional(TO, "Period To", ParameterType.DATE),
        ParameterSpec.optional(OFFICER, "TSU Officer", ParameterType.TEXT),
        ParameterSpec.optional(LINE, "Product Line", ParameterType.TEXT),
        ParameterSpec.select(
            TYPE,
            "Package Type",
            List.of(PmReportSupport.ALL, PACKAGE, NON_PACKAGE),
            PmReportSupport.ALL),
        ParameterSpec.select(KPI, "KPI", kpis, PmReportSupport.ALL));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String type = PmReportSupport.selected(p, TYPE);
    PmDashboardQuery.Filter filter =
        new PmDashboardQuery.Filter(
            p.longValue(PmReportSupport.COMPANY),
            p.optionalDate(FROM).orElse(null),
            p.optionalDate(TO).orElse(null),
            p.optionalText(OFFICER).orElse(null),
            PmReportSupport.upper(p, LINE),
            type == null ? null : PACKAGE.equals(type));
    String selected = PmReportSupport.selected(p, KPI);
    List<Kpi> kpis = selected == null ? PmDashboardQuery.KPIS : List.of(Kpi.valueOf(selected));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Kpi kpi : kpis) {
      for (DrillRow r : query.all(filter, kpi)) {
        rows.add(row(kpi, r));
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(KPI, "KPI"),
            ReportColumn.text("request", "Request Number"),
            ReportColumn.text("type", "Request Type"),
            ReportColumn.text("line", "Product Line"),
            ReportColumn.text("requestedBy", "Requested By"),
            ReportColumn.text("officer", "Assigned TSU Officer"),
            ReportColumn.text("status", "Current Status"),
            ReportColumn.date("submitted", "Submission Date"),
            ReportColumn.count("aging", "Aging (Days)"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  private Map<String, Object> row(Kpi kpi, DrillRow r) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(KPI, kpi.label());
    row.put("request", r.requestNo());
    row.put("type", r.requestType());
    row.put("line", r.productLine());
    row.put("requestedBy", names.displayName(r.requestedBy()));
    row.put("officer", names.displayName(r.assignee()));
    row.put("status", r.status());
    row.put("submitted", r.submittedAt() == null ? null : BusinessClock.dateOf(r.submittedAt()));
    row.put("aging", r.agingDays());
    return row;
  }
}
