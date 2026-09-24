package com.iortatechnxt.brokerverse.bulk.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Reads the first sheet of an Excel workbook into text cells (dates as yyyy-MM-dd). */
final class XlsxTableReader {

  private XlsxTableReader() {}

  /**
   * Reads a workbook.
   *
   * @param content xlsx bytes
   * @return rows of cells
   * @throws IOException when the file is not a readable workbook
   */
  static List<List<String>> read(byte[] content) throws IOException {
    try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(content))) {
      Sheet sheet = wb.getSheetAt(0);
      DataFormatter formatter = new DataFormatter(Locale.ROOT);
      List<List<String>> table = new ArrayList<>();
      for (int r = 0; r <= sheet.getLastRowNum(); r++) {
        table.add(cells(sheet.getRow(r), formatter));
      }
      return table;
    }
  }

  private static List<String> cells(Row row, DataFormatter formatter) {
    List<String> cells = new ArrayList<>();
    if (row != null) {
      for (int c = 0; c < row.getLastCellNum(); c++) {
        cells.add(text(row.getCell(c), formatter));
      }
    }
    return cells;
  }

  private static String text(Cell cell, DataFormatter formatter) {
    if (cell == null) {
      return "";
    }
    CellType type =
        cell.getCellType() == CellType.FORMULA
            ? cell.getCachedFormulaResultType()
            : cell.getCellType();
    if (type == CellType.NUMERIC) {
      if (DateUtil.isCellDateFormatted(cell)) {
        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
      }
      return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
    }
    return formatter.formatCellValue(cell);
  }
}
