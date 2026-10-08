package com.iortatechnxt.brokerverse.common.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Column widths of a generated sheet from its content: every column as wide as its longest value as
 * shown (dates and amounts formatted) plus a margin, so that a right-aligned date or amount never
 * touches the text of the next column ("02-Oct-2026Bautista"). A short heading stays on one line; a
 * long heading needs only its longest word, as headings wrap; very long values are capped.
 */
public final class SheetColumnWidths {

  /** Characters added to the longest value (the gap to the next column). */
  public static final int MARGIN = 3;

  /** Narrowest column, in characters. */
  public static final int MIN_CHARS = 8;

  /** A heading up to this length stays on one line. */
  public static final int ONE_LINE_HEADING = 20;

  /** Widest column, in characters. */
  public static final int MAX_CHARS = 50;

  private static final int UNITS_PER_CHAR = 256;

  private SheetColumnWidths() {}

  /**
   * Sets the width of the first columns of a sheet from the cells written.
   *
   * @param sheet sheet (all rows still in memory)
   * @param headerRow index of the heading row (a long heading counts with its longest word; the
   *     title rows above it do not count), -1 for none
   * @param columns number of columns to size
   */
  public static void fit(Sheet sheet, int headerRow, int columns) {
    DataFormatter formatter = new DataFormatter();
    int[] chars = new int[columns];
    for (Row row : sheet) {
      if (row.getRowNum() < headerRow) {
        continue;
      }
      for (int c = 0; c < columns; c++) {
        Cell cell = row.getCell(c);
        if (cell != null) {
          String text = formatter.formatCellValue(cell);
          int length = row.getRowNum() == headerRow ? heading(text) : longestLine(text);
          chars[c] = Math.max(chars[c], length);
        }
      }
    }
    for (int c = 0; c < columns; c++) {
      sheet.setColumnWidth(c, units(chars[c]));
    }
  }

  /**
   * The width, in Excel units, of a column whose longest value has the given number of characters.
   *
   * @param chars characters of the longest value
   * @return column width with the margin, between the narrowest and widest column
   */
  public static int units(int chars) {
    return Math.clamp(chars + MARGIN, MIN_CHARS, MAX_CHARS) * UNITS_PER_CHAR;
  }

  /**
   * The characters a heading needs: all of it when short, else its longest word (it wraps).
   *
   * @param text heading, may be null
   * @return characters
   */
  public static int heading(String text) {
    String t = text == null ? "" : text.strip();
    return t.length() <= ONE_LINE_HEADING ? t.length() : longestWord(t);
  }

  /**
   * The characters of the longest word of a heading.
   *
   * @param text heading, may be null
   * @return length of its longest word
   */
  public static int longestWord(String text) {
    int longest = 0;
    for (String word : (text == null ? "" : text).split("\\s+")) {
      longest = Math.max(longest, word.length());
    }
    return longest;
  }

  private static int longestLine(String text) {
    int longest = 0;
    for (String line : text.split("\\R")) {
      longest = Math.max(longest, line.length());
    }
    return longest;
  }
}
