package com.iortatechnxt.finverse.report.render;

import com.iortatechnxt.finverse.report.core.ColumnType;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.core.RowKind;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Excel (XLSX) export with typed numeric/date cells, frozen header and styled group rows.
 *
 * <p>Uses the streaming workbook so very large reports do not exhaust memory.
 */
@Component
public class XlsxReportRenderer implements ReportRenderer {

  private static final int WINDOW = 200;
  private static final short TITLE_POINTS = 12;
  private static final int LABEL_WIDTH = 36 * 256;
  private static final int TEXT_WIDTH = 28 * 256;
  private static final int NUMBER_WIDTH = 16 * 256;
  private static final String AMOUNT_FORMAT = "#,##0.00;(#,##0.00)";
  private static final DateTimeFormatter STAMP =
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.of("Asia/Manila"));

  @Override
  public ExportFormat format() {
    return ExportFormat.XLSX;
  }

  @Override
  public byte[] render(ReportResult result, ReportContext context) {
    try (SXSSFWorkbook wb = new SXSSFWorkbook(WINDOW);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Styles styles = new Styles(wb);
      Sheet sheet = wb.createSheet(result.code());
      int r = writeHeader(sheet, result, context, styles);
      int headerRow = r;
      Row head = sheet.createRow(r++);
      text(head, 0, "", styles.head);
      List<ReportColumn> cols = result.columns();
      for (int i = 0; i < cols.size(); i++) {
        text(head, i + 1, cols.get(i).label(), styles.head);
        sheet.setColumnWidth(
            i + 1, cols.get(i).type() == ColumnType.TEXT ? TEXT_WIDTH : NUMBER_WIDTH);
      }
      sheet.setColumnWidth(0, LABEL_WIDTH);
      sheet.createFreezePane(1, headerRow + 1);
      for (ReportRow row : result.rows()) {
        writeRow(sheet.createRow(r++), row, cols, styles);
      }
      for (String note : result.notes()) {
        text(sheet.createRow(++r), 0, "Note: " + note, styles.meta);
      }
      wb.write(out);
      return out.toByteArray();
    } catch (IOException ex) {
      throw new UncheckedIOException("Excel rendering failed", ex);
    }
  }

  private static int writeHeader(Sheet sheet, ReportResult result, ReportContext ctx, Styles s) {
    int r = 0;
    text(sheet.createRow(r++), 0, ctx.companyName(), s.title);
    text(sheet.createRow(r++), 0, result.title(), s.title);
    text(
        sheet.createRow(r++),
        0,
        "Report ID: "
            + result.code()
            + "   User ID: "
            + ctx.generatedBy()
            + "   Run Date: "
            + STAMP.format(ctx.generatedAt()),
        s.meta);
    for (String line : result.parameterEcho()) {
      text(sheet.createRow(r++), 0, line, s.meta);
    }
    return r + 1;
  }

  private static void writeRow(Row x, ReportRow row, List<ReportColumn> cols, Styles s) {
    boolean emphasis = row.kind() != RowKind.DETAIL;
    String label = row.label() == null ? "" : "  ".repeat(row.level()) + row.label();
    text(x, 0, label, emphasis ? s.bold : s.body);
    for (int i = 0; i < cols.size(); i++) {
      ReportColumn c = cols.get(i);
      writeCell(x.createCell(i + 1), row.cells().get(c.key()), c.type(), emphasis, s);
    }
  }

  private static void writeCell(Cell cell, Object v, ColumnType type, boolean emphasis, Styles s) {
    if (v instanceof Number n && type != ColumnType.TEXT) {
      cell.setCellValue(n.doubleValue());
      cell.setCellStyle(emphasis ? s.boldNumbers.get(type) : s.numbers.get(type));
    } else if (v instanceof LocalDate d) {
      cell.setCellValue(d);
      cell.setCellStyle(s.date);
    } else {
      cell.setCellValue(v == null ? "" : v.toString());
      cell.setCellStyle(emphasis ? s.bold : s.body);
    }
  }

  private static void text(Row row, int col, String value, CellStyle style) {
    Cell cell = row.createCell(col);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  /** Workbook cell styles (created once per workbook). */
  private static final class Styles {
    private final CellStyle title;
    private final CellStyle meta;
    private final CellStyle head;
    private final CellStyle body;
    private final CellStyle bold;
    private final CellStyle date;
    private final Map<ColumnType, CellStyle> numbers = new EnumMap<>(ColumnType.class);
    private final Map<ColumnType, CellStyle> boldNumbers = new EnumMap<>(ColumnType.class);

    Styles(SXSSFWorkbook wb) {
      Font titleFont = wb.createFont();
      titleFont.setBold(true);
      titleFont.setFontHeightInPoints(TITLE_POINTS);
      titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
      title = wb.createCellStyle();
      title.setFont(titleFont);
      meta = wb.createCellStyle();
      Font headFont = wb.createFont();
      headFont.setBold(true);
      headFont.setColor(IndexedColors.WHITE.getIndex());
      head = wb.createCellStyle();
      head.setFont(headFont);
      head.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
      head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      head.setBorderBottom(BorderStyle.THIN);
      body = wb.createCellStyle();
      Font boldFont = wb.createFont();
      boldFont.setBold(true);
      bold = wb.createCellStyle();
      bold.setFont(boldFont);
      date = wb.createCellStyle();
      date.setDataFormat(wb.createDataFormat().getFormat("dd-mm-yyyy"));
      for (ColumnType t : ColumnType.values()) {
        String fmt = t == ColumnType.NUMBER ? "#,##0" : AMOUNT_FORMAT;
        CellStyle n = wb.createCellStyle();
        n.setDataFormat(wb.createDataFormat().getFormat(fmt));
        numbers.put(t, n);
        CellStyle bn = wb.createCellStyle();
        bn.cloneStyleFrom(n);
        bn.setFont(boldFont);
        boldNumbers.put(t, bn);
      }
    }
  }
}
