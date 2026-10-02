package com.iortatechnxt.brokerverse.bulk.service;

import java.util.ArrayList;
import java.util.List;

/**
 * RFC 4180 CSV parser: comma separated, double-quoted fields with doubled quotes as escape, CRLF or
 * LF line ends, optional UTF-8 byte-order mark.
 */
// A parser instance lives for one parse call only, so its StringBuilder cannot grow unbounded.
@SuppressWarnings("PMD.AvoidStringBufferField")
final class CsvParser {

  private static final char QUOTE = '"';
  private static final char BOM = '﻿';

  private final String text;
  private final List<List<String>> table = new ArrayList<>();
  private List<String> row = new ArrayList<>();
  private final StringBuilder cell = new StringBuilder();
  private boolean quoted;
  private int pos;

  private CsvParser(String text) {
    this.text = !text.isEmpty() && text.charAt(0) == BOM ? text.substring(1) : text;
  }

  /**
   * Parses a CSV text.
   *
   * @param text CSV content
   * @return rows of cells
   */
  static List<List<String>> parse(String text) {
    return new CsvParser(text).run();
  }

  private List<List<String>> run() {
    while (pos < text.length()) {
      char ch = text.charAt(pos++);
      if (quoted) {
        inQuotes(ch);
      } else {
        outsideQuotes(ch);
      }
    }
    if (!cell.isEmpty() || !row.isEmpty()) {
      endRow();
    }
    return table;
  }

  private void inQuotes(char ch) {
    if (ch != QUOTE) {
      cell.append(ch);
    } else if (pos < text.length() && text.charAt(pos) == QUOTE) {
      cell.append(QUOTE);
      pos++;
    } else {
      quoted = false;
    }
  }

  private void outsideQuotes(char ch) {
    switch (ch) {
      case QUOTE -> quoted = true;
      case ',' -> endCell();
      case '\n' -> endRow();
      case '\r' -> {
        // CR of a CRLF line end: the LF ends the row.
      }
      default -> cell.append(ch);
    }
  }

  private void endCell() {
    row.add(cell.toString());
    cell.setLength(0);
  }

  private void endRow() {
    endCell();
    table.add(row);
    row = new ArrayList<>();
  }
}
