package com.iortatechnxt.finverse.reinsurance.report;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.reinsurance.domain.Cession;
import com.iortatechnxt.finverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.finverse.reinsurance.domain.FacPlacementRepository;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Parameters, filters and helpers shared by the reinsurance reports. Range parameters (branch,
 * class) are optional filters: blank = all. Amounts are reported in the company base currency.
 */
@Component
public class RiReportSupport {

  /** Company parameter. */
  public static final String COMPANY = "companyId";

  /** Branch parameter. */
  public static final String BRANCH = "branchId";

  /** Class (line of business) parameter. */
  public static final String CLASS = "businessLine";

  /** From date parameter. */
  public static final String FROM = "fromDate";

  /** To date parameter. */
  public static final String TO = "toDate";

  /** Allocation status parameter. */
  public static final String STATUS = "allocationStatus";

  /** Allocation status: every row. */
  public static final String ALL = "ALL";

  /** Allocation status: fully allocated (no facultative requirement pending). */
  public static final String ALLOCATED = "ALLOCATED";

  /** Allocation status: facultative requirement still provisional (or no allocation). */
  public static final String PROVISIONAL = "PROVISIONAL";

  /** Group cell: underwriting year. */
  public static final String K_UW_YEAR = "uwYear";

  /** Group cell: branch. */
  public static final String K_BRANCH = "branch";

  /** Group cell: class. */
  public static final String K_CLASS = "lob";

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final String YEAR_START = "YEAR_START";
  private static final String TODAY = "TODAY";

  private final OrganizationService organization;
  private final FacPlacementRepository placements;

  /**
   * Creates the helper.
   *
   * @param organization branches
   * @param placements facultative placements
   */
  public RiReportSupport(OrganizationService organization, FacPlacementRepository placements) {
    this.organization = organization;
    this.placements = placements;
  }

  /**
   * Company, branch and class parameters.
   *
   * @return specs (mutable list)
   */
  public static List<ParameterSpec> rangeParams() {
    return new ArrayList<>(
        List.of(
            ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.optional(BRANCH, "Branch", ParameterType.BRANCH),
            ParameterSpec.optional(CLASS, "Department (Class)", ParameterType.BUSINESS_LINE)));
  }

  /**
   * Mandatory date range defaulting to the year to date.
   *
   * @param fromLabel from label
   * @param toLabel to label
   * @return specs
   */
  public static List<ParameterSpec> dateRange(String fromLabel, String toLabel) {
    return List.of(
        ParameterSpec.required(FROM, fromLabel, ParameterType.DATE).withDefault(YEAR_START),
        ParameterSpec.required(TO, toLabel, ParameterType.DATE).withDefault(TODAY));
  }

  /**
   * Allocation status option.
   *
   * @return spec
   */
  public static ParameterSpec statusParam() {
    return ParameterSpec.select(
        STATUS, "Allocation Status", List.of(ALL, ALLOCATED, PROVISIONAL), ALL);
  }

  /**
   * Whether a record passes the branch and class filters.
   *
   * @param p parameters
   * @param branchId record branch
   * @param lob record line of business
   * @return true when it passes
   */
  public static boolean inRange(ReportParameters p, Long branchId, String lob) {
    boolean branchOk = p.optionalLong(BRANCH).map(b -> b.equals(branchId)).orElse(true);
    return branchOk && p.optionalText(CLASS).map(c -> c.equals(lob)).orElse(true);
  }

  /**
   * Branch codes of a company.
   *
   * @param companyId company
   * @return codes by branch id
   */
  public Map<Long, String> branchCodes(Long companyId) {
    return organization.listBranches(companyId).stream()
        .collect(Collectors.toMap(Branch::getId, Branch::getCode, (a, b) -> a));
  }

  /**
   * Facultative placements of cessions, by id.
   *
   * @param cessions cessions
   * @return placements by id
   */
  public Map<Long, FacPlacement> placementsOf(Collection<Cession> cessions) {
    List<Long> ids = cessions.stream().map(Cession::getId).toList();
    return ids.isEmpty()
        ? Map.of()
        : placements.findByCessionIdIn(ids).stream()
            .collect(Collectors.toMap(FacPlacement::getId, Function.identity()));
  }

  /**
   * Puts the grouping cells.
   *
   * @param row row
   * @param uwYear underwriting year
   * @param branch branch code
   * @param lob line of business
   */
  public static void putGroups(Map<String, Object> row, Object uwYear, String branch, String lob) {
    row.put(K_UW_YEAR, String.valueOf(uwYear));
    row.put(K_BRANCH, branch);
    row.put(K_CLASS, lob);
  }

  /**
   * A part as a percentage of a whole.
   *
   * @param part part
   * @param whole whole
   * @return part / whole x 100, zero when the whole is zero
   */
  public static BigDecimal pct(BigDecimal part, BigDecimal whole) {
    return whole.signum() == 0
        ? BigDecimal.ZERO.setScale(2)
        : part.multiply(HUNDRED).divide(whole, Money.SCALE, RoundingMode.HALF_EVEN);
  }

  /**
   * Scales a company-share amount back to 100 %.
   *
   * @param ours company share amount
   * @param sharePct company share %
   * @return amount at 100 %
   */
  public static BigDecimal full(BigDecimal ours, BigDecimal sharePct) {
    return sharePct.signum() == 0
        ? ours
        : ours.multiply(HUNDRED).divide(sharePct, Money.SCALE, RoundingMode.HALF_EVEN);
  }
}
