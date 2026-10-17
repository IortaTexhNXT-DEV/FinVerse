package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.csf.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseStore;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseStore.Filter;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseViews.CaseRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The figures of the Case Dashboard and of the case reports (FR-CSF-059): one computation over the
 * same cases, so the dashboard and the Case Summary show the same volume and SLA compliance rate.
 */
@Service
@Transactional(readOnly = true)
public class CsfCaseStatistics {

  private static final int MAX_CASES = 20_000;
  private static final int OPEN_SHOWN = 50;
  private static final int FIRST_BAND = 1;
  private static final int SECOND_BAND = 3;
  private static final int THIRD_BAND = 6;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final CsfCaseStore cases;
  private final CsfCaseViews views;

  /**
   * Creates the statistics.
   *
   * @param cases cases
   * @param views SLA and labels
   */
  public CsfCaseStatistics(CsfCaseStore cases, CsfCaseViews views) {
    this.cases = cases;
    this.views = views;
  }

  /**
   * The filters of the dashboard and the reports; null values are not filtered on.
   *
   * @param companyId company
   * @param from first creation day
   * @param to last creation day
   * @param caseType case type
   * @param priority priority
   * @param status status
   * @param marketSegment market segment
   * @param fulfilmentUnit fulfilment unit
   */
  public record Criteria(
      Long companyId,
      LocalDate from,
      LocalDate to,
      String caseType,
      String priority,
      String status,
      String marketSegment,
      String fulfilmentUnit) {}

  /**
   * A count by value.
   *
   * @param code code
   * @param label label
   * @param count cases
   */
  public record Count(String code, String label, long count) {}

  /**
   * The dashboard figures.
   *
   * @param created cases created in the period
   * @param closed cases of the period closed
   * @param open cases of the period still open
   * @param averageAgeClosed average age of the closed cases in working days
   * @param slaCompliance closed within TAT / closed, in percent (null without closed cases)
   * @param breached breached cases
   * @param notBreached cases within TAT or approaching
   * @param escalated escalated open cases
   * @param byStatus by status
   * @param byType by case type
   * @param byNature by nature of transaction
   * @param byUnit by fulfilment unit
   * @param ageing open cases by ageing band (0-1, 2-3, 4-6, over 6 working days)
   * @param openCases open cases with their SLA indicators, earliest due first
   */
  public record Dashboard(
      long created,
      long closed,
      long open,
      BigDecimal averageAgeClosed,
      BigDecimal slaCompliance,
      long breached,
      long notBreached,
      long escalated,
      List<Count> byStatus,
      List<Count> byType,
      List<Count> byNature,
      List<Count> byUnit,
      List<Count> ageing,
      List<CaseRow> openCases) {}

  /**
   * The rows of the cases matching the criteria, oldest first.
   *
   * @param k criteria
   * @return rows
   */
  public List<CaseRow> rows(Criteria k) {
    List<CsfCase> matching =
        cases
            .list(
                new Filter(
                    k.companyId(),
                    EnumSet.allOf(CaseStatus.class),
                    null,
                    k.fulfilmentUnit() == null ? null : List.of(k.fulfilmentUnit()),
                    k.from(),
                    k.to(),
                    false),
                MAX_CASES)
            .stream()
            .filter(c -> matches(k.caseType(), c.category().caseType()))
            .filter(c -> matches(k.priority(), c.category().priority()))
            .filter(c -> matches(k.status(), c.status().name()))
            .filter(c -> matches(k.marketSegment(), c.category().marketSegment()))
            .toList();
    List<CaseRow> rows = new ArrayList<>(views.rows(k.companyId(), matching));
    rows.sort(Comparator.comparing(CaseRow::createdAt).thenComparing(CaseRow::id));
    return rows;
  }

  /**
   * The dashboard of the criteria.
   *
   * @param k criteria
   * @return figures
   */
  public Dashboard dashboard(Criteria k) {
    List<CaseRow> rows = rows(k);
    Predicate<CaseRow> isClosed = r -> CaseStatus.valueOf(r.status()).closed();
    List<CaseRow> closed = rows.stream().filter(isClosed).toList();
    List<CaseRow> open = rows.stream().filter(isClosed.negate()).toList();
    long closedInTat =
        closed.stream().filter(r -> !CsfCaseCodes.BREACHED.equals(r.sla().status())).count();
    long breached =
        rows.stream().filter(r -> CsfCaseCodes.BREACHED.equals(r.sla().status())).count();
    return new Dashboard(
        rows.size(),
        closed.size(),
        open.size(),
        closed.isEmpty()
            ? null
            : BigDecimal.valueOf(closed.stream().mapToInt(r -> r.sla().ageDays()).sum())
                .divide(BigDecimal.valueOf(closed.size()), 1, RoundingMode.HALF_UP),
        closed.isEmpty()
            ? null
            : BigDecimal.valueOf(closedInTat)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(closed.size()), 1, RoundingMode.HALF_UP),
        breached,
        rows.size() - breached,
        open.stream().filter(r -> r.escalated() != null).count(),
        count(rows, r -> r.status(), r -> r.statusLabel()),
        count(rows, r -> r.caseType().code(), r -> r.caseType().label()),
        count(rows, r -> r.nature().code(), r -> r.nature().label()),
        count(rows, r -> r.fulfilmentUnit().code(), r -> r.fulfilmentUnit().label()),
        ageing(open),
        open.stream()
            .sorted(Comparator.comparing(CaseRow::dueDate).thenComparing(CaseRow::id))
            .limit(OPEN_SHOWN)
            .toList());
  }

  /**
   * The ageing band of an age.
   *
   * @param age working days
   * @return band label
   */
  public static String band(int age) {
    if (age <= FIRST_BAND) {
      return "0-1";
    }
    if (age <= SECOND_BAND) {
      return "2-3";
    }
    return age <= THIRD_BAND ? "4-6" : "Over 6";
  }

  private static List<Count> ageing(List<CaseRow> open) {
    Map<String, Long> bands = new LinkedHashMap<>();
    List.of("0-1", "2-3", "4-6", "Over 6").forEach(b -> bands.put(b, 0L));
    open.forEach(r -> bands.merge(band(r.sla().ageDays()), 1L, Long::sum));
    return bands.entrySet().stream()
        .map(e -> new Count(e.getKey(), e.getKey() + " working days", e.getValue()))
        .toList();
  }

  private static List<Count> count(
      List<CaseRow> rows, Function<CaseRow, String> code, Function<CaseRow, String> label) {
    Map<String, Count> counts = new LinkedHashMap<>();
    for (CaseRow r : rows) {
      String c = code.apply(r);
      Count before = counts.get(c);
      counts.put(c, new Count(c, label.apply(r), before == null ? 1 : before.count() + 1));
    }
    return counts.values().stream()
        .sorted(Comparator.comparingLong(Count::count).reversed().thenComparing(Count::label))
        .toList();
  }

  private static boolean matches(String wanted, String value) {
    return wanted == null || wanted.isBlank() || Objects.equals(wanted, value);
  }
}
