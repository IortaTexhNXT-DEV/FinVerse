package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleColumn;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinition;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Grouping;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.LayoutStatus;
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

  private static final String AGE_PREFIX = "age";
  private static final String NONE = "(none)";
  private static final String DOCUMENT_SEPARATOR = "|";

  private final ScheduleQueries queries;
  private final FinReportQueries names;
  private final StatementCommentRepository comments;

  /**
   * Creates the engine.
   *
   * @param queries ledger reads
   * @param names party and dimension names
   * @param comments commentary
   */
  public ScheduleEngine(
      ScheduleQueries queries, FinReportQueries names, StatementCommentRepository comments) {
    this.queries = queries;
    this.names = names;
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
    LocalDate from = request.from() == null ? asOf.withDayOfMonth(1) : request.from();
    if (from.isAfter(asOf)) {
      throw new BusinessRuleException(
          "SCHEDULE_DATES", "The period of schedule " + def.getCode() + " starts after its end");
    }
    List<AccountRef> accounts =
        queries.accounts(
            request.companyId(), def.values().selectorKind(), def.values().selectorEntries());
    AgeingSlots slots =
        def.isAgeing() ? AgeingSlots.parse(def.getAgeingSlots(), AgeingSlots.STANDARD) : null;
    List<ReportColumn> columns = columns(def, slots);
    List<String> notes = notes(def, accounts, slots);
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
    Map<String, List<Increase>> increases =
        slots == null
            ? Map.of()
            : queries.increases(selection, asOf, def.getSide() == Side.DEBIT).stream()
                .collect(
                    Collectors.groupingBy(Increase::key, LinkedHashMap::new, Collectors.toList()));
    Map<String, String[]> labels =
        labels(def.getGrouping(), request.companyId(), accounts, figures);
    Map<String, String> commentary =
        def.isCommentary() ? comments(request.companyId(), def.getCode(), asOf) : Map.of();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (RowFigures f : figures) {
      Map<Measure, BigDecimal> m = ScheduleMath.measures(f, def.getSide(), def.getBasis());
      if (isEmpty(def, m)) {
        continue;
      }
      String[] label = labels.getOrDefault(f.key(), new String[] {f.key(), ""});
      Map<String, Object> row = new LinkedHashMap<>();
      row.put(CODE, label[0]);
      row.put(NAME, label[1]);
      def.getColumns().forEach(c -> row.put(key(c), m.get(c.measure())));
      if (slots != null) {
        List<BigDecimal> buckets =
            ScheduleMath.fifo(
                m.get(Measure.CLOSING), increases.getOrDefault(f.key(), List.of()), asOf, slots);
        for (int i = 0; i < buckets.size(); i++) {
          row.put(AGE_PREFIX + i, buckets.get(i));
        }
      }
      if (def.isCommentary()) {
        row.put(COMMENT, commentary.getOrDefault(label[0], ""));
      }
      rows.add(row);
    }
    rows.sort(Comparator.comparing(r -> String.valueOf(r.get(CODE))));
    return new ScheduleOutput(columns, rows, notes);
  }

  /**
   * The key of the column of a figure.
   *
   * @param column column
   * @return key
   */
  public static String key(ScheduleColumn column) {
    return column.measure().name().toLowerCase(java.util.Locale.ROOT);
  }

  private static boolean isEmpty(ScheduleDefinition def, Map<Measure, BigDecimal> m) {
    return def.getColumns().stream()
        .map(c -> m.get(c.measure()))
        .allMatch(v -> v == null || v.signum() == 0);
  }

  private static List<ReportColumn> columns(ScheduleDefinition def, AgeingSlots slots) {
    String[] headings = headings(def.getGrouping());
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text(CODE, headings[0]));
    columns.add(ReportColumn.text(NAME, headings[1]));
    for (ScheduleColumn c : def.getColumns()) {
      columns.add(
          c.measure() == Measure.VARIANCE_PCT
              ? ReportColumn.percent(key(c), c.label())
              : ReportColumn.amount(key(c), c.label()));
    }
    if (slots != null) {
      List<String> ages = slots.labels();
      for (int i = 0; i < ages.size(); i++) {
        columns.add(ReportColumn.amount(AGE_PREFIX + i, ages.get(i) + " days"));
      }
    }
    if (def.isCommentary()) {
      columns.add(ReportColumn.text(COMMENT, "Commentary"));
    }
    return columns;
  }

  private static String[] headings(Grouping grouping) {
    return switch (grouping) {
      case ACCOUNT -> new String[] {"Account", "Account name"};
      case PARTY -> new String[] {"Party", "Party name"};
      case DOCUMENT -> new String[] {"Party", "Document"};
      case COST_CENTER -> new String[] {"Cost centre", "Cost centre name"};
      case BRANCH -> new String[] {"Branch", "Branch name"};
      case BUSINESS_LINE -> new String[] {"Line of business", "Name"};
    };
  }

  private static List<String> notes(
      ScheduleDefinition def, List<AccountRef> accounts, AgeingSlots slots) {
    List<String> notes = new ArrayList<>();
    notes.add(
        accounts.isEmpty()
            ? "No postable account matches " + def.values().accountSelector()
            : "Accounts: "
                + accounts.stream().map(AccountRef::code).collect(Collectors.joining(", ")));
    notes.add(
        def.getCurrency() == null
            ? "Amounts in base currency, every transaction currency"
            : "Amounts in "
                + def.getCurrency()
                + ", transactions in "
                + def.getCurrency()
                + " only");
    if (slots != null) {
      notes.add(
          "Ageing "
              + slots.describe()
              + " days, first in first out: the balance is made of the most recent increases");
    }
    if (def.getLayoutStatus() == LayoutStatus.TO_CONFIRM) {
      notes.add("Draft layout, to be confirmed with FRBS (AQ05)");
    }
    return notes;
  }

  private Map<String, String[]> labels(
      Grouping grouping, Long companyId, List<AccountRef> accounts, List<RowFigures> figures) {
    Map<String, String[]> labels = new LinkedHashMap<>();
    switch (grouping) {
      case ACCOUNT ->
          accounts.forEach(
              a -> labels.put(String.valueOf(a.id()), new String[] {a.code(), a.name()}));
      case BRANCH -> labels.putAll(queries.branchNames(companyId));
      case PARTY, DOCUMENT -> partyLabels(grouping, companyId, figures, labels);
      case COST_CENTER, BUSINESS_LINE -> {
        Map<String, String> dims = names.dimensionNames(companyId, grouping.name());
        figures.forEach(
            f ->
                labels.put(
                    f.key(), new String[] {orNone(f.key()), dims.getOrDefault(f.key(), "")}));
      }
    }
    return labels;
  }

  private void partyLabels(
      Grouping grouping, Long companyId, List<RowFigures> figures, Map<String, String[]> labels) {
    List<String> parties =
        figures.stream().map(f -> party(f.key())).filter(p -> !p.isEmpty()).distinct().toList();
    Map<String, String> partyNames = names.partyNames(companyId, parties);
    for (RowFigures f : figures) {
      String party = party(f.key());
      if (grouping == Grouping.PARTY) {
        labels.put(f.key(), new String[] {orNone(party), partyNames.getOrDefault(party, "")});
      } else {
        String document = f.key().substring(f.key().indexOf(DOCUMENT_SEPARATOR) + 1);
        labels.put(f.key(), new String[] {orNone(party), orNone(document)});
      }
    }
  }

  private static String party(String key) {
    int bar = key.indexOf(DOCUMENT_SEPARATOR);
    return bar < 0 ? key : key.substring(0, bar);
  }

  private static String orNone(String value) {
    return value == null || value.isEmpty() ? NONE : value;
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
