package com.iortatechnxt.brokerverse.common.excel;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuidedWorkbook.Placed;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Writes the fill-in templates of the platform as guided sheets (client standard): every data sheet
 * is self-contained, with a title block (what the file is for, who fills it in, how it is uploaded,
 * the rules of the file), a column guide band directly above the header row (Mandatory, Format,
 * Allowed values, What to enter), the header row with a note on every header, the example row(s)
 * marked to overwrite or delete, drop-downs from a visible Lists sheet, cell checks with input
 * messages, frozen panes and print setup. A template of several sheets starts with a "Start here"
 * sheet giving the order with links.
 *
 * <p>Column A carries the labels of the guide (and the example marker); the data columns start in
 * column B. The readers find the header row after the band and skip the example rows ({@link
 * GuidedTables}), so the file is uploaded as it is.
 */
public final class GuidedTemplateWriter {

  /** First sheet column of the data (column A holds the guide labels). */
  static final int FIRST_COLUMN = 1;

  /** Name of the first sheet of a template with several data sheets. */
  public static final String START_SHEET = "Start here";

  /** Last rule of every template. */
  public static final String AS_IS_RULE = "Upload the file as is – the guide rows are skipped.";

  private static final int LABEL_WIDTH = 24;
  private static final int MIN_INFO_SPAN = 6;
  static final float LINE_POINTS = 12.5f;
  private static final float TITLE_POINTS = 24f;
  static final int CHARACTER = 256;
  private static final int HEADER_PADDING = 4;
  private static final int MIN_WIDTH = 16;
  private static final int MAX_WIDTH = 30;
  private static final int START_SHEET_WIDTH = 40;
  private static final int START_TEXT_WIDTH = 80;
  private static final double CHARS_PER_WIDTH = 1.3;

  private GuidedTemplateWriter() {}

  /**
   * Writes a template.
   *
   * @param template template
   * @return xlsx bytes
   */
  public static byte[] write(GuidedTemplate template) {
    try (GuidedWorkbook wb = open(template)) {
      return wb.bytes();
    }
  }

  /**
   * Lays out a template and keeps the workbook open for data rows.
   *
   * @param template template
   * @return the workbook; close it
   */
  public static GuidedWorkbook open(GuidedTemplate template) {
    XSSFWorkbook wb = new XSSFWorkbook();
    GuidedStyles styles = new GuidedStyles(wb);
    GuidedLists lists = new GuidedLists(template.sheets());
    boolean multi = template.sheets().size() > 1;
    if (multi) {
      startSheet(wb, styles, template);
    }
    List<Placed> placed = new ArrayList<>();
    for (int i = 0; i < template.sheets().size(); i++) {
      placed.add(dataSheet(wb, styles, lists, template, i));
    }
    lists.write(wb, styles);
    wb.setActiveSheet(0);
    wb.getProperties().getCoreProperties().setTitle(template.name());
    wb.getProperties().getCoreProperties().setCreator("BIBS");
    return new GuidedWorkbook(wb, styles, placed);
  }

  private static Placed dataSheet(
      XSSFWorkbook wb, GuidedStyles styles, GuidedLists lists, GuidedTemplate t, int index) {
    GuidedSheet gs = t.sheets().get(index);
    Sheet sheet = wb.createSheet(gs.name());
    List<GuideColumn> columns = gs.columns();
    sheet.setColumnWidth(0, LABEL_WIDTH * CHARACTER);
    for (int c = 0; c < columns.size(); c++) {
      sheet.setColumnWidth(FIRST_COLUMN + c, widthOf(columns.get(c)) * CHARACTER);
      sheet.setDefaultColumnStyle(FIRST_COLUMN + c, styles.data(columns.get(c).kind(), false));
    }
    int span = Math.max(columns.size(), MIN_INFO_SPAN);
    int r = titleBlock(sheet, styles, t, index, span);
    r++;
    int bandStart = r;
    r = GuidedBand.band(sheet, styles, columns, r);
    int headerRow = r;
    GuidedBand.headerRow(sheet, styles, columns, headerRow);
    int firstData = examples(sheet, styles, gs, headerRow + 1);
    GuidedChecks.apply(sheet, columns, lists, headerRow + 1, headerRow + t.maxRows());
    sheet.createFreezePane(FIRST_COLUMN, headerRow + 1);
    sheet.setActiveCell(new CellAddress(firstData, FIRST_COLUMN));
    printSetup(sheet, t, bandStart, headerRow);
    return new Placed(sheet, headerRow, firstData, columns);
  }

  private static int widthOf(GuideColumn c) {
    if (c.width() > 0) {
      return c.width();
    }
    return Math.clamp(c.shownHeader().length() + (long) HEADER_PADDING, MIN_WIDTH, MAX_WIDTH);
  }

  /** Title and information rows; returns the next free row. */
  private static int titleBlock(
      Sheet sheet, GuidedStyles styles, GuidedTemplate t, int index, int span) {
    GuidedSheet gs = t.sheets().get(index);
    boolean multi = t.sheets().size() > 1;
    Row title = sheet.createRow(0);
    title.setHeightInPoints(TITLE_POINTS);
    text(title, 0, multi ? t.name() + " – " + gs.title() : t.name(), styles.title());
    int r = 1;
    List<String[]> info = new ArrayList<>();
    if (multi) {
      info.add(new String[] {"This sheet", gs.intro()});
      info.add(
          new String[] {
            "Order",
            "Sheet "
                + (index + 1)
                + " of "
                + t.sheets().size()
                + ". The order, who fills the file in and how it is uploaded are on the "
                + START_SHEET
                + " sheet."
          });
      info.add(new String[] {"Rules", AS_IS_RULE});
    } else {
      info.add(new String[] {"What it is for", join(t.purpose(), gs.intro())});
      info.add(new String[] {"Who fills it in", t.filledBy()});
      info.add(new String[] {"How to upload", t.howToUpload()});
      info.addAll(ruleRows(t.rules()));
    }
    for (String[] line : info) {
      if (!line[1].isBlank()) {
        infoRow(sheet, styles, r++, line[0], line[1], span);
      }
    }
    return r;
  }

  private static List<String[]> ruleRows(List<String> rules) {
    List<String[]> rows = new ArrayList<>();
    List<String> all = new ArrayList<>(rules);
    all.add(AS_IS_RULE);
    for (int i = 0; i < all.size(); i++) {
      rows.add(new String[] {i == 0 ? "Rules" : "", "• " + all.get(i)});
    }
    return rows;
  }

  private static String join(String a, String b) {
    if (b.isBlank()) {
      return a;
    }
    return a.isBlank() ? b : a + " " + b;
  }

  private static void infoRow(
      Sheet sheet, GuidedStyles styles, int r, String label, String value, int span) {
    Row row = sheet.createRow(r);
    text(row, 0, label, styles.infoLabel());
    text(row, FIRST_COLUMN, value, styles.info());
    int last = FIRST_COLUMN + span - 1;
    if (last > FIRST_COLUMN) {
      sheet.addMergedRegion(new CellRangeAddress(r, r, FIRST_COLUMN, last));
    }
    int chars = 0;
    for (int c = FIRST_COLUMN; c <= last; c++) {
      chars += sheet.getColumnWidth(c) / CHARACTER;
    }
    row.setHeightInPoints(LINE_POINTS * lines(value, chars) + 2);
  }

  /** Example rows; returns the first row after them. */
  private static int examples(Sheet sheet, GuidedStyles styles, GuidedSheet gs, int from) {
    int r = from;
    for (List<String> values : gs.examples()) {
      Row row = sheet.createRow(r++);
      text(row, 0, GuidedTables.EXAMPLE_MARKER, styles.exampleLabel());
      for (int c = 0; c < gs.columns().size(); c++) {
        Kind kind = gs.columns().get(c).kind();
        String v = c < values.size() && values.get(c) != null ? values.get(c) : "";
        Cell cell = row.createCell(FIRST_COLUMN + c);
        cell.setCellStyle(styles.data(kind, true));
        value(cell, kind, v);
      }
    }
    return r;
  }

  /**
   * Writes a value into a data cell with its kind: a date as a date cell, a number as a number,
   * anything else (or a value that does not parse) as text.
   *
   * @param cell cell
   * @param kind kind
   * @param value value (ISO date for dates)
   */
  public static void value(Cell cell, Kind kind, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    try {
      switch (kind) {
        case DATE -> cell.setCellValue(LocalDate.parse(value.strip()));
        case NUMBER, AMOUNT, INTEGER ->
            cell.setCellValue(new BigDecimal(value.strip().replace(",", "")).doubleValue());
        default -> cell.setCellValue(value);
      }
    } catch (DateTimeParseException | NumberFormatException e) {
      cell.setCellValue(value);
    }
  }

  private static void startSheet(XSSFWorkbook wb, GuidedStyles styles, GuidedTemplate t) {
    Sheet sheet = wb.createSheet(START_SHEET);
    sheet.setColumnWidth(0, LABEL_WIDTH * CHARACTER);
    sheet.setColumnWidth(FIRST_COLUMN, START_SHEET_WIDTH * CHARACTER);
    sheet.setColumnWidth(FIRST_COLUMN + 1, START_TEXT_WIDTH * CHARACTER);
    Row title = sheet.createRow(0);
    title.setHeightInPoints(TITLE_POINTS);
    text(title, 0, t.name(), styles.title());
    int r = 1;
    infoRow(sheet, styles, r++, "What it is for", t.purpose(), 2);
    infoRow(sheet, styles, r++, "Who fills it in", t.filledBy(), 2);
    infoRow(sheet, styles, r++, "How to upload", t.howToUpload(), 2);
    for (String[] rule : ruleRows(t.rules())) {
      infoRow(sheet, styles, r++, rule[0], rule[1], 2);
    }
    r++;
    Row head = sheet.createRow(r++);
    text(head, 0, "Step", styles.header());
    text(head, FIRST_COLUMN, "Sheet", styles.header());
    text(head, FIRST_COLUMN + 1, "What it holds", styles.header());
    for (int i = 0; i < t.sheets().size(); i++) {
      GuidedSheet gs = t.sheets().get(i);
      Row row = sheet.createRow(r++);
      text(row, 0, String.valueOf(i + 1), styles.info());
      Cell link = text(row, FIRST_COLUMN, gs.title(), styles.link());
      XSSFHyperlink h = wb.getCreationHelper().createHyperlink(HyperlinkType.DOCUMENT);
      h.setAddress("'" + gs.name().replace("'", "''") + "'!A1");
      link.setHyperlink(h);
      text(row, FIRST_COLUMN + 1, gs.intro(), styles.info());
      row.setHeightInPoints(LINE_POINTS * lines(gs.intro(), START_TEXT_WIDTH) + 2);
    }
    sheet.setActiveCell(new CellAddress(0, 0));
    PrintSetup ps = sheet.getPrintSetup();
    ps.setLandscape(true);
    ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
    ps.setFitWidth((short) 1);
    ps.setFitHeight((short) 0);
    sheet.setFitToPage(true);
  }

  private static void printSetup(Sheet sheet, GuidedTemplate t, int bandStart, int headerRow) {
    PrintSetup ps = sheet.getPrintSetup();
    ps.setLandscape(true);
    ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
    ps.setFitWidth((short) 1);
    ps.setFitHeight((short) 0);
    sheet.setFitToPage(true);
    sheet.setAutobreaks(true);
    sheet.setRepeatingRows(new CellRangeAddress(bandStart, headerRow, -1, -1));
    sheet.getHeader().setLeft(t.name());
    sheet.getFooter().setCenter("Page &P of &N");
  }

  static Cell text(Row row, int column, String value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value);
    cell.setCellStyle(style);
    return cell;
  }

  /**
   * Estimated number of lines of a wrapped text in a width.
   *
   * @param text text
   * @param width width in characters
   * @return lines, at least 1
   */
  static int lines(String text, int width) {
    int perLine = Math.max(1, (int) (width * CHARS_PER_WIDTH));
    int lines = 0;
    for (String part : text.split("\n", -1)) {
      lines += Math.max(1, (part.length() + perLine - 1) / perLine);
    }
    return Math.max(1, lines);
  }
}
