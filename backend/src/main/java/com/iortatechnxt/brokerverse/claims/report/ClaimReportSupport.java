package com.iortatechnxt.brokerverse.claims.report;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimPolicy;
import com.iortatechnxt.brokerverse.claims.service.ClaimFigures;
import com.iortatechnxt.brokerverse.claims.service.ClaimQueryService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.underwriting.report.UwFilters;
import com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport;
import com.iortatechnxt.brokerverse.underwriting.service.PremiumTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Parameters, filters, data access and cells shared by the claims (GI) reports. The Reports Book
 * range parameters (branch, class, product, customer, broker) are those of the underwriting reports
 * ({@link UwReportSupport}); amounts are company share in base currency.
 */
@Component
public class ClaimReportSupport {

  /** Company parameter. */
  public static final String COMPANY = UwReportSupport.COMPANY;

  /** From date parameter. */
  public static final String FROM = UwReportSupport.FROM;

  /** To date parameter. */
  public static final String TO = UwReportSupport.TO;

  /** As-on date parameter. */
  public static final String AS_ON = "asOnDate";

  /** Include-expense option. */
  public static final String INCLUDE_EXPENSE = "includeExpense";

  /** Branch cell key. */
  public static final String K_BRANCH = UwReportSupport.K_BRANCH;

  /** Class cell key. */
  public static final String K_CLASS = UwReportSupport.K_CLASS;

  /** Product cell key. */
  public static final String K_PRODUCT = UwReportSupport.K_PRODUCT;

  /** Underwriting year cell key. */
  public static final String K_UW_YEAR = UwReportSupport.K_UW_YEAR;

  /** Customer cell key. */
  public static final String K_CUSTOMER = "customer";

  /** Claim number cell key. */
  public static final String K_CLAIM = "claimNo";

  /** Policy number cell key. */
  public static final String K_POLICY = UwReportSupport.K_POLICY;

  /** Insured cell key. */
  public static final String K_INSURED = UwReportSupport.K_INSURED;

  /** Loss date cell key. */
  public static final String K_LOSS_DATE = "lossDate";

  /** Branch group label. */
  public static final String BRANCH_LABEL = "Branch";

  /** Class group label. */
  public static final String CLASS_LABEL = "Class";

  /** Product group label. */
  public static final String PRODUCT_LABEL = "Product";

  /** Footnote on amounts. */
  public static final String AMOUNTS_NOTE = "Amounts: company share, base currency.";

  private final ClaimQueryService query;
  private final UwReportSupport uw;

  /**
   * Creates the helper.
   *
   * @param query claim ledger queries
   * @param uw underwriting report support (range filters, branch codes)
   */
  public ClaimReportSupport(ClaimQueryService query, UwReportSupport uw) {
    this.query = query;
    this.uw = uw;
  }

  /**
   * Company and the optional range filters of the Reports Book.
   *
   * @return parameter specs (mutable)
   */
  public static List<ParameterSpec> rangeParams() {
    return UwReportSupport.rangeParams();
  }

  /**
   * Mandatory as-on date, defaulting to today.
   *
   * @return spec
   */
  public static ParameterSpec asOnParam() {
    return ParameterSpec.required(AS_ON, "As on Date", ParameterType.DATE).withDefault("TODAY");
  }

  /**
   * "Include Expense Provision" option (checked by default).
   *
   * @return spec
   */
  public static ParameterSpec includeExpenseParam() {
    return new ParameterSpec(
        INCLUDE_EXPENSE,
        "Include Expense Provision",
        ParameterType.BOOLEAN,
        true,
        List.of(),
        "true");
  }

  /**
   * Claims of the run's company that pass the range filters.
   *
   * @param p parameters
   * @return claims ordered by number
   */
  public List<Claim> claims(ReportParameters p) {
    UwFilters f = UwReportSupport.filters(p);
    return query.claims(p.longValue(COMPANY)).stream()
        .filter(
            c ->
                f.test(
                    c.getBranchId(),
                    c.getPolicy().getBusinessLine(),
                    c.getPolicy().getProductCode(),
                    c.getPolicy().getCustomerCode(),
                    c.getPolicy().getIntermediaryCode()))
        .toList();
  }

  /**
   * Figures of every claim as at a date (all movements up to it).
   *
   * @param companyId company
   * @param asOf date
   * @return figures by claim id
   */
  public Map<Long, ClaimFigures> asOf(Long companyId, LocalDate asOf) {
    return query.baseFigures(companyId, ClaimQueryService.EARLIEST, asOf);
  }

  /**
   * Figures of every claim from movements dated in a period.
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @return figures by claim id
   */
  public Map<Long, ClaimFigures> between(Long companyId, LocalDate from, LocalDate to) {
    return query.baseFigures(companyId, from, to);
  }

  /**
   * Sums claim figures per policy.
   *
   * @param companyId company
   * @param figures figures by claim id
   * @return figures by policy id (policies without claim movements are absent)
   */
  public Map<Long, ClaimFigures> byPolicy(Long companyId, Map<Long, ClaimFigures> figures) {
    Map<Long, ClaimFigures> out = new HashMap<>();
    for (Claim c : query.claims(companyId)) {
      ClaimFigures f = figures.get(c.getId());
      if (f != null) {
        out.merge(c.getPolicy().getPolicyId(), f, ClaimFigures::plus);
      }
    }
    return out;
  }

  /**
   * Branch codes of a company.
   *
   * @param companyId company
   * @return codes by branch id
   */
  public Map<Long, String> branchCodes(Long companyId) {
    return uw.branchCodes(companyId);
  }

  /**
   * Whether a claim was closed, repudiated or withdrawn on or before a date.
   *
   * @param c claim
   * @param date date
   * @return true when finished by then
   */
  public static boolean finishedBy(Claim c, LocalDate date) {
    return c.getStatus().isFinished() && c.getClosedOn() != null && !c.getClosedOn().isAfter(date);
  }

  /**
   * Puts the standard grouping and identification cells of a claim in a row.
   *
   * @param row row
   * @param c claim
   * @param branches branch codes
   */
  public static void putClaim(Map<String, Object> row, Claim c, Map<Long, String> branches) {
    ClaimPolicy p = c.getPolicy();
    row.put(K_BRANCH, branches.getOrDefault(c.getBranchId(), String.valueOf(c.getBranchId())));
    row.put(K_CLASS, p.getBusinessLine());
    row.put(K_PRODUCT, p.getProductCode() + " " + p.getProductName());
    row.put(K_UW_YEAR, String.valueOf(p.getUwYear()));
    row.put(K_CUSTOMER, p.getCustomerCode() + " " + p.getCustomerName());
    row.put(K_CLAIM, c.getClaimNo());
    row.put(K_POLICY, p.getPolicyNo());
    row.put(K_INSURED, p.getInsuredName());
    row.put(K_LOSS_DATE, c.getLoss().getLossDate());
  }

  /**
   * Sums a premium figure of transactions in base currency.
   *
   * @param txns premium transactions
   * @param amount figure of a transaction (policy currency)
   * @return total in base currency
   */
  public static BigDecimal premium(
      Collection<PremiumTransaction> txns, Function<PremiumTransaction, BigDecimal> amount) {
    return txns.stream().map(t -> t.toBase(amount.apply(t))).reduce(Money.zero(), BigDecimal::add);
  }

  /**
   * Ratio in percent, zero when the base is zero.
   *
   * @param amount numerator
   * @param base denominator
   * @return amount / base × 100, two decimals
   */
  public static BigDecimal ratio(BigDecimal amount, BigDecimal base) {
    return base.signum() == 0
        ? BigDecimal.ZERO
        : amount.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_EVEN);
  }
}
