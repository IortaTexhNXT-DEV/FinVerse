package com.iortatechnxt.brokerverse.renewal.dashboard.report;

import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardDrillService;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardFilter;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
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
 * Export, save and print of a drill-down of the Renewal dashboard (RNW-DASHBOARD; BDOI Renewal FRS
 * FRRN.002.07): the accounts of a figure with the columns of the screen, previewed, downloaded as
 * Excel, PDF or CSV and printed with the page size, orientation and margins of the Report Centre.
 */
@Component
public class DashboardDrillReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-DASHBOARD";

  private static final String COMPANY = "companyId";

  private final DashboardDrillService drill;

  /**
   * Creates the report.
   *
   * @param drill drill-down
   */
  public DashboardDrillReport(DashboardDrillService drill) {
    this.drill = drill;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.renewal(
        CODE,
        "Renewal Dashboard Drill-down",
        "The renewal accounts behind a figure of the Renewal dashboard",
        List.of(
            ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required("metric", "Figure", ParameterType.TEXT)
                .withDefault("CARD|TOTAL_EXPIRING"),
            ParameterSpec.optional("from", "Period From", ParameterType.DATE),
            ParameterSpec.optional("to", "Period To", ParameterType.DATE),
            ParameterSpec.optional("segment", "Market Segment", ParameterType.TEXT),
            ParameterSpec.optional("officer", "Account Officer", ParameterType.TEXT),
            ParameterSpec.optional("top", "Biggest Open Deals", ParameterType.NUMBER)));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    DashboardDrillService.Drill d =
        drill.drill(
            new DashboardFilter(
                p.longValue(COMPANY),
                p.optionalDate("from").orElse(null),
                p.optionalDate("to").orElse(null),
                p.optionalText("segment").orElse(null),
                p.optionalText("officer").orElse(null),
                null),
            p.text("metric"),
            p.optionalLong("top").map(Long::intValue).orElse(null));
    List<ReportColumn> columns =
        new ArrayList<>(
            List.of(
                ReportColumn.text("ref", "Reference Number"),
                ReportColumn.text("invoiceNo", "Invoice Number"),
                ReportColumn.text("expiringInvoiceNo", "Expiring Invoice Number"),
                ReportColumn.text("assured", "Assured's Name"),
                ReportColumn.text("productLine", "Product Line"),
                ReportColumn.text("riskCode", "Risk Code"),
                ReportColumn.amount("premium", "Premium"),
                ReportColumn.amount("commission", "Commission"),
                ReportColumn.text("status", "Renewal Status"),
                ReportColumn.text("assignedUser", "Assigned User"),
                ReportColumn.text("insurerDisposition", "Insurer Disposition"),
                ReportColumn.text("insurerRemarks", "Insurer Remarks"),
                ReportColumn.date("expiryDate", "Expiry Date")));
    for (DashboardDrillService.Column c : d.columns()) {
      columns.add(column(c));
    }
    List<Map<String, Object>> rows = new ArrayList<>();
    for (DashboardDrillService.Row r : d.rows()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("ref", r.ref());
      m.put("invoiceNo", r.invoiceNo());
      m.put("expiringInvoiceNo", r.expiringInvoiceNo());
      m.put("assured", r.assured());
      m.put("productLine", r.productLine());
      m.put("riskCode", r.riskCode());
      m.put("premium", r.premium());
      m.put("commission", r.commission());
      m.put("status", r.status());
      m.put("assignedUser", r.assignedUser());
      m.put("insurerDisposition", r.insurerDisposition());
      m.put("insurerRemarks", r.insurerRemarks());
      m.put("expiryDate", r.expiryDate());
      m.putAll(r.extra());
      rows.add(m);
    }
    return TabularReportBuilder.of(p).columns(columns).rows(rows).presorted().build();
  }

  private static ReportColumn column(DashboardDrillService.Column c) {
    return switch (c.kind()) {
      case "DATE" -> ReportColumn.date(c.key(), c.label());
      case "AMOUNT" -> ReportColumn.amount(c.key(), c.label());
      case "NUMBER" -> new ReportColumn(c.key(), c.label(), ColumnType.NUMBER, false);
      default -> ReportColumn.text(c.key(), c.label());
    };
  }
}
