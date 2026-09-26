package com.iortatechnxt.brokerverse.reserves.report;

import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveKey;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveType;
import com.iortatechnxt.brokerverse.reserves.service.ReserveAnalysisService;
import com.iortatechnxt.brokerverse.reserves.service.ValuationCalculator;
import com.iortatechnxt.brokerverse.reserves.service.ValuationResult;
import com.iortatechnxt.brokerverse.reserves.service.ValuationRunService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Parameters and data access shared by the actuarial reports. Processing reports read the valuation
 * run of the month when one exists (the processed figures), otherwise they calculate the reserves
 * live at the date.
 */
@Component
public class ReserveReportSupport {

  /** Company parameter. */
  public static final String COMPANY = "companyId";

  /** Valuation (processed) date parameter. */
  public static final String DATE = "valuationDate";

  /** Branch filter parameter. */
  public static final String BRANCH = "branchId";

  /** Line of business filter parameter. */
  public static final String CLASS = "businessLine";

  /** Branch cell key. */
  public static final String K_BRANCH = "branch";

  /** Line of business cell key. */
  public static final String K_CLASS = "lob";

  /** Channel cell key. */
  public static final String K_SOURCE = "sourceType";

  /** Product cell key. */
  public static final String K_PRODUCT = "product";

  private final OrganizationService organization;
  private final ValuationRunService runs;
  private final ValuationCalculator calculator;

  /**
   * Creates the helper.
   *
   * @param organization branches
   * @param runs valuation runs
   * @param calculator live reserve calculation
   */
  public ReserveReportSupport(
      OrganizationService organization, ValuationRunService runs, ValuationCalculator calculator) {
    this.organization = organization;
    this.runs = runs;
    this.calculator = calculator;
  }

  /**
   * Company, valuation date (default today) and the optional branch / class filters.
   *
   * @param dateLabel label of the date parameter
   * @return specs
   */
  public static List<ParameterSpec> baseParams(String dateLabel) {
    return new ArrayList<>(
        List.of(
            ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(DATE, dateLabel, ParameterType.DATE).withDefault("TODAY"),
            ParameterSpec.optional(BRANCH, "Branch", ParameterType.BRANCH),
            ParameterSpec.optional(CLASS, "Department (Class)", ParameterType.BUSINESS_LINE)));
  }

  /**
   * Branch codes of a company.
   *
   * @param companyId company
   * @return code by branch id
   */
  public Map<Long, String> branchCodes(Long companyId) {
    return organization.listBranches(companyId).stream()
        .collect(Collectors.toMap(Branch::getId, Branch::getCode));
  }

  /**
   * Reserve lines at the valuation date: those of the month's run, else a live calculation.
   *
   * @param p parameters
   * @return lines matching the branch / class filters
   */
  public List<ReserveLineValues> lines(ReportParameters p) {
    Long companyId = p.longValue(COMPANY);
    LocalDate date = p.date(DATE);
    List<ReserveLineValues> lines =
        runs.forMonth(companyId, date)
            .map(r -> ReserveAnalysisService.values(runs.get(r.getId())))
            .orElseGet(() -> live(companyId, date).lines());
    return lines.stream().filter(l -> matches(p, l.key())).toList();
  }

  /**
   * Live calculation at a date.
   *
   * @param companyId company
   * @param date valuation date
   * @return result
   */
  public ValuationResult live(Long companyId, LocalDate date) {
    return calculator.calculate(companyId, date);
  }

  /**
   * Whether a reporting unit passes the branch / class filters.
   *
   * @param p parameters
   * @param key unit
   * @return true when included
   */
  public static boolean matches(ReportParameters p, ReserveKey key) {
    Optional<Long> branch = p.optionalLong(BRANCH);
    Optional<String> line = p.optionalText(CLASS);
    return branch.map(b -> b.equals(key.branchId())).orElse(true)
        && line.map(l -> l.equals(key.businessLine())).orElse(true);
  }

  /**
   * Puts the unit's group cells (branch, class, channel, product) in a row.
   *
   * @param row row
   * @param key unit
   * @param branches branch codes
   */
  public static void putKey(Map<String, Object> row, ReserveKey key, Map<Long, String> branches) {
    row.put(K_BRANCH, branches.getOrDefault(key.branchId(), String.valueOf(key.branchId())));
    row.put(K_CLASS, key.businessLine());
    row.put(K_SOURCE, key.sourceType());
    row.put(K_PRODUCT, key.productCode());
  }

  /**
   * Rows of the lines of one reserve type: one per reporting unit, or per branch and class when
   * summarised (product and channel blank, amounts added).
   *
   * @param lines reserve lines
   * @param type reserve type
   * @param summary true to summarise per branch and class
   * @param branches branch codes
   * @param amounts amount cells of a line
   * @return rows
   */
  public static List<Map<String, Object>> unitRows(
      List<ReserveLineValues> lines,
      ReserveType type,
      boolean summary,
      Map<Long, String> branches,
      Function<ReserveLineValues, Map<String, BigDecimal>> amounts) {
    Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
    lines.stream()
        .filter(l -> l.type() == type)
        .sorted(Comparator.comparing(ReserveLineValues::key, ReserveKey.ORDER))
        .forEach(
            l -> {
              ReserveKey key =
                  summary
                      ? new ReserveKey(l.key().branchId(), l.key().businessLine(), "", "")
                      : l.key();
              Map<String, Object> row =
                  rows.computeIfAbsent(
                      key.toString(),
                      k -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        putKey(r, key, branches);
                        return r;
                      });
              amounts.apply(l).forEach((c, v) -> row.merge(c, v, ReserveReportSupport::add));
            });
    return new ArrayList<>(rows.values());
  }

  /**
   * Adds an amount to a row cell.
   *
   * @param a current value (may be null)
   * @param b amount to add
   * @return sum
   */
  public static Object add(Object a, Object b) {
    BigDecimal x = a == null ? BigDecimal.ZERO : (BigDecimal) a;
    return x.add((BigDecimal) b);
  }
}
