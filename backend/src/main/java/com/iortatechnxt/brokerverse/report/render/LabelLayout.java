package com.iortatechnxt.brokerverse.report.render;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import java.util.List;
import java.util.Map;

/**
 * Where the labels of group headers, subtotals and totals go in a printed report, as on the report
 * screen: a group header spans the full width; a subtotal or total label spans the leading columns
 * its row leaves empty. A separate label column in front of the data is printed only when a
 * labelled row has a value in the first column, so a report grouped for its group headers alone
 * does not lose a column of the page to an empty label column.
 */
final class LabelLayout {

  private LabelLayout() {}

  /**
   * Whether the report needs a separate label column.
   *
   * @param result report
   * @return true when a labelled subtotal, total or detail row has a value in the first column
   */
  static boolean needsLabelColumn(ReportResult result) {
    return result.rows().stream()
        .anyMatch(
            r ->
                r.kind() != RowKind.GROUP_HEADER
                    && r.kind() != RowKind.SECTION
                    && r.label() != null
                    && !r.label().isBlank()
                    && leadingEmpty(result.columns(), r.cells()) == 0);
  }

  /**
   * The number of leading columns without a value in a row (where its label can go).
   *
   * @param columns columns
   * @param cells cells of the row
   * @return number of empty columns before the first value
   */
  static int leadingEmpty(List<ReportColumn> columns, Map<String, Object> cells) {
    for (int i = 0; i < columns.size(); i++) {
      Object v = cells.get(columns.get(i).key());
      if (v != null && !(v instanceof String s && s.isBlank())) {
        return i;
      }
    }
    return columns.size();
  }

  /**
   * The label of a row, indented by its level; empty when it has none.
   *
   * @param row row
   * @return label text
   */
  static String label(ReportRow row) {
    return row.label() == null ? "" : "  ".repeat(row.level()) + row.label();
  }
}
