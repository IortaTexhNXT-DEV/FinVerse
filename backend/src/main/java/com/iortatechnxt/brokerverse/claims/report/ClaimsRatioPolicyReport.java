package com.iortatechnxt.brokerverse.claims.report;

import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CUSTOMER;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_POLICY;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_PRODUCT;

import com.iortatechnxt.brokerverse.claims.service.ClaimFigures;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.underwriting.report.UwFilters;
import com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.brokerverse.underwriting.service.PremiumTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * PGIBR028 Claims Ratio – Policy: policies with premium approved in a period, their premium and
 * commission of the period and the development of their claims over it. Opening O/S is the
 * outstanding the day before the period, closing O/S at its end, paid is net of recoveries in the
 * period. Net claim = paid + closing O/S − opening O/S; release = opening − paid − closing; ratio =
 * net claim / net premium × 100. Branch &gt; Class &gt; Product &gt; Customer.
 */
@Component
public class ClaimsRatioPolicyReport implements ReportDefinition {

  private final PolicyQueryService policies;
  private final ClaimReportSupport support;

  /**
   * Creates the report.
   *
   * @param policies underwriting read API
   * @param support claims report support
   */
  public ClaimsRatioPolicyReport(PolicyQueryService policies, ClaimReportSupport support) {
    this.policies = policies;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = ClaimReportSupport.rangeParams();
    params.addAll(UwReportSupport.dateRange("Approval Date From", "Approval Date To"));
    return new ReportMetadata(
        "PGIBR028",
        "Claims Ratio - Policy",
        ReportCategory.CLAIMS,
        "Premium, net claims incurred and claim ratio per policy for a period",
        params,
        Permission.CLAIM_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ClaimReportSupport.COMPANY);
    LocalDate from = p.date(ClaimReportSupport.FROM);
    LocalDate to = p.date(ClaimReportSupport.TO);
    UwFilters f = UwReportSupport.filters(p);
    Map<Long, List<PremiumTransaction>> byPolicy =
        policies.approvedTransactions(companyId, from, to).stream()
            .filter(t -> f.test(t.policy()))
            .collect(Collectors.groupingBy(t -> t.ref().policyId()));
    Map<Long, ClaimFigures> opening =
        support.byPolicy(companyId, support.asOf(companyId, from.minusDays(1)));
    Map<Long, ClaimFigures> closing = support.byPolicy(companyId, support.asOf(companyId, to));
    Map<Long, ClaimFigures> period =
        support.byPolicy(companyId, support.between(companyId, from, to));
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (List<PremiumTransaction> txns : byPolicy.values()) {
      PolicySnapshot policy = txns.get(0).policy();
      Long id = policy.id();
      BigDecimal open = outstanding(opening, id);
      BigDecimal close = outstanding(closing, id);
      BigDecimal paid = period.getOrDefault(id, ClaimFigures.none()).netPaid();
      BigDecimal net = ClaimReportSupport.premium(txns, t -> t.premium().getOurNetPremium());
      BigDecimal netClaim = paid.add(close).subtract(open);
      Map<String, Object> m = new LinkedHashMap<>();
      UwReportSupport.putGroups(m, policy, branches);
      m.put(K_CUSTOMER, policy.customerCode() + " " + policy.customerName());
      m.put(K_POLICY, policy.policyNo());
      m.put("period", policy.periodFrom() + " - " + policy.periodTo());
      m.put("status", policy.status().name());
      m.put("broker", policy.intermediaryName());
      m.put("commission", ClaimReportSupport.premium(txns, t -> t.premium().getCommission()));
      m.put("sharePct", policy.sharePct());
      m.put("netPremium", net);
      m.put("netClaim", netClaim);
      m.put("ratio", ClaimReportSupport.ratio(netClaim, net));
      m.put("openingOs", open);
      m.put("closingOs", close);
      m.put("paid", paid);
      m.put("release", open.subtract(paid).subtract(close));
      rows.add(m);
    }
    return TabularReportBuilder.of(p)
        .columns(columns())
        .groupBy(K_BRANCH, ClaimReportSupport.BRANCH_LABEL)
        .groupBy(K_CLASS, ClaimReportSupport.CLASS_LABEL)
        .groupBy(K_PRODUCT, ClaimReportSupport.PRODUCT_LABEL)
        .groupBy(K_CUSTOMER, "Customer")
        .rows(rows)
        .note("Net claim = paid + closing O/S - opening O/S; release = opening - paid - closing.")
        .note(ClaimReportSupport.AMOUNTS_NOTE)
        .build();
  }

  private static BigDecimal outstanding(Map<Long, ClaimFigures> figures, Long policyId) {
    return figures.getOrDefault(policyId, ClaimFigures.none()).paymentOutstanding(true);
  }

  private static List<ReportColumn> columns() {
    return List.of(
        ReportColumn.text(K_POLICY, "Policy No"),
        ReportColumn.text("period", "Period"),
        ReportColumn.text("status", "Status"),
        ReportColumn.text("broker", "Broker"),
        ReportColumn.amount("commission", "Commission"),
        ReportColumn.percent("sharePct", "Our Share %"),
        ReportColumn.amount("netPremium", "Net Premium"),
        ReportColumn.amount("netClaim", "Net Claim"),
        ReportColumn.percent("ratio", "Ratio %"),
        ReportColumn.amount("openingOs", "Opening O/S"),
        ReportColumn.amount("closingOs", "Closing O/S"),
        ReportColumn.amount("paid", "Paid"),
        ReportColumn.amount("release", "Release"));
  }
}
