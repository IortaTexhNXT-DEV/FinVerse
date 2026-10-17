package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.util.BusinessText;
import java.util.List;

/**
 * Generated report data, rendered on screen or exported to PDF / Excel / CSV.
 *
 * @param code report code
 * @param title title
 * @param parameterEcho human readable parameter lines printed under the title
 * @param columns columns
 * @param rows rows (details, group headers, subtotals, totals)
 * @param notes footnotes (e.g. calculation basis)
 */
public record ReportResult(
    String code,
    String title,
    List<String> parameterEcho,
    List<ReportColumn> columns,
    List<ReportRow> rows,
    List<String> notes) {

  /**
   * Canonical constructor copying lists; the title and notes are business texts (requirement
   * references removed, design notes left out).
   */
  public ReportResult {
    title = BusinessText.clean(title);
    notes =
        notes.stream().filter(n -> !BusinessText.isDesignNote(n)).map(BusinessText::clean).toList();
    parameterEcho = List.copyOf(parameterEcho);
    columns = List.copyOf(columns);
    rows = List.copyOf(rows);
    notes = List.copyOf(notes);
  }
}
