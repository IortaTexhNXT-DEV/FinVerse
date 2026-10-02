package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.report.render.CellFormatter;
import com.iortatechnxt.brokerverse.report.render.PrintOptions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Options of an export: the PDF print options (FRBS 2.4.9) and the column filters of the report
 * viewer (FRBS 2.4.4), applied server side so the file holds what the user sees.
 *
 * @param print PDF print options
 * @param columnFilters text filter per column key ("contains", case-insensitive)
 */
public record ExportOptions(PrintOptions print, Map<String, String> columnFilters) {

  /** No filter, default print layout. */
  public static final ExportOptions NONE = new ExportOptions(PrintOptions.DEFAULT, Map.of());

  /** Copies the filters, dropping blank ones. */
  public ExportOptions {
    print = print == null ? PrintOptions.DEFAULT : print;
    Map<String, String> clean = new LinkedHashMap<>();
    if (columnFilters != null) {
      columnFilters.forEach(
          (k, v) -> {
            if (k != null && v != null && !v.isBlank()) {
              clean.put(k, v.trim().toLowerCase(Locale.ROOT));
            }
          });
    }
    columnFilters = Map.copyOf(clean);
  }

  /**
   * Parses filters given as {@code column:text} pairs.
   *
   * @param pairs pairs, may be null
   * @return filters by column key
   */
  public static Map<String, String> parseFilters(List<String> pairs) {
    Map<String, String> filters = new LinkedHashMap<>();
    for (String pair : pairs == null ? List.<String>of() : pairs) {
      int colon = pair.indexOf(':');
      if (colon > 0) {
        filters.put(pair.substring(0, colon).trim(), pair.substring(colon + 1));
      }
    }
    return filters;
  }

  /**
   * Keeps the detail rows matching every column filter. Group headers, subtotals and totals no
   * longer add up once rows are filtered, so they are dropped and a note names the filters.
   *
   * @param result full result
   * @return filtered result (the same result when no filter is set)
   */
  public ReportResult filter(ReportResult result) {
    if (columnFilters.isEmpty()) {
      return result;
    }
    List<ReportRow> rows =
        result.rows().stream()
            .filter(r -> r.kind() == RowKind.DETAIL)
            .filter(r -> matchesAll(r, result.columns()))
            .toList();
    List<String> notes = new ArrayList<>(result.notes());
    notes.add("Filtered on " + columnFilters + ": " + rows.size() + " row(s), totals omitted");
    return new ReportResult(
        result.code(), result.title(), result.parameterEcho(), result.columns(), rows, notes);
  }

  private boolean matchesAll(ReportRow row, List<ReportColumn> columns) {
    return columnFilters.entrySet().stream()
        .allMatch(
            f ->
                columns.stream()
                    .filter(c -> c.key().equals(f.getKey()))
                    .findFirst()
                    .map(c -> matches(row.cells().get(c.key()), c.type(), f.getValue()))
                    .orElse(true));
  }

  private static boolean matches(Object value, ColumnType type, String text) {
    String raw = Objects.toString(value, "").toLowerCase(Locale.ROOT);
    String shown = CellFormatter.format(value, type).toLowerCase(Locale.ROOT);
    return raw.contains(text) || shown.contains(text);
  }
}
