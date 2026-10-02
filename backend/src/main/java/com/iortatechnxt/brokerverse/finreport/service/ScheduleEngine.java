package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleColumn;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinition;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Measure;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Side;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment;
import com.iortatechnxt.brokerverse.finreport.domain.StatementCommentRepository;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.AccountRef;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.Increase;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.RowFigures;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.Selection;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.Window;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.subledger.service.AgeingSlots;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns an account schedule definition into rows (FRBS 3.2.0, Appendix A II-IV; design section 10):
 * the selected accounts are read from the posted ledger, grouped by account, party, document, cost
 * centre, branch or business line, with the figures of the definition on its side, the comparative
 * and variance, the FIFO ageing buckets and the month's commentary.
 */
@Service
@Transactional(readOnly = true)
public class ScheduleEngine {

  /** Row key column. */
  public static final String CODE = "code";

  /** Row name column. */
  public static final String NAME = "name";

  /** Commentary column. */
  public static final String COMMENT = "comment";

  private final ScheduleQueries queries;
  private final ScheduleLabels labels;
  private final StatementCommentRepository comments;

  /**
   * Creates the engine.
   *
   * @param queries ledger reads
   * @param labels row labels
   * @param comments commentary
   */
  public ScheduleEngine(
      ScheduleQueries queries, ScheduleLabels labels, StatementCommentRepository comments) {
    this.queries = queries;
    this.labels = labels;
    this.comments = comments;
  }

  /**
   * Runs a schedule.
   *
   * @param def definition
   * @param request company, period and branch
   * @return columns, rows and notes
   */
  public ScheduleOutput run(ScheduleDefinition def, ScheduleRequest request) {
    LocalDate asOf = request.asOf();
    LocalDate from = from(def, request);
    List<AccountRef> accounts =
        queries.accounts(
            request.companyId(), def.values().selectorKind(), def.values().selectorEntries());
    AgeingSlots slots =
        def.isAgeing() ? AgeingSlots.parse(def.getAgeingSlots(), AgeingSlots.STANDARD) : null;
    List<ReportColumn> columns = ScheduleLayout.columns(def, slots);
    List<String> notes = ScheduleLayout.notes(def, accounts, slots);
    if (accounts.isEmpty()) {
      return new ScheduleOutput(columns, List.of(), notes);
    }
    Selection selection =
        new Selection(
            request.companyId(),
            def.getGrouping(),
            accounts.stream().map(AccountRef::id).toList(),
            def.getCurrency(),
            request.branchId());
    Window window =
        ScheduleMath.window(
            from,
            asOf,
            queries.yearStartMonth(request.companyId()),
            def.getBasis(),
            def.getComparative());
    List<RowFigures> figures = queries.figures(selection, window);
    RowContext ctx =
        new RowContext(
            def,
            asOf,
            slots,
            slots == null ? Map.of() : increases(selection, asOf, def.getSide()),
            labels.labels(def.getGrouping(), request.companyId(), accounts, figures),
            def.isCommentary() ? comments(request.companyId(), def.getCode(), asOf) : Map.of());
    List<Map<String, Object>> rows = new ArrayList<>();
    for (RowFigures f : figures) {
      Map<Measure, BigDecimal> m = ScheduleMath.measures(f, def.getSide(), def.getBasis());
      if (!isEmpty(def, m)) {
        rows.add(row(ctx, f.key(), m));
      }
    }
    rows.sort(Comparator.comparing(r -> String.valueOf(r.get(CODE))));
    return new ScheduleOutput(columns, rows, notes);
  }

  private static LocalDate from(ScheduleDefinition def, ScheduleRequest request) {
    LocalDate from = request.from() == null ? request.asOf().withDayOfMonth(1) : request.from();
    if (from.isAfter(request.asOf())) {
      throw new BusinessRuleException(
          "SCHEDULE_DATES", "The period of schedule " + def.getCode() + " starts after its end");
    }
    return from;
  }

  private Map<String, List<Increase>> increases(Selection selection, LocalDate asOf, Side side) {
    return queries.increases(selection, asOf, side == Side.DEBIT).stream()
        .collect(Collectors.groupingBy(Increase::key, LinkedHashMap::new, Collectors.toList()));
  }

  private static Map<String, Object> row(RowContext ctx, String key, Map<Measure, BigDecimal> m) {
    String[] label = ctx.labels().getOrDefault(key, new String[] {key, ""});
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(CODE, label[0]);
    row.put(NAME, label[1]);
    ctx.def().getColumns().forEach(c -> row.put(key(c), m.get(c.measure())));
    if (ctx.slots() != null) {
      List<BigDecimal> buckets =
          ScheduleMath.fifo(
              m.get(Measure.CLOSING),
              ctx.increases().getOrDefault(key, List.of()),
              ctx.asOf(),
              ctx.slots());
      for (int i = 0; i < buckets.size(); i++) {
        row.put(ScheduleLayout.AGE_PREFIX + i, buckets.get(i));
      }
    }
    if (ctx.def().isCommentary()) {
      row.put(COMMENT, ctx.commentary().getOrDefault(label[0], ""));
    }
    return row;
  }

  /** What every row of a run needs. */
  private record RowContext(
      ScheduleDefinition def,
      LocalDate asOf,
      AgeingSlots slots,
      Map<String, List<Increase>> increases,
      Map<String, String[]> labels,
      Map<String, String> commentary) {}

  /**
   * The key of the column of a figure.
   *
   * @param column column
   * @return key
   */
  public static String key(ScheduleColumn column) {
    return column.measure().name().toLowerCase(Locale.ROOT);
  }

  private static boolean isEmpty(ScheduleDefinition def, Map<Measure, BigDecimal> m) {
    return def.getColumns().stream()
        .map(c -> m.get(c.measure()))
        .allMatch(v -> v == null || v.signum() == 0);
  }

  private Map<String, String> comments(Long companyId, String code, LocalDate asOf) {
    return comments
        .findByCompanyIdAndScheduleCodeAndPeriod(companyId, code, YearMonth.from(asOf).toString())
        .stream()
        .collect(Collectors.toMap(StatementComment::getRowKey, StatementComment::getText));
  }

  /**
   * A schedule run.
   *
   * @param companyId company
   * @param from first day of the period, null for the first day of the as-of month
   * @param asOf as-of date
   * @param branchId branch, null for all
   */
  public record ScheduleRequest(Long companyId, LocalDate from, LocalDate asOf, Long branchId) {}

  /**
   * What a run produced.
   *
   * @param columns columns
   * @param rows rows (column key to value)
   * @param notes notes
   */
  public record ScheduleOutput(
      List<ReportColumn> columns, List<Map<String, Object>> rows, List<String> notes) {}
}
