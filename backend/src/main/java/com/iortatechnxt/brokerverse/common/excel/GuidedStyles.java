package com.iortatechnxt.brokerverse.common.excel;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.util.EnumMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Cell styles of a guided template, created once per workbook. */
final class GuidedStyles {

  private static final byte[] NAVY = {0x1F, 0x3A, 0x5F};
  private static final byte[] BAND = {(byte) 0xEE, (byte) 0xF2, (byte) 0xF7};
  private static final byte[] LABEL = {(byte) 0xD9, (byte) 0xE1, (byte) 0xEC};
  private static final byte[] MANDATORY = {(byte) 0xFC, (byte) 0xE4, (byte) 0xD6};
  private static final byte[] GREY = {(byte) 0x80, (byte) 0x80, (byte) 0x80};
  private static final byte[] EXAMPLE_FILL = {(byte) 0xF2, (byte) 0xF2, (byte) 0xF2};
  private static final byte[] ERROR_FILL = {(byte) 0xFF, (byte) 0xE0, (byte) 0xE0};
  private static final byte[] ERROR_TEXT = {(byte) 0x9C, 0x00, 0x06};
  private static final byte[] LINK = {0x05, 0x63, (byte) 0xC1};
  private static final byte[] BORDER = {(byte) 0xBF, (byte) 0xC9, (byte) 0xD6};
  private static final short TITLE_POINTS = 16;
  private static final short SMALL_POINTS = 9;

  private final XSSFWorkbook wb;
  private final Map<Kind, CellStyle> plain = new EnumMap<>(Kind.class);
  private final Map<Kind, CellStyle> examples = new EnumMap<>(Kind.class);
  private final CellStyle title;
  private final CellStyle infoLabel;
  private final CellStyle info;
  private final CellStyle bandLabel;
  private final CellStyle band;
  private final CellStyle bandMandatory;
  private final CellStyle header;
  private final CellStyle headerCorner;
  private final CellStyle exampleLabel;
  private final CellStyle listHead;
  private final CellStyle link;
  private final CellStyle marked;
  private final CellStyle markedDate;

  GuidedStyles(XSSFWorkbook wb) {
    this.wb = wb;
    title = style(font(true, TITLE_POINTS, NAVY, false), null, false);
    infoLabel = style(font(true, SMALL_POINTS, NAVY, false), null, true);
    info = style(font(false, SMALL_POINTS, null, false), null, true);
    bandLabel = bordered(style(font(true, SMALL_POINTS, NAVY, false), LABEL, true));
    band = bordered(style(font(false, SMALL_POINTS, null, false), BAND, true));
    bandMandatory = bordered(style(font(true, SMALL_POINTS, null, false), MANDATORY, true));
    header = bordered(style(font(true, (short) 0, new byte[] {-1, -1, -1}, false), NAVY, true));
    headerCorner = bordered(style(font(false, SMALL_POINTS, null, false), LABEL, true));
    exampleLabel = style(font(false, SMALL_POINTS, GREY, true), EXAMPLE_FILL, false);
    listHead = bordered(style(font(true, (short) 0, new byte[] {-1, -1, -1}, false), NAVY, false));
    XSSFFont linkFont = font(false, (short) 0, LINK, false);
    linkFont.setUnderline(Font.U_SINGLE);
    link = style(linkFont, null, false);
    marked = style(font(false, (short) 0, ERROR_TEXT, false), ERROR_FILL, true);
    markedDate = style(font(false, (short) 0, ERROR_TEXT, false), ERROR_FILL, false);
    markedDate.setDataFormat(wb.createDataFormat().getFormat(DisplayFormat.SHEET_DATE_FORMAT));
  }

  /**
   * The style of a data cell of a kind (text keeps leading zeros; dates show as dd-MMM-yyyy).
   *
   * @param kind kind
   * @param example grey italic example style
   * @return style
   */
  CellStyle data(Kind kind, boolean example) {
    Map<Kind, CellStyle> cache = example ? examples : plain;
    return cache.computeIfAbsent(kind, k -> newData(k, example));
  }

  private CellStyle newData(Kind kind, boolean example) {
    XSSFCellStyle s =
        example
            ? style(font(false, (short) 0, GREY, true), EXAMPLE_FILL, false)
            : wb.createCellStyle();
    s.setDataFormat(wb.createDataFormat().getFormat(formatOf(kind)));
    s.setVerticalAlignment(VerticalAlignment.TOP);
    return s;
  }

  private static String formatOf(Kind kind) {
    return switch (kind) {
      case DATE -> DisplayFormat.SHEET_DATE_FORMAT;
      case AMOUNT -> "0.00";
      case INTEGER -> "0";
      case NUMBER -> "General";
      case TEXT, LIST, YES_NO -> "@";
    };
  }

  private XSSFFont font(boolean bold, short points, byte[] rgb, boolean italic) {
    XSSFFont f = wb.createFont();
    f.setBold(bold);
    f.setItalic(italic);
    if (points > 0) {
      f.setFontHeightInPoints(points);
    }
    if (rgb != null) {
      f.setColor(new XSSFColor(rgb));
    }
    return f;
  }

  private XSSFCellStyle style(XSSFFont font, byte[] fill, boolean wrap) {
    XSSFCellStyle s = wb.createCellStyle();
    s.setFont(font);
    if (fill != null) {
      s.setFillForegroundColor(new XSSFColor(fill));
      s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }
    s.setWrapText(wrap);
    s.setVerticalAlignment(VerticalAlignment.TOP);
    s.setAlignment(HorizontalAlignment.LEFT);
    return s;
  }

  private static XSSFCellStyle bordered(XSSFCellStyle s) {
    s.setBorderBottom(BorderStyle.THIN);
    s.setBorderTop(BorderStyle.THIN);
    s.setBorderLeft(BorderStyle.THIN);
    s.setBorderRight(BorderStyle.THIN);
    XSSFColor line = new XSSFColor(BORDER);
    s.setBottomBorderColor(line);
    s.setTopBorderColor(line);
    s.setLeftBorderColor(line);
    s.setRightBorderColor(line);
    return s;
  }

  CellStyle title() {
    return title;
  }

  CellStyle infoLabel() {
    return infoLabel;
  }

  CellStyle info() {
    return info;
  }

  CellStyle bandLabel() {
    return bandLabel;
  }

  CellStyle band() {
    return band;
  }

  CellStyle bandMandatory() {
    return bandMandatory;
  }

  CellStyle header() {
    return header;
  }

  CellStyle headerCorner() {
    return headerCorner;
  }

  CellStyle exampleLabel() {
    return exampleLabel;
  }

  CellStyle listHead() {
    return listHead;
  }

  CellStyle link() {
    return link;
  }

  CellStyle marked() {
    return marked;
  }

  CellStyle markedDate() {
    return markedDate;
  }
}
