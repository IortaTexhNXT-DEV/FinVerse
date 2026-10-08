package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.excel.SheetColumnWidths;
import com.iortatechnxt.brokerverse.common.excel.SheetLogo;
import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.office.PdfBrandFooter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * The User Access Matrix workbook (DO-06) laid out for a reader: the BDO Insure report header
 * (logo, company, title, run by and run date, what the cells mean), the heading row with the group
 * profile codes turned upright, the heading row and the first columns frozen, a filter on every
 * column, columns as wide as their content and a print layout that repeats the headings and first
 * columns on every page.
 */
final class AccessMatrixWorkbook {

  /** Header block of a sheet. */
  record Header(String company, String title, String runLine, String legend) {}

  /** A sheet: the first (frozen) columns, then one column per group profile. */
  record Grid(
      String name,
      Header header,
      List<String> leading,
      List<String> profiles,
      List<List<Object>> rows,
      boolean wrapProfiles) {}

  private static final float LOGO_HEIGHT_POINTS = 22f;
  private static final float LOGO_ROW_POINTS = 30f;
  private static final short TITLE_POINTS = 12;
  private static final short UPRIGHT = 90;
  private static final int PROFILE_CHARS = 4;
  private static final int WRAPPED_PROFILE_CHARS = 18;
  private static final float POINTS_PER_UPRIGHT_CHAR = 6.5f;
  private static final float HEADING_PADDING_POINTS = 12f;

  private AccessMatrixWorkbook() {}

  /**
   * Writes the workbook.
   *
   * @param grids sheets in order
   * @return xlsx bytes
   */
  static byte[] write(List<Grid> grids) {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Styles styles = new Styles(wb);
      for (Grid grid : grids) {
        sheet(wb, grid, styles);
      }
      wb.write(out);
      return out.toByteArray();
    } catch (IOException ex) {
      throw new UncheckedIOException("Excel rendering failed", ex);
    }
  }

  private static void sheet(XSSFWorkbook wb, Grid g, Styles s) {
    Sheet sheet = wb.createSheet(g.name());
    int lead = g.leading().size();
    int columns = lead + g.profiles().size();
    int r = header(sheet, g.header(), s);
    int headRow = r;
    Row head = sheet.createRow(r++);
    for (int c = 0; c < lead; c++) {
      text(head, c, g.leading().get(c), s.head);
    }
    int longest = 0;
    for (int p = 0; p < g.profiles().size(); p++) {
      text(head, lead + p, g.profiles().get(p), s.upright);
      longest = Math.max(longest, g.profiles().get(p).length());
    }
    head.setHeightInPoints(longest * POINTS_PER_UPRIGHT_CHAR + HEADING_PADDING_POINTS);
    for (List<Object> values : g.rows()) {
      values(sheet.createRow(r++), values, lead, g.wrapProfiles(), s);
    }
    // Widths of the first columns from their content; profile columns narrow (Y marks) or, for
    // lists of permissions, wide enough to wrap.
    SheetColumnWidths.fit(sheet, headRow, lead);
    int profileWidth =
        SheetColumnWidths.units(g.wrapProfiles() ? WRAPPED_PROFILE_CHARS : PROFILE_CHARS);
    for (int c = lead; c < columns; c++) {
      sheet.setColumnWidth(c, profileWidth);
    }
    logo(wb, sheet);
    sheet.createFreezePane(lead, headRow + 1);
    sheet.setAutoFilter(new CellRangeAddress(headRow, Math.max(headRow, r - 1), 0, columns - 1));
    printSetup(sheet, g, headRow, lead);
  }

  private static void values(Row row, List<Object> values, int lead, boolean wrap, Styles s) {
    for (int c = 0; c < values.size(); c++) {
      Object v = values.get(c);
      Cell cell = row.createCell(c);
      if (v instanceof Number n) {
        cell.setCellValue(n.doubleValue());
        cell.setCellStyle(s.mark);
      } else {
        cell.setCellValue(v == null ? "" : v.toString());
        cell.setCellStyle(c < lead ? s.body : wrap ? s.wrapped : s.mark);
      }
    }
  }

  /** Logo, company, title, run line and legend; returns the row of the headings. */
  private static int header(Sheet sheet, Header h, Styles s) {
    sheet.createRow(0).setHeightInPoints(LOGO_ROW_POINTS);
    int r = 1;
    text(sheet.createRow(r++), 0, h.company(), s.title);
    text(sheet.createRow(r++), 0, h.title(), s.title);
    text(sheet.createRow(r++), 0, h.runLine(), s.meta);
    text(sheet.createRow(r++), 0, h.legend(), s.meta);
    return r + 1;
  }

  /** The BDO Insure logo in the first row, at the height of the other report headers. */
  private static void logo(XSSFWorkbook wb, Sheet sheet) {
    SheetLogo.place(wb, sheet, LOGO_HEIGHT_POINTS);
  }

  private static void printSetup(Sheet sheet, Grid g, int headRow, int lead) {
    PrintSetup setup = sheet.getPrintSetup();
    setup.setPaperSize(PrintSetup.A4_PAPERSIZE);
    setup.setLandscape(true);
    sheet.setRepeatingRows(new CellRangeAddress(headRow, headRow, -1, -1));
    sheet.setRepeatingColumns(new CellRangeAddress(-1, -1, 0, lead - 1));
    sheet.getFooter().setLeft(PdfBrandFooter.classified(""));
    sheet
        .getFooter()
        .setRight(BrandAssets.SYSTEM_NAME + " | " + g.header().title() + " | Page &P of &N");
  }

  private static void text(Row row, int col, String value, CellStyle style) {
    Cell cell = row.createCell(col);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private static XSSFColor color(String hex) {
    Color c = BrandAssets.color(hex);
    return new XSSFColor(
        new byte[] {(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue()},
        new DefaultIndexedColorMap());
  }

  /** Cell styles of the workbook. */
  private static final class Styles {
    private final CellStyle title;
    private final CellStyle meta;
    private final CellStyle head;
    private final CellStyle upright;
    private final CellStyle body;
    private final CellStyle mark;
    private final CellStyle wrapped;

    Styles(XSSFWorkbook wb) {
      XSSFColor brand = color(BrandAssets.HEADER);
      XSSFFont titleFont = font(wb);
      titleFont.setBold(true);
      titleFont.setFontHeightInPoints(TITLE_POINTS);
      titleFont.setColor(brand);
      title = wb.createCellStyle();
      title.setFont(titleFont);
      meta = wb.createCellStyle();
      meta.setFont(font(wb));
      XSSFFont headFont = font(wb);
      headFont.setBold(true);
      headFont.setColor(color("FFFFFF"));
      head = heading(wb, headFont, brand);
      head.setIndention(SheetColumnWidths.TEXT_INDENT);
      XSSFCellStyle up = heading(wb, headFont, brand);
      up.setRotation(UPRIGHT);
      up.setAlignment(HorizontalAlignment.CENTER);
      up.setVerticalAlignment(VerticalAlignment.BOTTOM);
      upright = up;
      body = bordered(wb);
      body.setIndention(SheetColumnWidths.TEXT_INDENT);
      mark = bordered(wb);
      mark.setAlignment(HorizontalAlignment.CENTER);
      wrapped = bordered(wb);
      wrapped.setWrapText(true);
    }

    private static XSSFFont font(XSSFWorkbook wb) {
      XSSFFont f = wb.createFont();
      f.setFontName(BrandAssets.FONT);
      return f;
    }

    private static XSSFCellStyle heading(XSSFWorkbook wb, XSSFFont font, XSSFColor fill) {
      XSSFCellStyle style = wb.createCellStyle();
      style.setFont(font);
      style.setFillForegroundColor(fill);
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      style.setVerticalAlignment(VerticalAlignment.BOTTOM);
      style.setWrapText(true);
      return style;
    }

    private static XSSFCellStyle bordered(XSSFWorkbook wb) {
      XSSFCellStyle style = wb.createCellStyle();
      style.setFont(font(wb));
      style.setVerticalAlignment(VerticalAlignment.TOP);
      style.setBorderBottom(BorderStyle.HAIR);
      style.setBottomBorderColor(color(BrandAssets.GRID));
      return style;
    }
  }
}
