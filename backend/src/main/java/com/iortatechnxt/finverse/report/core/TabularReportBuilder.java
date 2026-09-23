package com.iortatechnxt.finverse.report.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds grouped tabular reports with group headers, subtotals at every group level and a grand
 * total — the standard layout of the GI report book (e.g. Branch &gt; Class &gt; Product).
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

  private final ReportParameters params;
  private final List<ReportColumn> columns = new ArrayList<>();
  private final List<Group> groups = new ArrayList<>();
  private final List<Map<String, Object>> details = new ArrayList<>();
  private final List<String> notes = new ArrayList<>();
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
    List<Map<String, Object>> sorted = new ArrayList<>(details);
    if (!presorted) {
      sorted.sort(groupComparator());
    }
    List<ReportRow> out = new ArrayList<>();
    List<Object> current = new ArrayList<>();
    List<Map<String, BigDecimal>> accumulators = new ArrayList<>();
    for (Map<String, Object> row : sorted) {
      int changeLevel = firstChangedLevel(current, row);
      closeGroups(out, current, accumulators, changeLevel);
      openGroups(out, current, accumulators, row, changeLevel);
      out.add(ReportRow.detail(row));
      accumulators.forEach(acc -> add(acc, row));
    }
    closeGroups(out, current, accumulators, 0);
    if (grandTotal && !sorted.isEmpty() && columns.stream().anyMatch(ReportColumn::summed)) {
      Map<String, BigDecimal> total = new LinkedHashMap<>();
      sorted.forEach(r -> add(total, r));
      out.add(new ReportRow(RowKind.TOTAL, 0, "Grand Total", new LinkedHashMap<>(total)));
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
      int downToLevel) {
    for (int level = current.size() - 1; level >= downToLevel; level--) {
      Group g = groups.get(level);
      out.add(
          new ReportRow(
              RowKind.SUBTOTAL,
              level,
              "Total " + g.label() + " : " + current.get(level),
              new LinkedHashMap<>(accumulators.get(level))));
      current.remove(level);
      accumulators.remove(level);
    }
  }

  private void openGroups(
      List<ReportRow> out,
      List<Object> current,
      List<Map<String, BigDecimal>> accumulators,
      Map<String, Object> row,
      int fromLevel) {
    for (int level = fromLevel; level < groups.size(); level++) {
      Group g = groups.get(level);
      Object value = row.get(g.key());
      out.add(new ReportRow(RowKind.GROUP_HEADER, level, g.label() + " : " + value, Map.of()));
      current.add(value);
      accumulators.add(new LinkedHashMap<>());
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
