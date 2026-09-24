package com.iortatechnxt.brokerverse.reinsurance.report;

import static com.iortatechnxt.brokerverse.reinsurance.report.RiReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.reinsurance.report.RiReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.reinsurance.report.RiReportSupport.K_UW_YEAR;

import com.iortatechnxt.brokerverse.insurance.ClaimMovementType;
import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiClaimMovement;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiClaimMovementRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIR0638 Reinsurance Claims Paid Register: every claim payment (and salvage recovery, negative)
 * with an RI claim date in the period, split between facultative, excess of loss, retention, quota
 * share and surplus with their percentages of the amount paid. UW Year &gt; Branch &gt; Class.
 */
@Component
public class RiClaimsPaidRegisterReport implements ReportDefinition {

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
  public RiClaimsPaidRegisterReport(
      RiClaimMovementRepository movements, ClaimReportData data, RiReportSupport support) {
    this.movements = movements;
    this.data = data;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = RiReportSupport.rangeParams();
    params.addAll(RiReportSupport.dateRange("RI Claim Date From", "RI Claim Date To"));
    params.add(RiReportSupport.statusParam());
    return new ReportMetadata(
        "PGIR0638",
        "Reinsurance Claims Paid Register",
        ReportCategory.REINSURANCE,
        "Claims paid split between FAC, XOL, retention, quota share and surplus",
        params,
        Permission.REINSURANCE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(RiReportSupport.COMPANY);
    List<RiClaimMovement> list =
        movements
            .findByCompanyIdAndMovementDateBetweenOrderByMovementDateAscIdAsc(
                companyId, p.date(RiReportSupport.FROM), p.date(RiReportSupport.TO))
            .stream()
            .filter(m -> m.getMovementType() != ClaimMovementType.RESERVE_CHANGE)
            .filter(m -> RiReportSupport.inRange(p, m.getBranchId(), m.getBusinessLine()))
            .filter(m -> matchesStatus(p, m))
            .toList();
    Map<Long, Cession> cessions = data.cessionsOf(list);
    Map<Long, String> policyNos = data.policyNumbers(list, cessions);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (RiClaimMovement m : list) {
      Cession c = cessions.get(m.getCessionId());
      Map<String, Object> row = new LinkedHashMap<>();
      RiReportSupport.putGroups(
          row,
          c == null ? m.getLossDate().getYear() : c.getUwYear(),
          branches.get(m.getBranchId()),
          m.getBusinessLine());
      row.put("serial", String.valueOf(rows.size() + 1));
      row.put("policyNo", policyNos.get(m.getPolicyId()));
      row.put("claimNo", m.getClaimNo());
      row.put("lossDate", m.getLossDate());
      row.put("risk", ClaimReportData.firstRisk(c));
      row.put("si", ClaimReportData.fullSi(c));
      row.put("sharePct", ClaimReportData.sharePct(c));
      putSplit(row, m.signedLoss(), ClaimReportData.byLayer(m.getShares()), "paid");
      rows.add(row);
    }
    return TabularReportBuilder.of(p)
        .columns(columns("paid", "Claim Paid"))
        .groupBy(K_UW_YEAR, "UW Year")
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .rows(rows)
        .note("Base currency. Retention = paid - FAC - XOL - QS - Surplus; % of the amount paid.")
        .build();
  }

  private static boolean matchesStatus(ReportParameters p, RiClaimMovement m) {
    String status = p.optionalText(RiReportSupport.STATUS).orElse(RiReportSupport.ALL);
    return switch (status) {
      case RiReportSupport.ALLOCATED -> m.getCessionId() != null;
      case RiReportSupport.PROVISIONAL -> m.getCessionId() == null;
      default -> true;
    };
  }

  /**
   * Columns of a claims register (paid or outstanding).
   *
   * @param amountKey key of the claim amount
   * @param amountLabel label of the claim amount
   * @return columns
   */
  static List<ReportColumn> columns(String amountKey, String amountLabel) {
    return List.of(
        ReportColumn.text("serial", "S.No"),
        ReportColumn.text("policyNo", "Policy No"),
        ReportColumn.text("claimNo", "Claim No"),
        ReportColumn.date("lossDate", "Loss Date"),
        ReportColumn.text("risk", "Risk"),
        ReportColumn.amountNoTotal("si", "SI"),
        ReportColumn.percent("sharePct", "Our Share %"),
        ReportColumn.amount("fac", "FAC Amount"),
        ReportColumn.percent("facPct", "FAC %"),
        ReportColumn.amount("xol", "XOL"),
        ReportColumn.amount(amountKey, amountLabel),
        ReportColumn.amount("retention", "Retention"),
        ReportColumn.percent("retentionPct", "Retention %"),
        ReportColumn.amount("qs", "QS"),
        ReportColumn.percent("qsPct", "QS %"),
        ReportColumn.amount("surplus", "Surplus"),
        ReportColumn.percent("surplusPct", "Surplus %"));
  }

  /**
   * Puts the layer split of a claim amount.
   *
   * @param row row
   * @param amount claim amount (company share)
   * @param split reinsurers' shares by layer
   * @param amountKey key of the claim amount
   */
  static void putSplit(
      Map<String, Object> row,
      BigDecimal amount,
      Map<RiLayer, BigDecimal> split,
      String amountKey) {
    BigDecimal fac = split.get(RiLayer.FAC);
    BigDecimal xol = split.get(RiLayer.XOL);
    BigDecimal qs = split.get(RiLayer.QUOTA_SHARE);
    BigDecimal surplus = split.get(RiLayer.SURPLUS);
    BigDecimal retention = amount.subtract(fac).subtract(xol).subtract(qs).subtract(surplus);
    row.put(amountKey, amount);
    row.put("fac", fac);
    row.put("facPct", RiReportSupport.pct(fac, amount));
    row.put("xol", xol);
    row.put("retention", retention);
    row.put("retentionPct", RiReportSupport.pct(retention, amount));
    row.put("qs", qs);
    row.put("qsPct", RiReportSupport.pct(qs, amount));
    row.put("surplus", surplus);
    row.put("surplusPct", RiReportSupport.pct(surplus, amount));
  }
}
