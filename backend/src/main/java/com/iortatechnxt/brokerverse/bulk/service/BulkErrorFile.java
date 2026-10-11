package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter;
import com.iortatechnxt.brokerverse.common.excel.GuidedWorkbook;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/** The error file of an upload: the rows that were not processed, to correct and upload again. */
final class BulkErrorFile {

  private BulkErrorFile() {}

  /**
   * The error file: the rows that were not processed, in the guided template layout (same title
   * block, column guide and headers, so the corrected file is uploaded again as it is) with an
   * "Error" column last. The cells a message names are highlighted, and the Error cell of every
   * rejected row.
   *
   * @param template the handler's template
   * @param rows rows of the job
   * @param values row values by row id
   * @return xlsx bytes
   */
  static byte[] write(
      GuidedTemplate template, List<BulkRowRecord> rows, Map<Long, Map<String, String>> values) {
    GuidedTemplate layout = BulkTemplates.errorFile(template);
    List<GuideColumn> columns = layout.sheets().get(0).columns();
    try (GuidedWorkbook wb = GuidedTemplateWriter.open(layout)) {
      Sheet sheet = wb.sheet(0);
      int r = wb.firstDataRow(0);
      for (BulkRowRecord row : rows) {
        if (row.getStatus() != BulkRowStatus.COMMITTED) {
          errorRow(
              wb, sheet.createRow(r++), columns, row, values.getOrDefault(row.getId(), Map.of()));
        }
      }
      return wb.bytes();
    }
  }

  /** One rejected row of the error file: its values, then its messages in the Error column. */
  private static void errorRow(
      GuidedWorkbook wb,
      Row out,
      List<GuideColumn> columns,
      BulkRowRecord row,
      Map<String, String> v) {
    String messages = BulkWorkbooks.textOf(row.getMessages());
    String lower = messages.toLowerCase(Locale.ROOT);
    int dataColumns = columns.size() - 1;
    for (int c = 0; c < dataColumns; c++) {
      GuideColumn col = columns.get(c);
      Cell cell = out.createCell(GuidedWorkbook.sheetColumn(c));
      boolean date = col.kind() == Kind.DATE;
      boolean marked = !messages.isEmpty() && lower.contains(col.header().toLowerCase(Locale.ROOT));
      cell.setCellStyle(marked ? wb.markedStyle(date) : wb.dataStyle(col.kind()));
      GuidedTemplateWriter.value(cell, col.kind(), v.getOrDefault(col.header(), ""));
    }
    Cell error = out.createCell(GuidedWorkbook.sheetColumn(dataColumns));
    error.setCellValue(messages);
    if (!messages.isEmpty()) {
      error.setCellStyle(wb.markedStyle(false));
    }
  }
}
