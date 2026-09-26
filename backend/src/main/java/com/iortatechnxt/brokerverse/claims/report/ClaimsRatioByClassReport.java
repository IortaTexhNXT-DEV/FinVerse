package com.iortatechnxt.brokerverse.claims.report;

import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_INSURED;
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
import com.iortatechnxt.brokerverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.brokerverse.underwriting.report.UwFilters;
import com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.brokerverse.underwriting.service.PremiumTransaction;
import com.iortatechnxt.brokerverse.underwriting.service.TransactionQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * PGIBR012 Claims Ratio by Class of Business: approved policies expiring in a period with their
 * premium (all approved transactions) and claims as at the expiry-to date. Paid is net of
 * recoveries; Total = Paid + O/S; Claim Ratio = Total / our net premium × 100 when the premium is
 * not zero. Policy status is "Renewed" when a renewal endorsement was approved, else "New". Branch
 * &gt; Class &gt; Product.
 */
@Component
public class ClaimsRatioByClassReport implements ReportDefinition {

  private final PolicyQueryService policies;
  private final ClaimReportSupport support;

  /**
   * Creates the report.
   *
   * @param policies underwriting read API
   * @param support claims report support
   */
  public ClaimsRatioByClassReport(PolicyQueryService policies, ClaimReportSupport support) {
    this.policies = policies;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = ClaimReportSupport.rangeParams();
    params.addAll(UwReportSupport.dateRange("Policy Expiry From", "Policy Expiry To"));
    return new ReportMetadata(
        "PGIBR012",
        "Claims Ratio by Class of Business",
        ReportCategory.CLAIMS,
        "Premium, claims and claim ratio of policies expiring in a period",
        params,
        Permission.CLAIM_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ClaimReportSupport.COMPANY);
    LocalDate to = p.date(ClaimReportSupport.TO);
    UwFilters f = UwReportSupport.filters(p);
    List<PolicySnapshot> expiring =
        policies.policiesExpiring(companyId, p.date(ClaimReportSupport.FROM), to).stream()
            .filter(f::test)
            .toList();
    Map<Long, List<PremiumTransaction>> premium =
        policies.transactions(TransactionQuery.approved(companyId, null, null)).stream()
            .collect(Collectors.groupingBy(t -> t.ref().policyId()));
    Map<Long, ClaimFigures> claims = support.byPolicy(companyId, support.asOf(companyId, to));
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (PolicySnapshot policy : expiring) {
      List<PremiumTransaction> txns = premium.getOrDefault(policy.id(), List.of());
      ClaimFigures c = claims.getOrDefault(policy.id(), ClaimFigures.none());
      Map<String, Object> m = new LinkedHashMap<>();
      UwReportSupport.putGroups(m, policy, branches);
      m.put(K_POLICY, policy.policyNo());
      m.put("period", policy.periodFrom() + " - " + policy.periodTo());
      m.put("policyStatus", renewed(txns) ? "Renewed" : "New");
      m.put(K_INSURED, policy.insuredName());
      m.put("sharePct", policy.sharePct());
      m.put("si", ClaimReportSupport.premium(txns, t -> t.premium().getOurSumInsured()));
      BigDecimal net = ClaimReportSupport.premium(txns, t -> t.premium().getOurNetPremium());
      m.put("netPremium", net);
      m.put("paid", c.netPaid());
      m.put("os", c.paymentOutstanding(true));
      BigDecimal total = c.netPaid().add(c.paymentOutstanding(true));
      m.put("total", total);
      m.put("ratio", ClaimReportSupport.ratio(total, net));
      rows.add(m);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(K_POLICY, "Policy No"),
            ReportColumn.text("period", "Period"),
            ReportColumn.text("policyStatus", "Policy Status"),
            ReportColumn.text(K_INSURED, "Assured"),
            ReportColumn.percent("sharePct", "Our Share %"),
            ReportColumn.amount("si", "SI"),
            ReportColumn.amount("netPremium", "Net Premium"),
            ReportColumn.amount("paid", "Paid"),
            ReportColumn.amount("os", "O/S"),
            ReportColumn.amount("total", "Total"),
            ReportColumn.percent("ratio", "Claim Ratio %"))
        .groupBy(K_BRANCH, ClaimReportSupport.BRANCH_LABEL)
        .groupBy(K_CLASS, ClaimReportSupport.CLASS_LABEL)
        .groupBy(K_PRODUCT, ClaimReportSupport.PRODUCT_LABEL)
        .rows(rows)
        .note("Claim ratio = (paid + O/S) / our net premium x 100 (0 when there is no premium).")
        .note("Claims as at the expiry-to date; paid is net of recoveries.")
        .note(ClaimReportSupport.AMOUNTS_NOTE)
        .build();
  }

  private static boolean renewed(List<PremiumTransaction> txns) {
    return txns.stream().anyMatch(t -> EndorsementType.RENEWAL.name().equals(t.kind()));
  }
}
