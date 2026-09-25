package com.iortatechnxt.brokerverse.report.render;

import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.office.PdfBrandFooter;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import java.awt.Color;
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
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.springframework.stereotype.Component;

/**
 * Excel (XLSX) export with typed numeric/date cells, frozen header and styled group rows, in the
 * BDO Insure brand (client requirement 16): the logo above the title block, Header Blue headings,
 * Arial, and a print layout that follows the print options with the "Confidential" footer and "Page
 * x of y".
 *
 * <p>Uses the streaming workbook so very large reports do not exhaust memory.
 */
@Component
public class XlsxReportRenderer implements ReportRenderer {

  private static final int WINDOW = 200;
  private static final short TITLE_POINTS = 12;
  private static final float LOGO_ROW_POINTS = 30f;
  // Excel header / footer codes of the page number and the page count.
  private static final String PAGE_CODE = "&P";
  private static final String PAGES_CODE = "&N";
  private static final double LOGO_POINTS = 22;
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
      logo(wb, sheet);
      int r = writeHeader(sheet, result, context, styles);
      int headerRow = r;
      Row head = sheet.createRow(r++);
      text(head, 0, "", styles.head);
      // Column widths in column order: Excel refuses a sheet whose column list is not sorted.
      sheet.setColumnWidth(0, LABEL_WIDTH);
      List<ReportColumn> cols = result.columns();
      for (int i = 0; i < cols.size(); i++) {
        text(head, i + 1, cols.get(i).label(), styles.head);
        sheet.setColumnWidth(
            i + 1, cols.get(i).type() == ColumnType.TEXT ? TEXT_WIDTH : NUMBER_WIDTH);
      }
      sheet.createFreezePane(1, headerRow + 1);
      printSetup(sheet, result, context, headerRow);
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

  /** The BDO Insure logo in the first row, above the company. */
  private static void logo(SXSSFWorkbook wb, Sheet sheet) {
    Row row = sheet.createRow(0);
    row.setHeightInPoints(LOGO_ROW_POINTS);
    int picture = wb.addPicture(BrandAssets.logoPng(), Workbook.PICTURE_TYPE_PNG);
    ClientAnchor anchor = wb.getCreationHelper().createClientAnchor();
    anchor.setCol1(0);
    anchor.setRow1(0);
    anchor.setCol2(0);
    anchor.setRow2(0);
    anchor.setDx2(Units.toEMU(LOGO_POINTS * BrandAssets.LOGO_RATIO));
    anchor.setDy2(Units.toEMU(LOGO_POINTS));
    anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
    sheet.createDrawingPatriarch().createPicture(anchor, picture);
  }

  /**
   * Printing as in the PDF (FRBS 2.4.9): paper, orientation, fit to width, the heading row on every
   * page and the footer with the classification, footer text and "Page x of y".
   */
  private static void printSetup(
      Sheet sheet, ReportResult result, ReportContext ctx, int headerRow) {
    PrintOptions print = ctx.print();
    PrintSetup setup = sheet.getPrintSetup();
    setup.setPaperSize(
        switch (print.paper()) {
          case A4 -> PrintSetup.A4_PAPERSIZE;
          case LETTER -> PrintSetup.LETTER_PAPERSIZE;
          case LEGAL -> PrintSetup.LEGAL_PAPERSIZE;
          case A3 -> PrintSetup.A3_PAPERSIZE;
        });
    setup.setLandscape(print.landscape(result.columns().size()));
    if (print.fitToWidth()) {
      sheet.setFitToPage(true);
      setup.setFitWidth((short) 1);
      setup.setFitHeight((short) 0);
    }
    sheet.setRepeatingRows(new CellRangeAddress(headerRow, headerRow, -1, -1));
    Footer footer = sheet.getFooter();
    footer.setLeft(PdfBrandFooter.classified(ctx.footerText()));
    footer.setRight(
        "iNXT BrokerVerse | " + result.code() + " | Page " + PAGE_CODE + " of " + PAGES_CODE);
  }

  private static int writeHeader(Sheet sheet, ReportResult result, ReportContext ctx, Styles s) {
    int r = 1;
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

  private static Font font(SXSSFWorkbook wb) {
    Font f = wb.createFont();
    f.setFontName(BrandAssets.FONT);
    return f;
  }

  private static XSSFColor brandColor(String hex) {
    Color c = BrandAssets.color(hex);
    return new XSSFColor(
        new byte[] {(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue()},
        new DefaultIndexedColorMap());
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
      XSSFColor brand = brandColor(BrandAssets.HEADER_BLUE);
      XSSFFont titleFont = (XSSFFont) font(wb);
      titleFont.setBold(true);
      titleFont.setFontHeightInPoints(TITLE_POINTS);
      titleFont.setColor(brand);
      title = wb.createCellStyle();
      title.setFont(titleFont);
      meta = wb.createCellStyle();
      meta.setFont(font(wb));
      Font headFont = font(wb);
      headFont.setBold(true);
      headFont.setColor(IndexedColors.WHITE.getIndex());
      XSSFCellStyle headStyle = (XSSFCellStyle) wb.createCellStyle();
      headStyle.setFont(headFont);
      headStyle.setFillForegroundColor(brand);
      headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headStyle.setBorderBottom(BorderStyle.THIN);
      head = headStyle;
      Font bodyFont = font(wb);
      body = wb.createCellStyle();
      body.setFont(bodyFont);
      Font boldFont = font(wb);
      boldFont.setBold(true);
      bold = wb.createCellStyle();
      bold.setFont(boldFont);
      date = wb.createCellStyle();
      date.setFont(bodyFont);
      date.setDataFormat(wb.createDataFormat().getFormat("dd-mm-yyyy"));
      for (ColumnType t : ColumnType.values()) {
        String fmt = t == ColumnType.NUMBER ? "#,##0" : AMOUNT_FORMAT;
        CellStyle n = wb.createCellStyle();
        n.setFont(bodyFont);
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
