package com.iortatechnxt.brokerverse.audit.service;

import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog.Entry;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * The Excel file of the Master Data Change Log (G05): one row per field change with the record, the
 * field, the values before and after, the maker and the checker with their times, under a title row
 * naming the filters.
 */
public final class MasterChangeLogWorkbook {

  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
  private static final String[] HEADERS = {
    "Record Type",
    "Record ID",
    "Change",
    "Field",
    "Old Value",
    "New Value",
    "Maker",
    "Made At",
    "Checker",
    "Checked At"
  };
  private static final int WIDTH = 22 * 256;
  private static final int FIRST_ROW = 3;

  private MasterChangeLogWorkbook() {}

  /**
   * The workbook.
   *
   * @param title title row, naming the filters
   * @param rows changes
   * @return XLSX bytes
   */
  public static byte[] of(String title, List<Entry> rows) {
    try (Workbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("Master Data Change Log");
      CellStyle bold = wb.createCellStyle();
      Font font = wb.createFont();
      font.setBold(true);
      bold.setFont(font);
      Row titleRow = sheet.createRow(0);
      titleRow.createCell(0).setCellValue(title);
      titleRow.getCell(0).setCellStyle(bold);
      Row header = sheet.createRow(FIRST_ROW - 1);
      for (int i = 0; i < HEADERS.length; i++) {
        header.createCell(i).setCellValue(HEADERS[i]);
        header.getCell(i).setCellStyle(bold);
        sheet.setColumnWidth(i, WIDTH);
      }
      int r = FIRST_ROW;
      for (Entry e : rows) {
        Row row = sheet.createRow(r++);
        String[] values = {
          e.entityType(),
          String.valueOf(e.entityId()),
          e.changeKind(),
          e.field(),
          e.oldValue(),
          e.newValue(),
          e.maker(),
          time(e.madeAt()),
          e.checker(),
          time(e.checkedAt())
        };
        for (int i = 0; i < values.length; i++) {
          row.createCell(i).setCellValue(values[i] == null ? "" : values[i]);
        }
      }
      sheet.createFreezePane(0, FIRST_ROW);
      wb.write(out);
      return out.toByteArray();
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot write the change log workbook", ex);
    }
  }

  private static String time(Instant i) {
    return i == null ? null : TIME.format(i.atZone(BusinessClock.zone()));
  }
}
