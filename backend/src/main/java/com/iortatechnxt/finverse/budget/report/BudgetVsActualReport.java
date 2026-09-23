package com.iortatechnxt.finverse.budget.report;

import com.iortatechnxt.finverse.budget.service.BudgetMonitoringService;
import com.iortatechnxt.finverse.budget.service.BudgetMonitoringService.BudgetComparison;
import com.iortatechnxt.finverse.budget.service.VarianceLine;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * GL-BVA Budget vs Actual: month and year-to-date budget, actual, variance amount and percent per
 * account (optionally per cost centre), against the approved budget version.
 */
@Component
public class BudgetVsActualReport implements ReportDefinition {

  /** Cost centre split parameter. */
  static final String BY_COST_CENTRE = "byCostCenter";

  private final BudgetMonitoringService monitoring;

  /**
   * Creates the report.
   *
   * @param monitoring budget monitoring
   */
  public BudgetVsActualReport(BudgetMonitoringService monitoring) {
    this.monitoring = monitoring;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-BVA",
        "Budget vs Actual",
        ReportCategory.BUDGET,
        "Month and year-to-date budget against actual with variance amount and percent",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.asOfParam(),
            ParameterSpec.optional(BY_COST_CENTRE, "Split by cost centre", ParameterType.BOOLEAN)),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    BudgetComparison c =
        monitoring.compare(
            p.longValue(GlReportSupport.COMPANY),
            p.date(GlReportSupport.AS_OF),
            p.flag(BY_COST_CENTRE));
    List<Map<String, Object>> rows = c.lines().stream().map(BudgetVsActualReport::row).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("code", "Account"),
            ReportColumn.text("name", "Account Name"),
            ReportColumn.text("costCenter", "Cost Centre"),
            ReportColumn.amount("budgetMonth", "Budget (Month)"),
            ReportColumn.amount("actualMonth", "Actual (Month)"),
            ReportColumn.amount("varMonth", "Variance (Month)"),
            ReportColumn.percent("varMonthPct", "Var % (Month)"),
            ReportColumn.amount("budgetYtd", "Budget (YTD)"),
            ReportColumn.amount("actualYtd", "Actual (YTD)"),
            ReportColumn.amount("varYtd", "Variance (YTD)"),
            ReportColumn.percent("varYtdPct", "Var % (YTD)"),
            ReportColumn.text("assessment", "Assessment"))
        .groupBy("accountClass", "Class")
        .presorted()
        .rows(rows)
        .note(BudgetReports.basisNote(c))
        .note("Variance = actual - budget in natural sign (income and expense positive).")
        .build();
  }

  private static Map<String, Object> row(VarianceLine l) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("accountClass", l.accountClass().name());
    row.put("code", l.accountCode());
    row.put("name", l.accountName());
    row.put("costCenter", Objects.toString(l.costCenter(), ""));
    row.put("budgetMonth", l.budgetMonth());
    row.put("actualMonth", l.actualMonth());
    row.put("varMonth", l.monthVariance());
    row.put("varMonthPct", l.monthVariancePct());
    row.put("budgetYtd", l.budgetYtd());
    row.put("actualYtd", l.actualYtd());
    row.put("varYtd", l.ytdVariance());
    row.put("varYtdPct", l.ytdVariancePct());
    row.put("assessment", l.favourable() ? "Favourable" : "Unfavourable");
    return row;
  }
}
