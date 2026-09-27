package com.iortatechnxt.brokerverse.report.gl;

import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/** Chart of accounts listing with tier, controls and status. */
@Component
public class ChartOfAccountsReport implements ReportDefinition {

  private final ChartOfAccountsService accounts;

  /**
   * Creates the report.
   *
   * @param accounts chart of accounts
   */
  public ChartOfAccountsReport(ChartOfAccountsService accounts) {
    this.accounts = accounts;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-COA",
        "Chart of Accounts",
        ReportCategory.GENERAL_LEDGER,
        "All GL heads with tier, class, controls and authorization status",
        List.of(GlReportSupport.companyParam()),
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows =
        accounts.list(p.longValue(GlReportSupport.COMPANY)).stream()
            .map(ChartOfAccountsReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("code", "Code"),
            ReportColumn.text("name", "Name"),
            ReportColumn.text("level", "Tier"),
            ReportColumn.text("parent", "Parent"),
            ReportColumn.text("postable", "Postable"),
            ReportColumn.text("controls", "Controls"),
            ReportColumn.text("reportGroup", "Statement Line"),
            ReportColumn.text("status", "Status"))
        .groupBy("accountClass", "Class")
        .rows(rows)
        .withoutGrandTotal()
        .build();
  }

  private static Map<String, Object> row(GlAccount a) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("accountClass", a.getAccountClass().name());
    m.put("code", a.getCode());
    m.put("name", a.getName());
    m.put("level", a.getLevel().name());
    m.put("parent", a.getParent() == null ? "" : a.getParent().getCode());
    m.put("postable", a.isPostable() ? "Yes" : "No");
    m.put("controls", controls(a));
    m.put("reportGroup", a.getReportGroup());
    m.put("status", a.isFrozen() ? "FROZEN" : a.getRecordStatus().name());
    return m;
  }

  private static String controls(GlAccount a) {
    StringJoiner flags = new StringJoiner(" ");
    if (a.isControlAccount()) {
      flags.add("Control(" + a.getSubLedgerType() + ")");
    }
    if (!a.isAllowManualPosting()) {
      flags.add("NoManual");
    }
    if (a.isCostCenterRequired()) {
      flags.add("CostCentre");
    }
    if (a.isRevaluationRequired()) {
      flags.add("Reval");
    }
    if (a.isReconcilable()) {
      flags.add("Recon");
    }
    return flags.toString();
  }
}
