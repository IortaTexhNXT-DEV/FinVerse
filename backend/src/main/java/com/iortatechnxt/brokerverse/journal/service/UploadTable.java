package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.common.excel.GuidedTables;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The lines of a journal upload file read as a table: the header row (below the column guide of the
 * guided template, or the first row of a plain file), the data rows without the guide and example
 * rows, each parsed into an upload line.
 */
final class UploadTable {

  private UploadTable() {}

  /** The data rows of a file below its header row (guide and example rows skipped), parsed. */
  static List<UploadLine> lines(List<List<String>> table) {
    int headerRow = headerRow(table);
    List<String> columns = columns(table.get(headerRow));
    List<UploadLine> lines = new ArrayList<>();
    for (int i = headerRow + 1; i < table.size(); i++) {
      List<String> row = table.get(i);
      if (!SpreadsheetRows.isBlank(row) && !GuidedTables.isExample(row)) {
        lines.add(UploadLine.parse(i + 1, cells(columns, row)));
      }
    }
    if (lines.size() > JournalUploadService.MAX_ROWS) {
      throw new BusinessRuleException(
          "TOO_MANY_ROWS",
          "The file has more than " + JournalUploadService.MAX_ROWS + " data rows");
    }
    return lines;
  }

  /**
   * The header row: after the column guide of the guided template, else the row holding the
   * template columns (the first row of a plain file).
   */
  private static int headerRow(List<List<String>> table) {
    if (table.isEmpty()) {
      throw new BusinessRuleException("EMPTY_FILE", "The file has no header row");
    }
    return GuidedTables.headerRow(table, UploadLine.ALL_COLUMNS, UploadTable::column);
  }

  /** Normalized header columns; rejects files with missing columns. */
  private static List<String> columns(List<String> header) {
    List<String> columns = header.stream().map(UploadTable::column).toList();
    List<String> missing =
        UploadLine.REQUIRED_COLUMNS.stream().filter(c -> !columns.contains(c)).toList();
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "MISSING_COLUMNS", "Missing column(s): " + String.join(", ", missing));
    }
    return columns;
  }

  private static Map<String, String> cells(List<String> columns, List<String> row) {
    Map<String, String> values = new HashMap<>();
    for (int c = 0; c < columns.size() && c < row.size(); c++) {
      values.put(columns.get(c), row.get(c));
    }
    return values;
  }

  private static String column(String header) {
    return GuidedTables.header(header).toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
  }
}
