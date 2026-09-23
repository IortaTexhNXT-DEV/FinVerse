package com.iortatechnxt.finverse.underwriting.report;

import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_SHARE;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.underwriting.domain.MarineDetails;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PolicyRisk;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR027 Schedule of Shipment under Open Policy: approved marine certificates of an open cover
 * with their shipment details, in the cover currency.
 */
@Component
public class ShipmentScheduleReport implements ReportDefinition {

  private static final String OPEN_COVER = "openCoverNo";
  private static final MarineDetails NONE =
      new MarineDetails(null, null, null, null, null, null, null, null, null);

  private final UwReportSupport support;

  /**
   * Creates the report.
   *
   * @param support shared report support
   */
  public ShipmentScheduleReport(UwReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(UwReportSupport.COMPANY, "Company", ParameterType.COMPANY));
    params.add(ParameterSpec.optional(OPEN_COVER, "Open Policy No", ParameterType.TEXT));
    params.addAll(
        UwReportSupport.dateRange(
            "Certificate Approval Date From", "Certificate Approval Date To"));
    return new ReportMetadata(
        "PGIBR027",
        "Schedule of Shipment under Open Policy",
        ReportCategory.UNDERWRITING,
        "Marine certificates declared under open covers with shipment details",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate from = p.date(UwReportSupport.FROM);
    LocalDate to = p.date(UwReportSupport.TO);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Policy cert :
        support.approvedCertificates(
            p.longValue(UwReportSupport.COMPANY), p.optionalText(OPEN_COVER).orElse(null))) {
      LocalDate approved = cert.getWorkflow().getApprovalDate();
      if (!approved.isBefore(from) && !approved.isAfter(to)) {
        rows.add(row(cert));
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("certificateNo", "Certificate No"),
            ReportColumn.text("lcNo", "LC No"),
            ReportColumn.text("bank", "Bank"),
            ReportColumn.text("vessel", "Vessel"),
            ReportColumn.date("sailDate", "Sail Date"),
            ReportColumn.text("portFrom", "Port From"),
            ReportColumn.text("portTo", "Port To"),
            ReportColumn.text("blNo", "B/L No"),
            ReportColumn.date("blDate", "B/L Date"),
            ReportColumn.text("valuation", "Basis of Valuation"),
            ReportColumn.percent(K_SHARE, "Our Share %"),
            ReportColumn.amount("si", "SI"),
            ReportColumn.amount("net", "Net Premium"),
            ReportColumn.amount("charges", "Charges"),
            ReportColumn.amount("commission", "Commission"))
        .groupBy(OPEN_COVER, "Open Policy")
        .rows(rows)
        .note("Amounts in the open cover currency.")
        .build();
  }

  private static Map<String, Object> row(Policy cert) {
    List<PolicyRisk> risks = cert.getRisks();
    MarineDetails d =
        risks.isEmpty() || risks.get(0).marine() == null ? NONE : risks.get(0).marine();
    PremiumBreakdown b = cert.getPremium();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(OPEN_COVER, cert.getOpenCover().getOpenCoverNo());
    m.put("certificateNo", cert.getPolicyNo());
    m.put("lcNo", d.lcNo());
    m.put("bank", d.bankName());
    m.put("vessel", d.vesselName());
    m.put("sailDate", d.sailDate());
    m.put("portFrom", d.voyageFrom());
    m.put("portTo", d.voyageTo());
    m.put("blNo", d.blNo());
    m.put("blDate", d.blDate());
    m.put("valuation", d.valuationBasis());
    m.put(K_SHARE, cert.getSharePct());
    m.put("si", b.getOurSumInsured());
    m.put("net", b.getOurNetPremium());
    m.put("charges", b.taxesAndCharges());
    m.put("commission", b.getCommission());
    return m;
  }
}
