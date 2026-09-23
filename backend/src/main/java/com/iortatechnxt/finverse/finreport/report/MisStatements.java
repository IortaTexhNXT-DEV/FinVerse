package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.AccountNode;
import com.iortatechnxt.finverse.finreport.service.FormatLine;
import com.iortatechnxt.finverse.finreport.service.StatementFormat;
import com.iortatechnxt.finverse.finreport.service.StatementFormat.Evaluation;
import com.iortatechnxt.finverse.finreport.service.StatementFormat.Rounding;
import com.iortatechnxt.finverse.finreport.service.StatementFormatService;
import com.iortatechnxt.finverse.ledger.service.AccountBalance;
import com.iortatechnxt.finverse.ledger.service.BalanceQuery;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.core.RowKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Shared engine of the MIS statements (FIN-MIS-*): format loading, balances per window, and the
 * statement-level and schedule-level (account detail) row layouts for any number of comparative
 * columns.
 */
@Component
public class MisStatements {

  /** Format parameter. */
  static final String FORMAT = "formatId";

  /** Rounding parameter. */
  static final String ROUNDING = "rounding";

  /** Particulars column. */
  static final String PARTICULARS = "particulars";

  /** Schedule reference column. */
  static final String SCHEDULE = "schedule";

  private static final String LAST_YEAR_BASIS = "lastYearBasis";
  private static final String SAME_DATE = "SAME_DATE_LAST_YEAR";
  private static final String PRIOR_FY_END = "PREVIOUS_FY_END";

  private final LedgerQueryService ledger;
  private final StatementFormatService formats;
  private final FinReportSupport support;

  /**
   * Creates the engine.
   *
   * @param ledger ledger read model
   * @param formats statement formats
   * @param support finance report helpers
   */
  public MisStatements(
      LedgerQueryService ledger, StatementFormatService formats, FinReportSupport support) {
    this.ledger = ledger;
    this.formats = formats;
    this.support = support;
  }

  /**
   * Format ID parameter (default: the built-in STANDARD format).
   *
   * @return spec
   */
  static ParameterSpec formatParam() {
    return ParameterSpec.required(FORMAT, "Format ID", ParameterType.TEXT)
        .withDefault(StatementFormatService.STANDARD);
  }

  /**
   * Rounding option parameter.
   *
   * @return spec
   */
  static ParameterSpec roundingParam() {
    return ParameterSpec.select(
        ROUNDING,
        "Rounding Option",
        Arrays.stream(Rounding.values()).map(Rounding::name).toList(),
        Rounding.NONE.name());
  }

  /**
   * Basis of the balance sheet "last year" column. The book does not state it: the default is the
   * same day and month one year earlier; the previous fiscal year end is the alternative.
   *
   * @return spec
   */
  static ParameterSpec lastYearParam() {
    return ParameterSpec.select(
        LAST_YEAR_BASIS, "Last Year Basis", List.of(SAME_DATE, PRIOR_FY_END), SAME_DATE);
  }

  /**
   * Comparative (last year) date of a balance sheet as-of date.
   *
   * @param p parameters
   * @param asOf as-of date
   * @return comparative date
   */
  LocalDate lastYearDate(ReportParameters p, LocalDate asOf) {
    return PRIOR_FY_END.equals(p.optionalText(LAST_YEAR_BASIS).orElse(SAME_DATE))
        ? yearStart(p, asOf).minusDays(1)
        : asOf.minusYears(1);
  }

  /**
   * Selected rounding.
   *
   * @param p parameters
   * @return rounding
   */
  static Rounding rounding(ReportParameters p) {
    return p.optionalText(ROUNDING).map(Rounding::valueOf).orElse(Rounding.NONE);
  }

  /**
   * Loads the selected format.
   *
   * @param p parameters
   * @param h chart of accounts
   * @param statementType "BS" or "IE"
   * @return format
   */
  StatementFormat format(ReportParameters p, AccountHierarchy h, String statementType) {
    return formats.load(p.longValue(FinParams.COMPANY), p.text(FORMAT), statementType, h);
  }

  /**
   * Chart of accounts of the selected company.
   *
   * @param p parameters
   * @return hierarchy
   */
  AccountHierarchy hierarchy(ReportParameters p) {
    return support.hierarchy(p.longValue(FinParams.COMPANY));
  }

  /**
   * Fiscal year start of a date.
   *
   * @param p parameters
   * @param date date
   * @return fiscal year start
   */
  LocalDate yearStart(ReportParameters p, LocalDate date) {
    return support.fiscalYearStart(p.longValue(FinParams.COMPANY), date);
  }

  /**
   * Net base balances (debit positive) of all accounts over a window, aggregated in SQL.
   *
   * @param p parameters (company, optional division)
   * @param from window start or null for inception
   * @param to window end
   * @return net balance by account
   */
  Map<Long, BigDecimal> net(ReportParameters p, LocalDate from, LocalDate to) {
    Map<Long, BigDecimal> out = new HashMap<>();
    BalanceQuery q =
        new BalanceQuery(p.longValue(FinParams.COMPANY), FinParams.branch(p), from, to, false);
    for (AccountBalance b : ledger.balances(q)) {
      out.put(b.accountId(), b.netBase());
    }
    return out;
  }

  /**
   * Statement-level rows: headings, format lines and totals with one value per column.
   *
   * @param f format
   * @param keys value column keys
   * @param evaluations evaluations, one per column
   * @return rows
   */
  static List<ReportRow> lineRows(
      StatementFormat f, List<String> keys, List<Evaluation> evaluations) {
    List<ReportRow> rows = new ArrayList<>();
    for (FormatLine line : f.lines()) {
      if (line.type() == FormatLine.Type.HEADING) {
        rows.add(FinRows.label(RowKind.SECTION, 0, line.caption()));
        continue;
      }
      Map<String, Object> cells = lineCells(line, keys, evaluations);
      rows.add(
          line.type() == FormatLine.Type.TOTAL
              ? FinRows.row(RowKind.SUBTOTAL, 0, line.caption(), cells)
              : FinRows.row(RowKind.DETAIL, 1, null, cells));
    }
    return rows;
  }

  /**
   * Schedule rows: under each ACCOUNTS line, the mapped accounts with a balance, then the line
   * total; totals of the format follow in order.
   *
   * @param f format
   * @param h chart of accounts
   * @param keys value column keys
   * @param nets balances per column, before sign
   * @param evaluations evaluations per column
   * @return rows
   */
  static List<ReportRow> scheduleRows(
      StatementFormat f,
      AccountHierarchy h,
      List<String> keys,
      List<Map<Long, BigDecimal>> nets,
      List<Evaluation> evaluations) {
    List<ReportRow> rows = new ArrayList<>();
    for (FormatLine line : f.lines()) {
      switch (line.type()) {
        case ACCOUNTS -> {
          String ref = line.scheduleRef() == null ? "" : "Schedule " + line.scheduleRef() + " : ";
          rows.add(FinRows.label(RowKind.GROUP_HEADER, 0, ref + line.caption()));
          accountsOf(f, h, line, nets).forEach(a -> rows.add(accountRow(a, line, keys, nets)));
          rows.add(
              FinRows.row(
                  RowKind.SUBTOTAL,
                  0,
                  "TOTAL " + line.caption(),
                  lineCells(line, keys, evaluations)));
        }
        case RESULT ->
            rows.add(FinRows.row(RowKind.DETAIL, 1, null, lineCells(line, keys, evaluations)));
        case TOTAL ->
            rows.add(
                FinRows.row(RowKind.TOTAL, 0, line.caption(), lineCells(line, keys, evaluations)));
        default -> rows.add(FinRows.label(RowKind.SECTION, 0, line.caption()));
      }
    }
    return rows;
  }

  /**
   * Notes listing accounts with a balance that the format does not map.
   *
   * @param evaluations evaluations
   * @return notes (empty when every account is mapped)
   */
  static List<String> unmappedNotes(List<Evaluation> evaluations) {
    Set<String> codes = new TreeSet<>();
    evaluations.forEach(e -> codes.addAll(e.unmappedAccounts()));
    return codes.isEmpty()
        ? List.of()
        : List.of("WARNING: accounts with a balance not mapped by the format: " + codes);
  }

  private static List<AccountNode> accountsOf(
      StatementFormat f, AccountHierarchy h, FormatLine line, List<Map<Long, BigDecimal>> nets) {
    Set<Long> ids = new TreeSet<>();
    nets.forEach(m -> ids.addAll(m.keySet()));
    return ids.stream()
        .map(h::node)
        .filter(a -> f.lineOf(a).map(l -> l.lineNo() == line.lineNo()).orElse(false))
        .sorted(Comparator.comparing(AccountNode::code))
        .toList();
  }

  private static ReportRow accountRow(
      AccountNode a, FormatLine line, List<String> keys, List<Map<Long, BigDecimal>> nets) {
    Map<String, Object> cells = FinRows.cells(PARTICULARS, a.caption());
    for (int i = 0; i < keys.size(); i++) {
      BigDecimal v = nets.get(i).getOrDefault(a.id(), BigDecimal.ZERO);
      cells.put(keys.get(i), v.multiply(BigDecimal.valueOf(line.sign())));
    }
    return FinRows.row(RowKind.DETAIL, 1, null, cells);
  }

  private static Map<String, Object> lineCells(
      FormatLine line, List<String> keys, List<Evaluation> evaluations) {
    Map<String, Object> cells =
        FinRows.cells(SCHEDULE, line.scheduleRef(), PARTICULARS, line.caption());
    for (int i = 0; i < keys.size(); i++) {
      cells.put(keys.get(i), evaluations.get(i).value(line.lineNo()));
    }
    return cells;
  }
}
