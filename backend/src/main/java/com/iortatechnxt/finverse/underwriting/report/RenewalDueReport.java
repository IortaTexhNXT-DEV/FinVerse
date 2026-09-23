package com.iortatechnxt.finverse.underwriting.report;

import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_BRANCH;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_CLASS;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_PRODUCT;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_SHARE;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_UW_YEAR;

import com.iortatechnxt.finverse.common.util.Money;
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
import com.iortatechnxt.finverse.underwriting.domain.DateBasis;
import com.iortatechnxt.finverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.finverse.underwriting.service.ClaimsFigures;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import com.iortatechnxt.finverse.underwriting.service.ReinsuranceFigures;
import com.iortatechnxt.finverse.underwriting.service.TransactionQuery;
import com.iortatechnxt.finverse.underwriting.service.TransactionRef;
import com.iortatechnxt.finverse.underwriting.service.UnderwritingPorts;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * PGIBR013 List of Policies Due for Renewal: approved policies expiring in a period with the
 * premium of their current period, FAC ceded and claims experience (claims module). Claim ratio =
 * net claim / our net premium × 100. Branch &gt; Class &gt; Product &gt; UW Year.
 */
@Component
public class RenewalDueReport implements ReportDefinition {

  private static final String EXPIRY_FROM = "expiryFrom";
  private static final String EXPIRY_TO = "expiryTo";
  private static final String[] AMOUNTS = {
    "si", "gross", "discount", "loading", "net", "policyFee", "commission", "fac"
  };
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final PolicyQueryService policies;
  private final UwReportSupport support;
  private final UnderwritingPorts ports;

  /**
   * Creates the report.
   *
   * @param policies policy read service
   * @param support shared report support
   * @param ports claims and reinsurance figures
   */
  public RenewalDueReport(
      PolicyQueryService policies, UwReportSupport support, UnderwritingPorts ports) {
    this.policies = policies;
    this.support = support;
    this.ports = ports;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = UwReportSupport.rangeParams();
    params.add(
        ParameterSpec.required(EXPIRY_FROM, "Expiry Date From", ParameterType.DATE)
            .withDefault("TODAY"));
    params.add(ParameterSpec.optional(EXPIRY_TO, "Expiry Date To", ParameterType.DATE));
    return new ReportMetadata(
        "PGIBR013",
        "List of Policies Due for Renewal",
        ReportCategory.UNDERWRITING,
        "Expiring policies with premium, FAC and claims experience",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(UwReportSupport.COMPANY);
    UwFilters f = UwReportSupport.filters(p);
    List<PolicySnapshot> expiring =
        policies
            .policiesExpiring(
                companyId, p.date(EXPIRY_FROM), p.optionalDate(EXPIRY_TO).orElse(null))
            .stream()
            .filter(f::test)
            .toList();
    Set<Long> ids = expiring.stream().map(PolicySnapshot::id).collect(Collectors.toSet());
    Map<Long, List<PremiumTransaction>> byPolicy =
        policies
            .transactions(
                new TransactionQuery(
                    companyId, DateBasis.APPROVAL, null, null, UwReportSupport.APPROVED))
            .stream()
            .filter(t -> ids.contains(t.ref().policyId()))
            .collect(Collectors.groupingBy(t -> t.ref().policyId()));
    Map<TransactionRef, ReinsuranceFigures> ri =
        ports.reinsurance(
            byPolicy.values().stream().flatMap(List::stream).map(PremiumTransaction::ref).toList());
    Map<Long, ClaimsFigures> claims = ports.claims(ids);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (PolicySnapshot policy : expiring) {
      Map<String, Object> m = new LinkedHashMap<>();
      // The report shows the current period, so it groups by that period's UW year.
      UwReportSupport.putGroups(m, policy, policy.currentUwYear(), branches);
      m.put(UwReportSupport.K_POLICY, policy.policyNo());
      m.put(UwReportSupport.K_INSURED, policy.insuredName());
      m.put("periodFrom", policy.periodFrom());
      m.put("periodTo", policy.periodTo());
      m.put("customer", policy.customerName());
      m.put("intermediary", policy.intermediaryName());
      m.put(K_SHARE, policy.sharePct());
      addPremium(m, currentPeriod(byPolicy.getOrDefault(policy.id(), List.of())), ri);
      addClaims(m, claims.getOrDefault(policy.id(), ClaimsFigures.none()));
      rows.add(m);
    }
    return TabularReportBuilder.of(p)
        .columns(columns())
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .groupBy(K_PRODUCT, "Product")
        .groupBy(K_UW_YEAR, "UW Year")
        .rows(rows)
        .note("Premium of the current period (last renewal onwards), our share, base currency.")
        .note("Claim ratio = net claim / our net premium x 100.")
        .build();
  }

  /** Transactions of the current period: from the latest renewal (or the original issue). */
  private static List<PremiumTransaction> currentPeriod(List<PremiumTransaction> txns) {
    List<PremiumTransaction> sorted =
        txns.stream().sorted(Comparator.comparingInt(PremiumTransaction::endorsementNo)).toList();
    int start = 0;
    for (int i = 0; i < sorted.size(); i++) {
      if (EndorsementType.RENEWAL.name().equals(sorted.get(i).kind())) {
        start = i;
      }
    }
    return sorted.subList(start, sorted.size());
  }

  private static void addPremium(
      Map<String, Object> m,
      List<PremiumTransaction> txns,
      Map<TransactionRef, ReinsuranceFigures> ri) {
    for (String key : AMOUNTS) {
      m.put(key, Money.zero());
    }
    for (PremiumTransaction t : txns) {
      var b = t.premium();
      BigDecimal fac =
          ri.getOrDefault(t.ref(), ReinsuranceFigures.noCession(b.getOurNetPremium())).facPremium();
      BigDecimal[] values = {
        b.getOurSumInsured(),
        b.getOurGrossPremium(),
        b.getOurDiscount(),
        b.getOurLoading(),
        b.getOurNetPremium(),
        b.getPolicyFee(),
        b.getCommission(),
        fac
      };
      for (int i = 0; i < AMOUNTS.length; i++) {
        m.merge(AMOUNTS[i], t.toBase(values[i]), UwReportSupport::add);
      }
    }
    m.put("rate", txns.isEmpty() ? null : txns.get(0).exchangeRate());
  }

  private static void addClaims(Map<String, Object> m, ClaimsFigures c) {
    BigDecimal rate = (BigDecimal) m.remove("rate");
    BigDecimal netClaim = rate == null ? c.netClaims() : Money.convert(c.netClaims(), rate);
    BigDecimal net = (BigDecimal) m.get("net");
    m.put("claims", c.claimCount());
    m.put("netClaim", netClaim);
    m.put(
        "claimRatio",
        net.signum() == 0
            ? BigDecimal.ZERO
            : netClaim.multiply(HUNDRED).divide(net, 2, RoundingMode.HALF_EVEN));
  }

  private static List<ReportColumn> columns() {
    return List.of(
        ReportColumn.text(UwReportSupport.K_POLICY, "Policy No"),
        ReportColumn.text(UwReportSupport.K_INSURED, "Assured"),
        ReportColumn.date("periodFrom", "Period From"),
        ReportColumn.date("periodTo", "Period To"),
        ReportColumn.text("customer", "Customer"),
        ReportColumn.count("claims", "No of Claims"),
        ReportColumn.text("intermediary", "Intermediary"),
        ReportColumn.percent(K_SHARE, "Our Share %"),
        ReportColumn.amount("si", "SI"),
        ReportColumn.amount("gross", "Gross"),
        ReportColumn.amount("discount", "Disc"),
        ReportColumn.amount("loading", "Loading"),
        ReportColumn.amount("net", "Net"),
        ReportColumn.amount("policyFee", "Policy Fee"),
        ReportColumn.amount("commission", "Commission"),
        ReportColumn.amount("fac", "FAC"),
        ReportColumn.amount("netClaim", "Net Claim Amount"),
        ReportColumn.percent("claimRatio", "Claim Ratio %"));
  }
}
