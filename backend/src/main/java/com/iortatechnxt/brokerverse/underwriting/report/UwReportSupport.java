package com.iortatechnxt.brokerverse.underwriting.report;

import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.underwriting.domain.DateBasis;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.brokerverse.underwriting.service.OpenCoverService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.brokerverse.underwriting.service.PremiumTransaction;
import com.iortatechnxt.brokerverse.underwriting.service.TransactionQuery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Parameters, filters and data access shared by the underwriting (GI) reports. Range parameters of
 * the Reports Book (branch, class, product, customer, broker) are optional filters: blank = all.
 */
@Component
public class UwReportSupport {

  /** Company parameter. */
  public static final String COMPANY = "companyId";

  /** Branch parameter. */
  public static final String BRANCH = "branchId";

  /** Class (line of business) parameter. */
  public static final String CLASS = "businessLine";

  /** Product code parameter. */
  public static final String PRODUCT = "productCode";

  /** Customer code parameter. */
  public static final String CUSTOMER = "customerCode";

  /** Broker / intermediary code parameter. */
  public static final String BROKER = "brokerCode";

  /** From date parameter. */
  public static final String FROM = "fromDate";

  /** To date parameter. */
  public static final String TO = "toDate";

  /** Date basis parameter. */
  public static final String BASIS = "basedOn";

  /** Approved documents (including those of policies cancelled later). */
  public static final Set<PolicyStatus> APPROVED =
      EnumSet.of(PolicyStatus.APPROVED, PolicyStatus.CANCELLED);

  /** Common group / cell keys. */
  public static final String K_BRANCH = "branch";

  /** Class (line of business) cell key. */
  public static final String K_CLASS = "lob";

  /** Product cell key. */
  public static final String K_PRODUCT = "product";

  /** Underwriting year cell key. */
  public static final String K_UW_YEAR = "uwYear";

  /** Policy number cell key. */
  public static final String K_POLICY = "policyNo";

  /** Endorsement number cell key. */
  public static final String K_ENDT = "endtNo";

  /** Insured cell key. */
  public static final String K_INSURED = "insured";

  /** Share % cell key. */
  public static final String K_SHARE = "sharePct";

  private static final String YEAR_START = "YEAR_START";
  private static final String TODAY = "TODAY";

  private final PolicyQueryService policies;
  private final OpenCoverService openCovers;
  private final OrganizationService organization;

  /**
   * Creates the helper.
   *
   * @param policies policy read service
   * @param openCovers open covers and certificates
   * @param organization branches
   */
  public UwReportSupport(
      PolicyQueryService policies, OpenCoverService openCovers, OrganizationService organization) {
    this.policies = policies;
    this.openCovers = openCovers;
    this.organization = organization;
  }

  /**
   * Approved (or since cancelled) marine certificates, with risks and open cover loaded.
   *
   * @param companyId company
   * @param openCoverNo open cover number, null for all covers
   * @return certificates
   */
  public List<Policy> approvedCertificates(Long companyId, String openCoverNo) {
    return openCovers.list(companyId).stream()
        .filter(c -> openCoverNo == null || c.getOpenCoverNo().equals(openCoverNo))
        .flatMap(c -> openCovers.certificates(c.getId()).stream())
        .filter(c -> c.getWorkflow().getApprovalDate() != null)
        .toList();
  }

  /**
   * Company and the optional range filters of the Reports Book.
   *
   * @return parameter specs
   */
  public static List<ParameterSpec> rangeParams() {
    return new ArrayList<>(
        List.of(
            ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.optional(BRANCH, "Branch", ParameterType.BRANCH),
            ParameterSpec.optional(CLASS, "Department (Class)", ParameterType.BUSINESS_LINE),
            ParameterSpec.optional(PRODUCT, "Product Code", ParameterType.TEXT),
            ParameterSpec.optional(CUSTOMER, "Customer Code", ParameterType.TEXT),
            ParameterSpec.optional(BROKER, "Broker / Agent Code", ParameterType.TEXT)));
  }

  /**
   * Mandatory date range, defaulting to the year to date.
   *
   * @param fromLabel label of the from date
   * @param toLabel label of the to date
   * @return specs
   */
  public static List<ParameterSpec> dateRange(String fromLabel, String toLabel) {
    return List.of(
        ParameterSpec.required(FROM, fromLabel, ParameterType.DATE).withDefault(YEAR_START),
        ParameterSpec.required(TO, toLabel, ParameterType.DATE).withDefault(TODAY));
  }

  /**
   * "Based on" option (Issue / Accounting / Approval / Period-from).
   *
   * @return spec
   */
  public static ParameterSpec basisParam() {
    return ParameterSpec.select(
        BASIS, "Based on", List.of("ISSUE", "ACCOUNTING", "APPROVAL", "PERIOD_FROM"), "APPROVAL");
  }

  /**
   * Resolves the "Based on" option (accounting date = approval date in BrokerVerse).
   *
   * @param p parameters
   * @return date basis
   */
  public static DateBasis basis(ReportParameters p) {
    return switch (p.optionalText(BASIS).orElse("APPROVAL")) {
      case "ISSUE" -> DateBasis.ISSUE;
      case "PERIOD_FROM" -> DateBasis.PERIOD_FROM;
      default -> DateBasis.APPROVAL;
    };
  }

  /**
   * Range filters of a run.
   *
   * @param p parameters
   * @return filters
   */
  public static UwFilters filters(ReportParameters p) {
    return new UwFilters(
        p.optionalLong(BRANCH).orElse(null),
        p.optionalText(CLASS).orElse(null),
        p.optionalText(PRODUCT).orElse(null),
        p.optionalText(CUSTOMER).orElse(null),
        p.optionalText(BROKER).orElse(null));
  }

  /**
   * Premium transactions of a run, filtered by the range parameters.
   *
   * @param p parameters (company, dates, ranges)
   * @param basis date basis
   * @param statuses statuses
   * @return transactions
   */
  public List<PremiumTransaction> transactions(
      ReportParameters p, DateBasis basis, Set<PolicyStatus> statuses) {
    UwFilters f = filters(p);
    return policies
        .transactions(
            new TransactionQuery(
                p.longValue(COMPANY),
                basis,
                p.optionalDate(FROM).orElse(null),
                p.optionalDate(TO).orElse(null),
                statuses))
        .stream()
        .filter(t -> f.test(t.policy()))
        .toList();
  }

  /**
   * Branch codes of a company by id.
   *
   * @param companyId company
   * @return codes
   */
  public Map<Long, String> branchCodes(Long companyId) {
    return organization.listBranches(companyId).stream()
        .collect(Collectors.toMap(Branch::getId, Branch::getCode, (a, b) -> a));
  }

  /**
   * Puts the standard grouping cells (branch, class, product, UW year) of a policy in a row; the UW
   * year is the policy's (original issue).
   *
   * @param row row
   * @param policy policy
   * @param branches branch codes
   */
  public static void putGroups(
      Map<String, Object> row, PolicySnapshot policy, Map<Long, String> branches) {
    putGroups(row, policy, policy.uwYear(), branches);
  }

  /**
   * Puts the standard grouping cells of a premium transaction in a row; the UW year is the
   * transaction's own (a renewal and the later endorsements of the renewed period belong to the
   * year the renewed period starts).
   *
   * @param row row
   * @param transaction premium transaction
   * @param branches branch codes
   */
  public static void putGroups(
      Map<String, Object> row, PremiumTransaction transaction, Map<Long, String> branches) {
    putGroups(row, transaction.policy(), transaction.uwYear(), branches);
  }

  /**
   * Puts the standard grouping cells of a policy in a row with a given UW year.
   *
   * @param row row
   * @param policy policy
   * @param uwYear underwriting year to group by
   * @param branches branch codes
   */
  public static void putGroups(
      Map<String, Object> row, PolicySnapshot policy, int uwYear, Map<Long, String> branches) {
    row.put(K_BRANCH, branches.getOrDefault(policy.branchId(), String.valueOf(policy.branchId())));
    row.put(K_CLASS, policy.businessLine());
    row.put(K_PRODUCT, policy.productCode() + " " + policy.productName());
    row.put(K_UW_YEAR, String.valueOf(uwYear));
  }

  /**
   * Standard identification columns of a premium transaction.
   *
   * @return policy, endorsement and insured columns
   */
  public static List<ReportColumn> documentColumns() {
    return List.of(
        ReportColumn.text(K_POLICY, "Policy No"),
        ReportColumn.text(K_ENDT, "Endt No"),
        ReportColumn.text(K_INSURED, "Insured / Assured"));
  }

  /**
   * Puts the identification cells of a transaction.
   *
   * @param row row
   * @param t transaction
   */
  public static void putDocument(Map<String, Object> row, PremiumTransaction t) {
    row.put(K_POLICY, t.policy().policyNo());
    row.put(K_ENDT, t.endorsementNo() == 0 ? "" : String.valueOf(t.endorsementNo()));
    row.put(K_INSURED, t.policy().insuredName());
  }

  /**
   * Adds two decimal cell values (merge function for aggregated rows).
   *
   * @param a first value (BigDecimal)
   * @param b second value (BigDecimal)
   * @return sum
   */
  public static Object add(Object a, Object b) {
    return ((BigDecimal) a).add((BigDecimal) b);
  }

  /**
   * Indexes elements by a key.
   *
   * @param items items
   * @param key key extractor
   * @param <K> key type
   * @param <V> item type
   * @return map (first wins)
   */
  public static <K, V> Map<K, V> index(List<V> items, Function<V, K> key) {
    return items.stream().collect(Collectors.toMap(key, Function.identity(), (a, b) -> a));
  }
}
