package com.iortatechnxt.finverse.reinsurance.report;

import static com.iortatechnxt.finverse.reinsurance.report.RiReportSupport.K_BRANCH;
import static com.iortatechnxt.finverse.reinsurance.report.RiReportSupport.K_CLASS;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.insurance.ClaimMovementType;
import com.iortatechnxt.finverse.reinsurance.domain.Cession;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimMovement;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimMovementRepository;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * PGIR0696 Treaty Claims Paid: per claim with a loss date in the period, the amount paid (net of
 * salvage) and its split between facultative, retention, quota share and surplus. Recovery Paid % =
 * reinsurance recovery / paid x 100. Branch &gt; Class.
 */
@Component
public class TreatyClaimsPaidReport implements ReportDefinition {

  private final RiClaimMovementRepository movements;
  private final ClaimReportData data;
  private final RiReportSupport support;

  /**
   * Creates the report.
   *
   * @param movements claim movements
   * @param data claim report data
   * @param support shared support
   */
  public TreatyClaimsPaidReport(
      RiClaimMovementRepository movements, ClaimReportData data, RiReportSupport support) {
    this.movements = movements;
    this.data = data;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = RiReportSupport.rangeParams();
    params.addAll(RiReportSupport.dateRange("Loss Date From", "Loss Date To"));
    return new ReportMetadata(
        "PGIR0696",
        "Treaty Claims Paid",
        ReportCategory.REINSURANCE,
        "Claims paid per claim with the reinsurance recovery by treaty",
        params,
        Permission.REINSURANCE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(RiReportSupport.COMPANY);
    Map<Long, List<RiClaimMovement>> claims =
        movements
            .findByCompanyIdAndLossDateBetweenOrderByClaimNoAscIdAsc(
                companyId, p.date(RiReportSupport.FROM), p.date(RiReportSupport.TO))
            .stream()
            .filter(m -> m.getMovementType() != ClaimMovementType.RESERVE_CHANGE)
            .filter(m -> RiReportSupport.inRange(p, m.getBranchId(), m.getBusinessLine()))
            .collect(
                Collectors.groupingBy(
                    RiClaimMovement::getClaimId, LinkedHashMap::new, Collectors.toList()));
    List<RiClaimMovement> all = claims.values().stream().flatMap(List::stream).toList();
    Map<Long, Cession> cessions = data.cessionsOf(all);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (List<RiClaimMovement> claim : claims.values()) {
      rows.add(row(claim, cessions.get(claim.get(0).getCessionId()), branches));
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("product", "Product"),
            ReportColumn.text("claimNo", "Claim No"),
            ReportColumn.amount("paid", "Claim Paid"),
            ReportColumn.percent("recoveryPct", "Recovery Paid %"),
            ReportColumn.amount("fac", "FAC"),
            ReportColumn.amount("retention", "Retention"),
            ReportColumn.amount("qs", "QS"),
            ReportColumn.amount("surplus", "Surplus"),
            ReportColumn.amount("xol", "XOL"))
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .rows(rows)
        .note("Base currency. Retention = paid - all reinsurance recoveries (incl. XOL).")
        .build();
  }

  private static Map<String, Object> row(
      List<RiClaimMovement> claim, Cession c, Map<Long, String> branches) {
    RiClaimMovement m = claim.get(0);
    BigDecimal paid =
        claim.stream().map(RiClaimMovement::signedLoss).reduce(Money.zero(), BigDecimal::add);
    Map<RiLayer, BigDecimal> split =
        ClaimReportData.byLayer(claim.stream().flatMap(x -> x.getShares().stream()).toList());
    BigDecimal recovery =
        claim.stream().map(RiClaimMovement::ceded).reduce(Money.zero(), BigDecimal::add);
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(K_BRANCH, branches.get(m.getBranchId()));
    row.put(K_CLASS, m.getBusinessLine());
    row.put("product", c == null ? "" : c.getProductCode());
    row.put("claimNo", m.getClaimNo());
    row.put("paid", paid);
    row.put("recoveryPct", RiReportSupport.pct(recovery, paid));
    row.put("fac", split.get(RiLayer.FAC));
    row.put("retention", paid.subtract(recovery));
    row.put("qs", split.get(RiLayer.QUOTA_SHARE));
    row.put("surplus", split.get(RiLayer.SURPLUS));
    row.put("xol", split.get(RiLayer.XOL));
    return row;
  }
}
