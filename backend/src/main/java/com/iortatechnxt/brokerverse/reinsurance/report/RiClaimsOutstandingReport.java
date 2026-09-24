package com.iortatechnxt.brokerverse.reinsurance.report;

import static com.iortatechnxt.brokerverse.reinsurance.report.RiReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.reinsurance.report.RiReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.reinsurance.report.RiReportSupport.K_UW_YEAR;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.insurance.ClaimMovementType;
import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiClaimMovement;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiClaimMovementRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiClaimShare;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.brokerverse.reinsurance.domain.Treaty;
import com.iortatechnxt.brokerverse.reinsurance.domain.TreatyLayer;
import com.iortatechnxt.brokerverse.reinsurance.service.AllocationMath;
import com.iortatechnxt.brokerverse.reinsurance.service.TreatyService;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * PGIR0639 Reinsurance Claims Outstanding Register: per claim, the outstanding reserve at the as-on
 * date (estimate − paid, i.e. the sum of reserve changes) and the reinsurers' share of it by layer;
 * the excess of loss column estimates the further recovery on the net retained loss (paid +
 * outstanding) above the priority. FAC amount / % are the facultative sum insured of the policy. UW
 * Year &gt; Branch &gt; Class.
 */
@Component
public class RiClaimsOutstandingReport implements ReportDefinition {

  private static final String AS_ON = "asOnDate";

  private final RiClaimMovementRepository movements;
  private final ClaimReportData data;
  private final RiReportSupport support;
  private final TreatyService treaties;

  /**
   * Creates the report.
   *
   * @param movements claim movements
   * @param data claim report data
   * @param support shared support
   * @param treaties excess of loss programme
   */
  public RiClaimsOutstandingReport(
      RiClaimMovementRepository movements,
      ClaimReportData data,
      RiReportSupport support,
      TreatyService treaties) {
    this.movements = movements;
    this.data = data;
    this.support = support;
    this.treaties = treaties;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = RiReportSupport.rangeParams();
    params.add(ParameterSpec.required(AS_ON, "As On", ParameterType.DATE).withDefault("TODAY"));
    params.add(RiReportSupport.statusParam());
    return new ReportMetadata(
        "PGIR0639",
        "Reinsurance Claims Outstanding Register",
        ReportCategory.REINSURANCE,
        "Outstanding claims and the reinsurers' share by layer",
        params,
        Permission.REINSURANCE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(RiReportSupport.COMPANY);
    LocalDate asOn = p.date(AS_ON);
    Map<Long, List<RiClaimMovement>> reserves =
        byClaim(companyId, asOn, ClaimMovementType.RESERVE_CHANGE);
    Map<Long, List<RiClaimMovement>> paid =
        byClaim(companyId, asOn, ClaimMovementType.PAYMENT, ClaimMovementType.RECOVERY);
    List<RiClaimMovement> all = reserves.values().stream().flatMap(List::stream).toList();
    Map<Long, Cession> cessions = data.cessionsOf(all);
    Map<Long, String> policyNos = data.policyNumbers(all, cessions);
    Map<Long, String> branches = support.branchCodes(companyId);
    String status = p.optionalText(RiReportSupport.STATUS).orElse(RiReportSupport.ALL);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (List<RiClaimMovement> claim : reserves.values()) {
      RiClaimMovement m = claim.get(0);
      BigDecimal os =
          claim.stream().map(RiClaimMovement::getBaseAmount).reduce(Money.zero(), BigDecimal::add);
      Cession c = cessions.get(m.getCessionId());
      if (os.signum() == 0
          || !RiReportSupport.inRange(p, m.getBranchId(), m.getBusinessLine())
          || !statusMatches(status, c)) {
        continue;
      }
      Map<RiLayer, BigDecimal> split = ClaimReportData.byLayer(shares(claim));
      split.put(
          RiLayer.XOL, xolEstimate(m, os, split, paid.getOrDefault(m.getClaimId(), List.of())));
      rows.add(row(rows.size() + 1, m, c, os, split, policyNos, branches));
    }
    List<ReportColumn> columns =
        new ArrayList<>(RiClaimsPaidRegisterReport.columns("os", "Claim O/S"));
    columns.add(ReportColumn.amount("facOs", "FAC O/S"));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy(K_UW_YEAR, "UW Year")
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .rows(rows)
        .note(
            "Base currency. O/S = estimate - paid. XOL = estimated recovery on the net retained loss.")
        .build();
  }

  private Map<Long, List<RiClaimMovement>> byClaim(
      Long companyId, LocalDate asOn, ClaimMovementType first, ClaimMovementType... rest) {
    return movements
        .findByCompanyIdAndMovementTypeInAndMovementDateLessThanEqual(
            companyId, EnumSet.of(first, rest), asOn)
        .stream()
        .collect(
            Collectors.groupingBy(
                RiClaimMovement::getClaimId, LinkedHashMap::new, Collectors.toList()));
  }

  private static boolean statusMatches(String status, Cession c) {
    return switch (status) {
      case RiReportSupport.ALLOCATED -> c != null;
      case RiReportSupport.PROVISIONAL -> c == null;
      default -> true;
    };
  }

  private static List<RiClaimShare> shares(List<RiClaimMovement> claim) {
    return claim.stream().flatMap(m -> m.getShares().stream()).toList();
  }

  private BigDecimal xolEstimate(
      RiClaimMovement m,
      BigDecimal os,
      Map<RiLayer, BigDecimal> split,
      List<RiClaimMovement> paid) {
    Treaty xol =
        treaties
            .programme(m.getCompanyId(), m.getBusinessLine(), m.getLossDate().getYear())
            .excessOfLoss();
    if (xol == null) {
      return Money.zero();
    }
    BigDecimal proportional =
        split.get(RiLayer.QUOTA_SHARE).add(split.get(RiLayer.SURPLUS)).add(split.get(RiLayer.FAC));
    BigDecimal netPaid =
        paid.stream().map(RiClaimMovement::getNetRetained).reduce(Money.zero(), BigDecimal::add);
    BigDecimal recovered = ClaimReportData.byLayer(shares(paid)).get(RiLayer.XOL);
    BigDecimal netLoss = netPaid.add(os).subtract(proportional);
    BigDecimal cover = Money.zero();
    for (TreatyLayer layer : xol.getLayers()) {
      cover =
          cover.add(
              AllocationMath.excessOfLoss(netLoss, layer.getPriority(), layer.getLayerLimit()));
    }
    return cover.subtract(recovered).max(Money.zero());
  }

  private static Map<String, Object> row(
      int serial,
      RiClaimMovement m,
      Cession c,
      BigDecimal os,
      Map<RiLayer, BigDecimal> split,
      Map<Long, String> policyNos,
      Map<Long, String> branches) {
    Map<String, Object> row = new LinkedHashMap<>();
    RiReportSupport.putGroups(
        row,
        c == null ? m.getLossDate().getYear() : c.getUwYear(),
        branches.get(m.getBranchId()),
        m.getBusinessLine());
    row.put("serial", String.valueOf(serial));
    row.put("policyNo", policyNos.get(m.getPolicyId()));
    row.put("claimNo", m.getClaimNo());
    row.put("lossDate", m.getLossDate());
    row.put("risk", ClaimReportData.firstRisk(c));
    row.put("si", ClaimReportData.fullSi(c));
    row.put("sharePct", ClaimReportData.sharePct(c));
    RiClaimsPaidRegisterReport.putSplit(row, os, split, "os");
    row.put("facOs", split.get(RiLayer.FAC));
    BigDecimal facSi =
        c == null
            ? Money.zero()
            : c.toBase(
                c.getLines().stream()
                    .filter(l -> l.getLayer() == RiLayer.FAC)
                    .map(CessionLine::getSumInsured)
                    .reduce(Money.zero(), BigDecimal::add));
    row.put("fac", facSi);
    row.put(
        "facPct", c == null ? BigDecimal.ZERO : RiReportSupport.pct(facSi, c.toBase(c.getOurSi())));
    return row;
  }
}
