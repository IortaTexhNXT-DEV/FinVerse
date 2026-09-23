package com.iortatechnxt.finverse.report.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Report row.
 *
 * @param kind row role
 * @param level nesting level of group headers / subtotals (0 = outermost)
 * @param label label for non-detail rows
 * @param cells values by column key
 */
public record ReportRow(RowKind kind, int level, String label, Map<String, Object> cells) {

  /** Canonical constructor keeping an unmodifiable, ordered copy of the cells. */
  public ReportRow {
    cells = Collections.unmodifiableMap(new LinkedHashMap<>(cells));
  }

  /**
   * Detail row.
   *
   * @param cells values
   * @return row
   */
  public static ReportRow detail(Map<String, Object> cells) {
    return new ReportRow(RowKind.DETAIL, 0, null, cells);
  }

  /**
   * Section heading row (e.g. "ASSETS").
   *
   * @param label label
   * @return row
   */
  public static ReportRow section(String label) {
    return new ReportRow(RowKind.SECTION, 0, label, Map.of());
  }
}
