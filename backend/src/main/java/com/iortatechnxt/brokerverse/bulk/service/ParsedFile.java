package com.iortatechnxt.brokerverse.bulk.service;

import java.util.List;
import java.util.Map;

/**
 * An uploaded file read into rows.
 *
 * @param headers headers of the first row, in order
 * @param rows data rows: file row number and raw values by header (non-blank only)
 */
public record ParsedFile(List<String> headers, List<RawRow> rows) {

  /** Defensive copies. */
  public ParsedFile {
    headers = List.copyOf(headers);
    rows = List.copyOf(rows);
  }

  /**
   * A raw data row.
   *
   * @param rowNo row number in the file (header = 1)
   * @param values raw values by header
   */
  public record RawRow(int rowNo, Map<String, String> values) {

    /** Defensive copy. */
    public RawRow {
      values = Map.copyOf(values);
    }
  }
}
