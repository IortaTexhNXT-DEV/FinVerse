package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter;
import com.iortatechnxt.brokerverse.common.excel.GuidedWorkbook;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * The current data of a configuration screen in the layout of its upload template (same title
 * block, column guide and headers), so it can be changed and uploaded again as it is (round trip).
 */
final class BulkExportFile {

  private BulkExportFile() {}

  /**
   * Writes the rows into the template.
   *
   * @param template the handler's template
   * @param rows values by header
   * @return xlsx bytes
   */
  static byte[] write(GuidedTemplate template, List<Map<String, String>> rows) {
    List<GuideColumn> columns = template.sheets().get(0).columns();
    try (GuidedWorkbook wb = GuidedTemplateWriter.open(template)) {
      Sheet sheet = wb.sheet(0);
      int r = wb.firstDataRow(0);
      for (Map<String, String> values : rows) {
        Row out = sheet.createRow(r++);
        for (int c = 0; c < columns.size(); c++) {
          GuideColumn col = columns.get(c);
          Cell cell = out.createCell(GuidedWorkbook.sheetColumn(c));
          cell.setCellStyle(wb.dataStyle(col.kind()));
          GuidedTemplateWriter.value(cell, col.kind(), values.getOrDefault(col.header(), ""));
        }
      }
      return wb.bytes();
    }
  }
}
