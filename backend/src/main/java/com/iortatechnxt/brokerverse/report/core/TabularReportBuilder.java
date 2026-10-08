package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds grouped tabular reports with group headers, subtotals at every group level and a grand
 * total — the standard layout of the GI report book (e.g. Branch &gt; Class &gt; Product). A
 * subtotal names the number of records of its group unless a totalled count column already shows
 * it; a grouped report without totalled columns still ends with a grand total of the records.
 * Status and other code values of the columns named in {@link #labelCodes} are shown as labels
 * (FULLY_REMITTED as "Fully Remitted"), as on the screens.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * return TabularReportBuilder.of(params)
 *     .columns(ReportColumn.text("policyNo", "Policy No"), ReportColumn.amount("net", "Net"))
 *     .groupBy("branch", "Branch")
 *     .groupBy("lob", "Class")
 *     .rows(detailRows)
 *     .build();
 * }</pre>
 */
public final class TabularReportBuilder {

  /** A status or other code: capitals, digits and underscores (DIRECT_BILLED, OUTSTANDING). */
  private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]*");

  private final ReportParameters params;
  private final List<ReportColumn> columns = new ArrayList<>();
  private final List<Group> groups = new ArrayList<>();
  private final List<Map<String, Object>> details = new ArrayList<>();
  private final List<String> notes = new ArrayList<>();
  private final Set<String> codeKeys = new HashSet<>();
  private boolean grandTotal = true;
  private boolean presorted;

  private TabularReportBuilder(ReportParameters params) {
    this.params = params;
  }

  /**
   * Starts a builder.
   *
   * @param params report parameters (supplies code, title and parameter echo)
   * @return builder
   */
  public static TabularReportBuilder of(ReportParameters params) {
    return new TabularReportBuilder(params);
  }

  /**
   * Adds columns.
   *
   * @param cols columns in display order
   * @return this
   */
  public TabularReportBuilder columns(ReportColumn... cols) {
    columns.addAll(List.of(cols));
    return this;
  }

  /**
   * Adds columns.
   *
   * @param cols columns in display order
   * @return this
   */
  public TabularReportBuilder columns(List<ReportColumn> cols) {
    columns.addAll(cols);
    return this;
  }

  /**
   * Adds a grouping level (outermost first). The key refers to a cell value of each detail row; it
   * need not be a displayed column.
   *
   * @param key cell key
   * @param label group label, e.g. "Branch"
   * @return this
   */
  public TabularReportBuilder groupBy(String key, String label) {
    groups.add(new Group(key, label));
    return this;
  }

  /**
   * Shows the values of these columns or grouping keys as labels: a status or other code (an enum,
   * or a text of capitals, digits and underscores such as DIRECT_BILLED) is written as words with
   * capitals ("Direct Billed"); other values are kept.
   *
   * @param keys cell keys
   * @return this
   */
  public TabularReportBuilder labelCodes(String... keys) {
    codeKeys.addAll(List.of(keys));
    return this;
  }

  /**
   * Adds detail rows.
   *
   * @param rows rows
   * @return this
   */
  public TabularReportBuilder rows(List<Map<String, Object>> rows) {
    details.addAll(rows);
    return this;
  }

  /**
   * Adds a footnote.
   *
   * @param note note
   * @return this
   */
  public TabularReportBuilder note(String note) {
    notes.add(note);
    return this;
  }

  /**
   * Disables the grand total row.
   *
   * @return this
   */
  public TabularReportBuilder withoutGrandTotal() {
    this.grandTotal = false;
    return this;
  }

  /**
   * Declares that rows are already ordered by the grouping keys, so the caller's order (for example
   * by account class, then code) is kept instead of sorting group values alphabetically.
   *
   * @return this
   */
  public TabularReportBuilder presorted() {
    this.presorted = true;
    return this;
  }

  /**
   * Builds the result.
   *
   * @return report result
   */
  public ReportResult build() {
    List<Map<String, Object>> sorted = new ArrayList<>(details.size());
    details.forEach(d -> sorted.add(labelled(d)));
    if (!presorted) {
      sorted.sort(groupComparator());
    }
    List<ReportRow> out = new ArrayList<>();
    List<Object> current = new ArrayList<>();
    List<Map<String, BigDecimal>> accumulators = new ArrayList<>();
    List<int[]> counts = new ArrayList<>();
    for (Map<String, Object> row : sorted) {
      int changeLevel = firstChangedLevel(current, row);
      closeGroups(out, current, accumulators, counts, changeLevel);
      openGroups(out, current, accumulators, counts, row, changeLevel);
      out.add(ReportRow.detail(row));
      accumulators.forEach(acc -> add(acc, row));
      counts.forEach(c -> c[0]++);
    }
    closeGroups(out, current, accumulators, counts, 0);
    boolean summed = columns.stream().anyMatch(ReportColumn::summed);
    if (grandTotal && !sorted.isEmpty() && (summed || !groups.isEmpty())) {
      Map<String, BigDecimal> total = new LinkedHashMap<>();
      sorted.forEach(r -> add(total, r));
      out.add(
          new ReportRow(
              RowKind.TOTAL, 0, counted("Grand Total", sorted.size()), new LinkedHashMap<>(total)));
    }
    ReportMetadata meta = params.metadata();
    return new ReportResult(meta.code(), meta.title(), params.echo(), columns, out, notes);
  }

  private Comparator<Map<String, Object>> groupComparator() {
    Comparator<Map<String, Object>> cmp = (a, b) -> 0;
    for (Group g : groups) {
      cmp = cmp.thenComparing(r -> String.valueOf(r.get(g.key())));
    }
    return cmp;
  }

  private int firstChangedLevel(List<Object> current, Map<String, Object> row) {
    for (int i = 0; i < current.size(); i++) {
      if (!Objects.equals(current.get(i), row.get(groups.get(i).key()))) {
        return i;
      }
    }
    return current.size();
  }

  private void closeGroups(
      List<ReportRow> out,
      List<Object> current,
      List<Map<String, BigDecimal>> accumulators,
      List<int[]> counts,
      int downToLevel) {
    for (int level = current.size() - 1; level >= downToLevel; level--) {
      Group g = groups.get(level);
      out.add(
          new ReportRow(
              RowKind.SUBTOTAL,
              level,
              counted("Total " + g.label() + " : " + current.get(level), counts.get(level)[0]),
              new LinkedHashMap<>(accumulators.get(level))));
      current.remove(level);
      accumulators.remove(level);
      counts.remove(level);
    }
  }

  /** A total label with the number of records, unless a totalled count column already shows it. */
  private String counted(String label, int records) {
    boolean countColumn =
        columns.stream().anyMatch(c -> c.summed() && c.type() == ColumnType.NUMBER);
    if (countColumn) {
      return label;
    }
    return label + " (" + records + (records == 1 ? " record)" : " records)");
  }

  private Map<String, Object> labelled(Map<String, Object> row) {
    if (codeKeys.isEmpty() || codeKeys.stream().noneMatch(row::containsKey)) {
      return row;
    }
    Map<String, Object> copy = new LinkedHashMap<>(row);
    codeKeys.forEach(k -> copy.computeIfPresent(k, (key, v) -> codeLabel(v)));
    return copy;
  }

  /**
   * A status or other code as its label; other values unchanged.
   *
   * @param value cell value
   * @return the label, or the value
   */
  static Object codeLabel(Object value) {
    if (value instanceof Enum<?> e) {
      return DisplayFormat.label(e.name());
    }
    if (value instanceof String s && CODE.matcher(s).matches()) {
      return DisplayFormat.label(s);
    }
    return value;
  }

  private void openGroups(
      List<ReportRow> out,
      List<Object> current,
      List<Map<String, BigDecimal>> accumulators,
      List<int[]> counts,
      Map<String, Object> row,
      int fromLevel) {
    for (int level = fromLevel; level < groups.size(); level++) {
      Group g = groups.get(level);
      Object value = row.get(g.key());
      out.add(new ReportRow(RowKind.GROUP_HEADER, level, g.label() + " : " + value, Map.of()));
      current.add(value);
      accumulators.add(new LinkedHashMap<>());
      counts.add(new int[1]);
    }
  }

  private void add(Map<String, BigDecimal> acc, Map<String, Object> row) {
    for (ReportColumn c : columns) {
      if (c.summed() && row.get(c.key()) instanceof Number n) {
        acc.merge(c.key(), toDecimal(n), BigDecimal::add);
      }
    }
  }

  private static BigDecimal toDecimal(Number n) {
    return n instanceof BigDecimal bd ? bd : new BigDecimal(n.toString());
  }

  private record Group(String key, String label) {}
}
