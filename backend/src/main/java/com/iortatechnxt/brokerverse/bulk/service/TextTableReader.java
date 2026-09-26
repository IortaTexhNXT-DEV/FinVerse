package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.bulk.service.TextLayout.FixedField;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reads a plain-text upload (.txt) into a table whose first row holds the headers, following the
 * handler's {@link TextLayout} (BRQID.006, CSHID.008). Blank lines are skipped; values are trimmed.
 */
final class TextTableReader {

  private static final String LAYOUT_ERROR = "BULK_TEXT_LAYOUT";
  private static final char[] DETECTED = {'\t', '|', ';', ','};
  private static final Pattern LINES = Pattern.compile("\\R");
  private static final char BOM = '﻿';

  private TextTableReader() {}

  /**
   * Reads a text file.
   *
   * @param content bytes (UTF-8)
   * @param layout layout
   * @return header row followed by data rows
   */
  static List<List<String>> read(byte[] content, TextLayout layout) {
    List<String> lines = lines(content);
    if (lines.isEmpty()) {
      return List.of();
    }
    return layout.kind() == TextLayout.Kind.FIXED_WIDTH
        ? fixedWidth(lines, layout.fields())
        : delimited(lines, layout);
  }

  private static List<String> lines(byte[] content) {
    String text = new String(content, StandardCharsets.UTF_8);
    if (!text.isEmpty() && text.charAt(0) == BOM) {
      text = text.substring(1);
    }
    return Arrays.stream(LINES.split(text)).filter(l -> !l.isBlank()).toList();
  }

  private static List<List<String>> delimited(List<String> lines, TextLayout layout) {
    char delimiter = layout.delimiter() == '\0' ? detect(lines.get(0)) : layout.delimiter();
    String separator = Pattern.quote(String.valueOf(delimiter));
    List<List<String>> table = new ArrayList<>();
    if (!layout.headers().isEmpty()) {
      table.add(layout.headers());
    }
    for (String line : lines) {
      table.add(Arrays.stream(line.split(separator, -1)).map(String::trim).toList());
    }
    return table;
  }

  private static char detect(String headerLine) {
    for (char candidate : DETECTED) {
      if (headerLine.indexOf(candidate) >= 0) {
        return candidate;
      }
    }
    throw new BusinessRuleException(
        LAYOUT_ERROR, "The text file has no tab, pipe, semicolon or comma separated header line");
  }

  private static List<List<String>> fixedWidth(List<String> lines, List<FixedField> fields) {
    if (fields.isEmpty()) {
      throw new BusinessRuleException(
          LAYOUT_ERROR, "The fixed-width layout of this upload type has no fields");
    }
    List<List<String>> table = new ArrayList<>();
    table.add(fields.stream().map(FixedField::header).toList());
    for (String line : lines) {
      table.add(fields.stream().map(f -> slice(line, f)).toList());
    }
    return table;
  }

  private static String slice(String line, FixedField field) {
    int from = Math.max(0, field.start() - 1);
    if (from >= line.length()) {
      return "";
    }
    return line.substring(from, Math.min(line.length(), from + field.length())).trim();
  }
}
