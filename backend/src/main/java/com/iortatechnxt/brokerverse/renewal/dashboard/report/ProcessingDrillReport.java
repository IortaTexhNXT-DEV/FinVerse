package com.iortatechnxt.brokerverse.renewal.dashboard.report;

import com.iortatechnxt.brokerverse.renewal.dashboard.api.ProcessingDashboardController.ProcessingRow;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardFilter;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.ProcessingDashboardService;
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
 * Export, save and print of a card of the Processing dashboard (RNW-PROCESSING-DASHBOARD; BDOI
 * Renewal FRS FRRN.003.04): the accounts of a KPI card with the columns of its drill-down.
 */
@Component
public class ProcessingDrillReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-PROCESSING-DASHBOARD";

  private static final String COMPANY = "companyId";
  private static final String BUSINESS_TYPE = "businessType";

  private static Map<String, Object> row(ProcessingRow r) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("arn", r.arn());
    m.put(BUSINESS_TYPE, r.businessType());
    m.put("branch", r.branch());
    m.put("department", r.department());
    m.put("unitHead", r.unitHead());
    m.put("accountOfficer", r.accountOfficer());
    m.put("assured", r.assured());
    m.put("insurer", r.insurer());
    m.put("riskCode", r.riskCode());
    m.put("insuranceLine", r.insuranceLine());
    m.put("inceptionDate", r.inceptionDate());
    m.put("expiryDate", r.expiryDate());
    m.put("datePosted", r.datePosted());
    m.put("placementDate", r.placementDate());
    m.put("bookingDate", r.bookingDate());
    m.put("processor", r.processor());
    m.put("sumInsured", r.sumInsured());
    m.put("premium", r.premium());
    m.put("commission", r.commission());
    m.put("policyNumber", r.policyNumber());
    m.put("policyStatus", r.policyStatus());
    m.put("ageing", r.ageing());
    m.put("tatStatus", r.tatStatus());
    m.put("transmittalStatus", r.transmittalStatus());
    m.put("returnReason", r.returnReason());
    return m;
  }

  private final ProcessingDashboardService processing;

  /**
   * Creates the report.
   *
   * @param processing processing dashboard
   */
  public ProcessingDrillReport(ProcessingDashboardService processing) {
    this.processing = processing;
  }

  @Override
  public ReportMetadata metadata() {
    List<String> cards = ProcessingDashboardService.CARDS.stream().map(c -> c[0]).toList();
    return ReportMetadata.renewal(
        CODE,
        "Processing Dashboard Drill-down",
        "The New Business and renewal accounts behind a card of the Processing dashboard",
        List.of(
            ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.select("card", "Card", cards, "FOR_PLACEMENT"),
            ParameterSpec.select(
                BUSINESS_TYPE, "Business Type", List.of("ALL", "NEW_BUSINESS", "RENEWAL"), "ALL"),
            ParameterSpec.optional("tab", "Tab (Assigned / Unassigned)", ParameterType.TEXT),
            ParameterSpec.optional("from", "Period From", ParameterType.DATE),
            ParameterSpec.optional("to", "Period To", ParameterType.DATE),
            ParameterSpec.optional("segment", "Market Segment", ParameterType.TEXT),
            ParameterSpec.optional("officer", "Account Officer", ParameterType.TEXT)));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    DashboardFilter f =
        new DashboardFilter(
            p.longValue(COMPANY),
            p.optionalDate("from").orElse(null),
            p.optionalDate("to").orElse(null),
            p.optionalText("segment").orElse(null),
            p.optionalText("officer").orElse(null),
            p.optionalText(BUSINESS_TYPE).orElse(null));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (var item : processing.drill(f, p.text("card"), p.optionalText("tab").orElse(null))) {
      rows.add(row(ProcessingRow.of(item)));
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("arn", "Account Reference Number"),
            ReportColumn.text(BUSINESS_TYPE, "Business Type"),
            ReportColumn.text("branch", "BDOI Branch"),
            ReportColumn.text("department", "Department"),
            ReportColumn.text("unitHead", "Unit Head"),
            ReportColumn.text("accountOfficer", "Account Officer"),
            ReportColumn.text("assured", "Assured's Name"),
            ReportColumn.text("insurer", "Insurer"),
            ReportColumn.text("riskCode", "Risk Code"),
            ReportColumn.text("insuranceLine", "Insurance Line"),
            ReportColumn.date("inceptionDate", "Inception Date"),
            ReportColumn.date("expiryDate", "Expiry Date"),
            ReportColumn.date("datePosted", "Date Posted"),
            ReportColumn.date("placementDate", "Placement Date"),
            ReportColumn.date("bookingDate", "Booking Date"),
            ReportColumn.text("processor", "Processor"),
            ReportColumn.amount("sumInsured", "Sum Insured"),
            ReportColumn.amount("premium", "Basic Premium"),
            ReportColumn.amount("commission", "Commission"),
            ReportColumn.text("policyNumber", "Policy Number"),
            ReportColumn.text("policyStatus", "Policy Status"),
            ReportColumn.count("ageing", "Ageing (Days)"),
            ReportColumn.text("tatStatus", "TAT Status"),
            ReportColumn.text("transmittalStatus", "Policy Transmittal Status"),
            ReportColumn.text("returnReason", "Reason for Return"))
        .rows(rows)
        .presorted()
        .build();
  }
}
