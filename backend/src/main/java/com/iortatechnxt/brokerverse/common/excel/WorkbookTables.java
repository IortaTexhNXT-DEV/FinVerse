package com.iortatechnxt.brokerverse.common.excel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
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

/**
 * Reads the data sheet of an uploaded Excel workbook into text cells (dates as yyyy-MM-dd): the
 * sheet holding the most expected headers, else the first sheet carrying a column guide, else the
 * first sheet.
 */
public final class WorkbookTables {

  private WorkbookTables() {}

  /**
   * Reads a workbook.
   *
   * @param content xlsx bytes
   * @param expected expected headers (empty when unknown)
   * @return rows of cells
   * @throws IOException when the file is not a readable workbook
   */
  public static List<List<String>> read(byte[] content, Collection<String> expected)
      throws IOException {
    try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(content))) {
      DataFormatter formatter = new DataFormatter(Locale.ROOT);
      Sheet sheet = dataSheet(wb, expected, formatter);
      return rows(sheet, formatter, sheet.getLastRowNum());
    }
  }

  private static Sheet dataSheet(
      Workbook wb, Collection<String> expected, DataFormatter formatter) {
    if (wb.getNumberOfSheets() == 1) {
      return wb.getSheetAt(0);
    }
    Sheet best = null;
    int bestScore = 0;
    Sheet firstGuided = null;
    for (Sheet sheet : wb) {
      List<List<String>> top = rows(sheet, formatter, GuidedTables.SEARCH_ROWS);
      if (firstGuided == null && GuidedTables.isGuided(top)) {
        firstGuided = sheet;
      }
      int score = expected.isEmpty() ? 0 : GuidedTables.matchScore(top, expected);
      if (score > bestScore) {
        best = sheet;
        bestScore = score;
      }
    }
    if (best != null) {
      return best;
    }
    return firstGuided != null ? firstGuided : wb.getSheetAt(0);
  }

  private static List<List<String>> rows(Sheet sheet, DataFormatter formatter, int lastRow) {
    List<List<String>> table = new ArrayList<>();
    for (int r = 0; r <= Math.min(lastRow, sheet.getLastRowNum()); r++) {
      table.add(cells(sheet.getRow(r), formatter));
    }
    return table;
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
