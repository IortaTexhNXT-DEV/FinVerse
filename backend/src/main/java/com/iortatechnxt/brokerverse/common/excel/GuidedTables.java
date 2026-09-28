package com.iortatechnxt.brokerverse.common.excel;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reading side of the guided templates: finds the header row of a table read from an upload (a
 * guided template has its title block and column guide above the header; a plain file has the
 * header in row 1), cleans a header text (the star of a mandatory column) and recognises the
 * example rows to skip. Works on any table of text cells, so XLSX, ODS and CSV (a guided sheet
 * saved as CSV) are read alike.
 */
public final class GuidedTables {

  /** Suffix marking a mandatory column in the header row. */
  public static final String MANDATORY_MARK = " *";

  /** Label of the first cell of an example row. */
  public static final String EXAMPLE_MARKER = "Example – overwrite or delete";

  /** Text of the header row's first cell (column A) of a guided template; not a header. */
  public static final String HEADER_CORNER = "Your rows start below ↓";

  /** Labels of the column guide band, in order; the header row follows the last one. */
  public static final List<String> BAND_LABELS =
      List.of("Mandatory", "Format", "Allowed values", "What to enter");

  /** Rows searched for the header row. */
  public static final int SEARCH_ROWS = 60;

  private static final Pattern EXAMPLE = Pattern.compile("(?i)^\\s*example\\s*[–—-].*");
  private static final String LAST_BAND_LABEL = BAND_LABELS.get(BAND_LABELS.size() - 1);

  private GuidedTables() {}

  /**
   * Index of the header row: the row after the guide band of a guided template; otherwise, when
   * expected headers are given, the first row holding the most of them (a guided file whose band
   * was removed); otherwise the first row.
   *
   * @param table rows of text cells
   * @param expected expected header texts (empty when unknown)
   * @return 0-based row index
   */
  public static int headerRow(List<List<String>> table, Collection<String> expected) {
    return headerRow(table, expected, UnaryOperator.identity());
  }

  /**
   * Index of the header row, comparing headers after a normalisation (e.g. lower case).
   *
   * @param table rows of text cells
   * @param expected expected header texts (empty when unknown)
   * @param normal normalisation applied to both sides
   * @return 0-based row index
   */
  public static int headerRow(
      List<List<String>> table, Collection<String> expected, UnaryOperator<String> normal) {
    int rows = Math.min(table.size(), SEARCH_ROWS);
    for (int r = 0; r < rows; r++) {
      if (isBandRow(table.get(r), LAST_BAND_LABEL)) {
        return nextFilled(table, r + 1);
      }
    }
    if (expected == null || expected.isEmpty()) {
      return 0;
    }
    Set<String> wanted =
        expected.stream().map(h -> normal.apply(header(h))).collect(Collectors.toSet());
    int best = 0;
    int bestHits = 0;
    for (int r = 0; r < rows; r++) {
      int hits = hits(table.get(r), wanted, normal);
      if (hits > bestHits) {
        best = r;
        bestHits = hits;
      }
    }
    return best;
  }

  /**
   * Number of expected headers found in the first rows of a table (to choose the data sheet of a
   * workbook).
   *
   * @param table rows
   * @param expected expected headers
   * @return largest number found in one row
   */
  public static int matchScore(List<List<String>> table, Collection<String> expected) {
    Set<String> wanted = expected.stream().map(GuidedTables::header).collect(Collectors.toSet());
    int best = 0;
    for (int r = 0; r < Math.min(table.size(), SEARCH_ROWS); r++) {
      best = Math.max(best, hits(table.get(r), wanted, UnaryOperator.identity()));
    }
    return best;
  }

  /**
   * Whether a table carries the column guide of a guided template.
   *
   * @param table rows
   * @return true when a guide band is found
   */
  public static boolean isGuided(List<List<String>> table) {
    for (int r = 0; r < Math.min(table.size(), SEARCH_ROWS); r++) {
      if (isBandRow(table.get(r), LAST_BAND_LABEL)) {
        return true;
      }
    }
    return false;
  }

  /**
   * A header cell without the star of a mandatory column.
   *
   * @param cell header cell
   * @return header text
   */
  public static String header(String cell) {
    if (cell == null) {
      return "";
    }
    String h = cell.strip();
    if (HEADER_CORNER.equals(h)) {
      return "";
    }
    while (h.endsWith("*")) {
      h = h.substring(0, h.length() - 1).strip();
    }
    return h;
  }

  /**
   * The headers of a row, cleaned.
   *
   * @param row header row
   * @return header texts
   */
  public static List<String> headers(List<String> row) {
    return row.stream().map(GuidedTables::header).toList();
  }

  /**
   * Whether a row is an example row of a guided template (its first cell carries the example
   * marker).
   *
   * @param row cells
   * @return true to skip the row
   */
  public static boolean isExample(List<String> row) {
    return !row.isEmpty() && row.get(0) != null && EXAMPLE.matcher(row.get(0)).matches();
  }

  private static boolean isBandRow(List<String> row, String label) {
    return !row.isEmpty()
        && row.get(0) != null
        && row.get(0).strip().toLowerCase(Locale.ROOT).equals(label.toLowerCase(Locale.ROOT));
  }

  private static int nextFilled(List<List<String>> table, int from) {
    for (int r = from; r < table.size(); r++) {
      if (table.get(r).stream().anyMatch(c -> c != null && !c.isBlank())) {
        return r;
      }
    }
    return from;
  }

  private static int hits(List<String> row, Set<String> wanted, UnaryOperator<String> normal) {
    int hits = 0;
    for (String cell : row) {
      if (cell != null && wanted.contains(normal.apply(header(cell)))) {
        hits++;
      }
    }
    return hits;
  }
}
