package com.iortatechnxt.brokerverse.common.excel;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * The visible "Lists" sheet of a guided template: every list of allowed values (code and label) in
 * a block of two columns, the drop-downs of the data sheets taking their codes from it. The same
 * list used by several columns is written once.
 */
final class GuidedLists {

  /** Name of the lists sheet. */
  static final String SHEET = "Lists";

  /** Values written out in the Allowed values cell of the band. */
  static final int SHOWN_IN_BAND = 6;

  private static final int FIRST_VALUE_ROW = 4;
  private static final int CODE_WIDTH = 18;
  private static final int LABEL_WIDTH = 42;
  private static final int CHARACTER = 256;

  private final Map<List<Choice>, List<String>> lists = new LinkedHashMap<>();

  GuidedLists(List<GuidedSheet> sheets) {
    for (GuidedSheet sheet : sheets) {
      for (GuideColumn c : sheet.columns()) {
        if (!c.choices().isEmpty()) {
          List<String> users = lists.computeIfAbsent(c.choices(), k -> new ArrayList<>());
          if (!users.contains(c.header())) {
            users.add(c.header());
          }
        }
      }
    }
  }

  /**
   * The absolute reference of the codes of a list, e.g. Lists!$A$4:$A$9.
   *
   * @param choices list
   * @return reference, or null when the list is not on the sheet
   */
  String reference(List<Choice> choices) {
    int block = 0;
    for (List<Choice> list : lists.keySet()) {
      if (list.equals(choices)) {
        String col = columnLetter(block * 2);
        return SHEET
            + "!$"
            + col
            + "$"
            + (FIRST_VALUE_ROW + 1)
            + ":$"
            + col
            + "$"
            + (FIRST_VALUE_ROW + list.size());
      }
      block++;
    }
    return null;
  }

  /**
   * Writes the sheet (nothing when no column has a list).
   *
   * @param wb workbook
   * @param styles styles
   */
  void write(XSSFWorkbook wb, GuidedStyles styles) {
    if (lists.isEmpty()) {
      return;
    }
    Sheet sheet = wb.createSheet(SHEET);
    Row title = sheet.createRow(0);
    set(title, 0, "Lists of values").setCellStyle(styles.title);
    set(
            sheet.createRow(1),
            0,
            "The drop-downs of the template take their codes from here. Enter the code.")
        .setCellStyle(styles.info);
    Row names = sheet.createRow(2);
    Row heads = sheet.createRow(3);
    int block = 0;
    for (Map.Entry<List<Choice>, List<String>> e : lists.entrySet()) {
      int c = block * 2;
      sheet.setColumnWidth(c, CODE_WIDTH * CHARACTER);
      sheet.setColumnWidth(c + 1, LABEL_WIDTH * CHARACTER);
      set(names, c, String.join(", ", e.getValue())).setCellStyle(styles.infoLabel);
      set(heads, c, "Code").setCellStyle(styles.listHead);
      set(heads, c + 1, "Label").setCellStyle(styles.listHead);
      List<Choice> values = e.getKey();
      for (int i = 0; i < values.size(); i++) {
        Row row = row(sheet, FIRST_VALUE_ROW + i);
        set(row, c, values.get(i).code());
        set(row, c + 1, values.get(i).label());
      }
      block++;
    }
    sheet.createFreezePane(0, FIRST_VALUE_ROW);
    sheet.getPrintSetup().setLandscape(true);
    sheet.getPrintSetup().setFitWidth((short) 1);
    sheet.getPrintSetup().setFitHeight((short) 0);
    sheet.setFitToPage(true);
  }

  private static Row row(Sheet sheet, int r) {
    Row row = sheet.getRow(r);
    return row == null ? sheet.createRow(r) : row;
  }

  private static Cell set(Row row, int c, String value) {
    Cell cell = row.createCell(c);
    cell.setCellValue(value);
    return cell;
  }

  private static String columnLetter(int index) {
    StringBuilder out = new StringBuilder();
    int n = index + 1;
    while (n > 0) {
      int rem = (n - 1) % 26;
      out.insert(0, (char) ('A' + rem));
      n = (n - 1) / 26;
    }
    return out.toString();
  }
}
