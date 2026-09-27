package com.iortatechnxt.brokerverse.claims.report;

import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_PRODUCT;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.service.ClaimFigures;
import com.iortatechnxt.brokerverse.common.util.Money;
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
import com.iortatechnxt.brokerverse.underwriting.service.PremiumTransaction;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/**
 * PGIBR023 Production / Claims Analysis per product for a period. Premium (our gross, base
 * currency) of transactions approved in the period by kind: New = original issue, Renewal = renewal
 * endorsement, Additional = positive endorsement, Refund = negative endorsement; total gross at 100
 * % and our share. Claims (BrokerVerse rule): claims with a date of loss in the period, as at the
 * period end: estimate (payment side), paid and outstanding. Branch &gt; Class.
 */
@Component
public class ProductionClaimsReport implements ReportDefinition {

  private static final String[] AMOUNTS = {
    "newBusiness",
    "renewal",
    "additional",
    "refund",
    "gross100",
    "grossOur",
    "estimate",
    "paid",
    "os"
  };

  private final PolicyQueryService policies;
  private final ClaimReportSupport support;

  /**
   * Creates the report.
   *
   * @param policies underwriting read API
   * @param support claims report support
   */
  public ProductionClaimsReport(PolicyQueryService policies, ClaimReportSupport support) {
    this.policies = policies;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = ClaimReportSupport.rangeParams();
    params.addAll(UwReportSupport.dateRange("Approval Date From", "Approval Date To"));
    return new ReportMetadata(
        "PGIBR023",
        "Production / Claims Analysis",
        ReportCategory.CLAIMS,
        "Premium production by kind against claims per product",
        params,
        Permission.CLAIM_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ClaimReportSupport.COMPANY);
    LocalDate from = p.date(ClaimReportSupport.FROM);
    LocalDate to = p.date(ClaimReportSupport.TO);
    UwFilters f = UwReportSupport.filters(p);
    Map<Long, String> branches = support.branchCodes(companyId);
    Map<String, Map<String, Object>> rows = new TreeMap<>();
    for (PremiumTransaction t : policies.approvedTransactions(companyId, from, to)) {
      if (f.test(t.policy())) {
        Map<String, Object> row = row(rows, branches.get(t.policy().branchId()), t);
        row.merge(kind(t), t.toBase(t.premium().getOurGrossPremium()), UwReportSupport::add);
        row.merge("gross100", t.toBase(t.premium().getGrossPremium()), UwReportSupport::add);
        row.merge("grossOur", t.toBase(t.premium().getOurGrossPremium()), UwReportSupport::add);
      }
    }
    Map<Long, ClaimFigures> figures = support.asOf(companyId, to);
    for (Claim c : support.claims(p)) {
      LocalDate loss = c.getLoss().getLossDate();
      if (loss.isBefore(from) || loss.isAfter(to)) {
        continue;
      }
      ClaimFigures cf = figures.getOrDefault(c.getId(), ClaimFigures.none());
      Map<String, Object> row = row(rows, branches.get(c.getBranchId()), c);
      row.merge("estimate", cf.paymentEstimate(true), UwReportSupport::add);
      row.merge("paid", cf.paid(true), UwReportSupport::add);
      row.merge("os", cf.paymentOutstanding(true), UwReportSupport::add);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(K_PRODUCT, "Product"),
            ReportColumn.amount("newBusiness", "New"),
            ReportColumn.amount("renewal", "Renewal"),
            ReportColumn.amount("additional", "Additional"),
            ReportColumn.amount("refund", "Refund"),
            ReportColumn.amount("gross100", "Total Gross 100%"),
            ReportColumn.amount("grossOur", "Total Gross Our"),
            ReportColumn.amount("estimate", "Claim Estimate"),
            ReportColumn.amount("paid", "Claim Paid"),
            ReportColumn.amount("os", "Outstanding"))
        .groupBy(K_BRANCH, ClaimReportSupport.BRANCH_LABEL)
        .groupBy(K_CLASS, ClaimReportSupport.CLASS_LABEL)
        .rows(new ArrayList<>(rows.values()))
        .note("Premium: our gross premium by kind; claims with a date of loss in the period.")
        .note(ClaimReportSupport.AMOUNTS_NOTE)
        .build();
  }

  /** Premium column of a transaction kind. */
  private static String kind(PremiumTransaction t) {
    if (PremiumTransaction.NEW.equals(t.kind())) {
      return "newBusiness";
    }
    if (EndorsementType.RENEWAL.name().equals(t.kind())) {
      return "renewal";
    }
    return t.premium().getOurGrossPremium().signum() < 0 ? "refund" : "additional";
  }

  private static Map<String, Object> row(
      Map<String, Map<String, Object>> rows, String branch, PremiumTransaction t) {
    return row(
        rows,
        branch,
        t.policy().businessLine(),
        t.policy().productCode() + " " + t.policy().productName());
  }

  private static Map<String, Object> row(
      Map<String, Map<String, Object>> rows, String branch, Claim c) {
    return row(
        rows,
        branch,
        c.getPolicy().getBusinessLine(),
        c.getPolicy().getProductCode() + " " + c.getPolicy().getProductName());
  }

  private static Map<String, Object> row(
      Map<String, Map<String, Object>> rows, String branch, String lob, String product) {
    return rows.computeIfAbsent(
        branch + "|" + lob + "|" + product,
        k -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put(K_BRANCH, branch);
          m.put(K_CLASS, lob);
          m.put(K_PRODUCT, product);
          for (String key : AMOUNTS) {
            m.put(key, Money.zero());
          }
          return m;
        });
  }
}
