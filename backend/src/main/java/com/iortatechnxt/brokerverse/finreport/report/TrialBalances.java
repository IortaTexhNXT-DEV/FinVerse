package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.AccountNode;
import com.iortatechnxt.brokerverse.finreport.service.MovementRow;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Shared trial balance mechanics: roll-up to a key and the standard TB columns. */
final class TrialBalances {

  static final String CODE = "code";
  static final String NAME = "name";
  static final String OPENING = "opening";
  static final String DEBIT = "debit";
  static final String CREDIT = "credit";
  static final String CLOSING_DR = "closingDebit";
  static final String CLOSING_CR = "closingCredit";

  private TrialBalances() {}

  /**
   * Sums movement rows per key.
   *
   * @param rows rows
   * @param keyOf key function
   * @param <K> key type
   * @return amounts by key, in first-seen order
   */
  static <K> Map<K, TbAmounts> rollUp(
      Collection<MovementRow> rows, Function<MovementRow, K> keyOf) {
    Map<K, TbAmounts> out = new LinkedHashMap<>();
    for (MovementRow r : rows) {
      out.merge(keyOf.apply(r), TbAmounts.of(r), TbAmounts::plus);
    }
    return out;
  }

  /**
   * Sums complete movement rows (base and FC) per key.
   *
   * @param rows rows
   * @param keyOf key function
   * @param <K> key type
   * @return summed rows by key, in first-seen order
   */
  static <K> Map<K, MovementRow> sumByKey(
      Collection<MovementRow> rows, Function<MovementRow, K> keyOf) {
    Map<K, MovementRow> out = new LinkedHashMap<>();
    for (MovementRow r : rows) {
      out.merge(keyOf.apply(r), r, MovementRow::plus);
    }
    return out;
  }

  /**
   * Standard TB columns: code, name, opening, month debits and credits, closing debit / credit.
   *
   * @param codeLabel label of the account code column
   * @return columns
   */
  static List<ReportColumn> columns(String codeLabel) {
    return List.of(
        ReportColumn.text(CODE, codeLabel),
        ReportColumn.text(NAME, "Account Name"),
        ReportColumn.amount(OPENING, "Opening Balance Dr/(Cr)"),
        ReportColumn.amount(DEBIT, "This Month Debits"),
        ReportColumn.amount(CREDIT, "This Month Credits"),
        ReportColumn.amount(CLOSING_DR, "Closing Debits"),
        ReportColumn.amount(CLOSING_CR, "Closing Credits"));
  }

  /**
   * Standard TB cells of an account.
   *
   * @param account account
   * @param t amounts
   * @return cells
   */
  static Map<String, Object> cells(AccountNode account, TbAmounts t) {
    return FinRows.cells(
        CODE,
        account.code(),
        NAME,
        account.name(),
        OPENING,
        t.opening(),
        DEBIT,
        t.debit(),
        CREDIT,
        t.credit(),
        CLOSING_DR,
        t.closingDebit(),
        CLOSING_CR,
        t.closingCredit());
  }

  /**
   * Rows of main accounts sorted by code, skipping zero rows unless requested.
   *
   * @param byMain amounts by main account
   * @param includeZero whether to keep all-zero rows
   * @return detail rows
   */
  static List<Map<String, Object>> mainRows(
      Map<AccountNode, TbAmounts> byMain, boolean includeZero) {
    List<Map<String, Object>> rows = new ArrayList<>();
    byMain.entrySet().stream()
        .filter(e -> includeZero || !e.getValue().isZero())
        .sorted(Map.Entry.comparingByKey(Comparator.comparing(AccountNode::code)))
        .forEach(e -> rows.add(cells(e.getKey(), e.getValue())));
    return rows;
  }

  /**
   * Balance check note: Σ closing debits must equal Σ closing credits.
   *
   * @param rows detail rows
   * @return note
   */
  static String balanceNote(List<Map<String, Object>> rows) {
    BigDecimal dr = sum(rows, CLOSING_DR);
    BigDecimal cr = sum(rows, CLOSING_CR);
    return dr.compareTo(cr) == 0
        ? "Trial balance agrees: closing debits equal closing credits."
        : "WARNING: trial balance difference of " + dr.subtract(cr) + " (to suspense).";
  }

  private static BigDecimal sum(List<Map<String, Object>> rows, String key) {
    return rows.stream()
        .map(r -> (BigDecimal) r.getOrDefault(key, BigDecimal.ZERO))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
