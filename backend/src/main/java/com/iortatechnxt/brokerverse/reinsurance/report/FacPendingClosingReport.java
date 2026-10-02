package com.iortatechnxt.brokerverse.reinsurance.report;

import static com.iortatechnxt.brokerverse.reinsurance.report.RiReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.reinsurance.report.RiReportSupport.K_CLASS;

import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacementRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacStatus;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIR0693 Policies Pending for FAC Closing: facultative placements not yet closed (provisional,
 * pending approval or placed) of policies approved in the period, with the provisional requirement
 * and what was placed. Placement % = placed SI / FAC SI x 100. Branch &gt; Class &gt; Product.
 */
@Component
public class FacPendingClosingReport implements ReportDefinition {

  private static final String K_PRODUCT = "product";

  private final FacPlacementRepository placements;
  private final RiReportSupport support;

  /**
   * Creates the report.
   *
   * @param placements placements
   * @param support shared support
   */
  public FacPendingClosingReport(FacPlacementRepository placements, RiReportSupport support) {
    this.placements = placements;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = RiReportSupport.rangeParams();
    params.addAll(RiReportSupport.dateRange("Approval Date From", "Approval Date To"));
    return new ReportMetadata(
        "PGIR0693",
        "Policies Pending for FAC Closing",
        ReportCategory.REINSURANCE,
        "Facultative requirements not yet closed, provisional versus placed",
        params,
        Permission.REINSURANCE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(RiReportSupport.COMPANY);
    LocalDate from = p.date(RiReportSupport.FROM);
    LocalDate to = p.date(RiReportSupport.TO);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (FacPlacement f :
        placements.findByCompanyIdAndStatusInOrderByIdDesc(
            companyId,
            EnumSet.of(FacStatus.PROVISIONAL, FacStatus.PENDING_APPROVAL, FacStatus.PLACED))) {
      Cession c = f.getCession();
      boolean inPeriod = !c.getRiDate().isBefore(from) && !c.getRiDate().isAfter(to);
      if (inPeriod && RiReportSupport.inRange(p, c.getBranchId(), c.getBusinessLine())) {
        rows.add(row(f, c, branches));
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("policyNo", "Policy No"),
            ReportColumn.date("approvalDate", "Approval Date"),
            ReportColumn.text("risk", "Risk"),
            ReportColumn.amount("riskSi", "Risk SI"),
            ReportColumn.amount("riskPremium", "Risk Premium"),
            ReportColumn.percent("facPct", "Prov. FAC %"),
            ReportColumn.amount("facSi", "Prov. FAC SI"),
            ReportColumn.amount("facPremium", "Prov. FAC Premium"),
            ReportColumn.text("placementNo", "Placement No"),
            ReportColumn.amount("placedSi", "Placed SI"),
            ReportColumn.amount("placedPremium", "Placed Premium"),
            ReportColumn.percent("placementPct", "Placement %"),
            ReportColumn.text("status", "Status"),
            ReportColumn.amount("commission", "FAC Commission"))
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .groupBy(K_PRODUCT, "Product")
        .rows(rows)
        .note("Company share, base currency. Placement % = placed SI / FAC SI x 100.")
        .build();
  }

  private static Map<String, Object> row(FacPlacement f, Cession c, Map<Long, String> branches) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(K_BRANCH, branches.get(c.getBranchId()));
    m.put(K_CLASS, c.getBusinessLine());
    m.put(K_PRODUCT, c.getProductCode());
    m.put("policyNo", c.getPolicyNo());
    m.put("approvalDate", c.getRiDate());
    m.put("risk", f.getRiskLineNo() + " " + f.getRiskDescription());
    m.put("riskSi", c.toBase(f.getRiskSi()));
    m.put("riskPremium", c.toBase(f.getRiskPremium()));
    m.put("facPct", f.getFacPct());
    m.put("facSi", c.toBase(f.getFacSi()));
    m.put("facPremium", c.toBase(f.getFacPremium()));
    m.put("placementNo", f.getPlacementNo());
    m.put("placedSi", c.toBase(f.getPlacedSi()));
    m.put("placedPremium", c.toBase(f.getPlacedPremium()));
    m.put("placementPct", f.placementPct());
    m.put("status", f.getStatus().name());
    m.put("commission", c.toBase(f.commission()));
    return m;
  }
}
