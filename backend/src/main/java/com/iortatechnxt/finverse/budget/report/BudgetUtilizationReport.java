package com.iortatechnxt.finverse.budget.report;

import com.iortatechnxt.finverse.budget.service.BudgetMonitoringService;
import com.iortatechnxt.finverse.budget.service.BudgetMonitoringService.BudgetComparison;
import com.iortatechnxt.finverse.budget.service.VarianceLine;
import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * GL-BUTIL Budget Utilization: annual budget, year-to-date actual, utilization percent and
 * remaining budget per account, flagging accounts over a threshold.
 */
@Component
public class BudgetUtilizationReport implements ReportDefinition {

  private static final String THRESHOLD = "threshold";
  private static final BigDecimal DEFAULT_THRESHOLD = BigDecimal.valueOf(90);

  private final BudgetMonitoringService monitoring;

  /**
   * Creates the report.
   *
   * @param monitoring budget monitoring
   */
  public BudgetUtilizationReport(BudgetMonitoringService monitoring) {
    this.monitoring = monitoring;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-BUTIL",
        "Budget Utilization",
        ReportCategory.BUDGET,
        "Year-to-date consumption of the annual budget with remaining availability",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.asOfParam(),
            ParameterSpec.required(THRESHOLD, "Alert threshold %", ParameterType.NUMBER)
                .withDefault("90")),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    BigDecimal threshold = p.optionalDecimal(THRESHOLD).orElse(DEFAULT_THRESHOLD);
    BudgetComparison c =
        monitoring.compare(
            p.longValue(GlReportSupport.COMPANY), p.date(GlReportSupport.AS_OF), false);
    List<Map<String, Object>> rows = c.lines().stream().map(l -> row(l, threshold)).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("code", "Account"),
            ReportColumn.text("name", "Account Name"),
            ReportColumn.amount("annual", "Annual Budget"),
            ReportColumn.amount("actualYtd", "Actual (YTD)"),
            ReportColumn.percent("utilization", "Utilization %"),
            ReportColumn.amount("available", "Available"),
            ReportColumn.text("flag", "Alert"))
        .groupBy("accountClass", "Class")
        .presorted()
        .rows(rows)
        .note(BudgetReports.basisNote(c))
        .note(
            "Utilization = YTD actual / annual budget. Alert when an expense account reaches "
                + threshold.toPlainString()
                + " %.")
        .build();
  }

  private static Map<String, Object> row(VarianceLine l, BigDecimal threshold) {
    BigDecimal utilization = l.utilizationPct();
    boolean breached =
        l.accountClass() == AccountClass.EXPENSE
            && utilization != null
            && utilization.compareTo(threshold) >= 0;
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("accountClass", l.accountClass().name());
    row.put("code", l.accountCode());
    row.put("name", l.accountName());
    row.put("annual", l.annualBudget());
    row.put("actualYtd", l.actualYtd());
    row.put("utilization", utilization);
    row.put("available", l.available());
    row.put("flag", breached ? "Budget threshold exceeded" : "");
    return row;
  }
}
