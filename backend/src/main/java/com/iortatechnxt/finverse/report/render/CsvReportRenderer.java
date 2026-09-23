package com.iortatechnxt.finverse.report.render;

import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.core.RowKind;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RFC 4180 CSV export. Cells beginning with a formula trigger character are prefixed with an
 * apostrophe to prevent CSV/formula injection when opened in a spreadsheet.
 */
@Component
public class CsvReportRenderer implements ReportRenderer {

  private static final String LABEL = "Label";
  private static final char BOM = '﻿';

  @Override
  public ExportFormat format() {
    return ExportFormat.CSV;
  }

  @Override
  public byte[] render(ReportResult result, ReportContext context) {
    StringBuilder sb = new StringBuilder().append(BOM);
    List<String> header = new ArrayList<>();
    header.add(LABEL);
    result.columns().forEach(c -> header.add(c.label()));
    appendLine(sb, header);
    for (ReportRow row : result.rows()) {
      List<String> cells = new ArrayList<>();
      cells.add(row.kind() == RowKind.DETAIL ? "" : nullToEmpty(row.label()));
      for (ReportColumn c : result.columns()) {
        cells.add(CellFormatter.format(row.cells().get(c.key()), c.type()));
      }
      appendLine(sb, cells);
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void appendLine(StringBuilder sb, List<String> cells) {
    for (int i = 0; i < cells.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(escape(cells.get(i)));
    }
    sb.append("\r\n");
  }

  static String escape(String raw) {
    String value = raw;
    if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 && !isNumber(value)) {
      value = "'" + value;
    }
    if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
      value = '"' + value.replace("\"", "\"\"") + '"';
    }
    return value;
  }

  private static boolean isNumber(String value) {
    return value.matches("-?[\\d,]+(\\.\\d+)?%?");
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
