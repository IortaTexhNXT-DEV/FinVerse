package com.iortatechnxt.brokerverse.common.office;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.List;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

/**
 * A Word (DOCX) file in the BDO Insure layout (client requirement 16): page size and orientation, a
 * page header with the logo and the company, a page footer with the "Confidential" classification,
 * the footer text and "Page x of y", and branded tables with a Header Blue heading row repeated on
 * every page. Used by the report platform ({@code DocxReportRenderer}) and by the business
 * documents ({@code DocumentComposer.docx}).
 */
public final class BrandedDocx {

  /** Twips (1/20 point) per point. */
  public static final int TWIPS_PER_POINT = 20;

  private static final int PERCENT_FULL_WIDTH = 5000;
  private static final int GRID_SIZE = 4;
  private static final int GOLD_RULE_SIZE = 12;
  private static final int CELL_MARGIN = 60;
  private static final double LOGO_HEIGHT_POINTS = 22;
  private static final double FOOTER_SIZE = 7;
  private static final String GREY = "808080";
  private static final String SEPARATOR = "  |  ";

  private final XWPFDocument doc = new XWPFDocument();
  private final Page page;

  /**
   * Creates an empty document with the page setup.
   *
   * @param page paper and margins
   * @param title document title (file properties)
   */
  public BrandedDocx(Page page, String title) {
    this.page = page;
    CTSectPr sect = doc.getDocument().getBody().addNewSectPr();
    CTPageSz size = sect.addNewPgSz();
    size.setW(twips(page.landscape() ? page.height() : page.width()));
    size.setH(twips(page.landscape() ? page.width() : page.height()));
    if (page.landscape()) {
      size.setOrient(STPageOrientation.LANDSCAPE);
    }
    CTPageMar margins = sect.addNewPgMar();
    BigInteger margin = twips(page.margin());
    margins.setTop(margin);
    margins.setBottom(margin);
    margins.setLeft(margin);
    margins.setRight(margin);
    margins.setHeader(twips(page.margin() / 2));
    margins.setFooter(twips(page.margin() / 2));
    CTFonts fonts = CTFonts.Factory.newInstance();
    fonts.setAscii(BrandAssets.FONT);
    fonts.setHAnsi(BrandAssets.FONT);
    fonts.setCs(BrandAssets.FONT);
    doc.createStyles().setDefaultFonts(fonts);
    doc.getProperties().getCoreProperties().setTitle(title);
    doc.getProperties().getCoreProperties().setCreator("iNXT BrokerVerse");
  }

  /**
   * Width available to the content, between the margins.
   *
   * @return twips
   */
  public long contentWidth() {
    return page.contentWidth();
  }

  /**
   * The page header of every page: the BDO Insure logo and the company.
   *
   * @param companyName company, blank for none
   */
  public void pageHeader(String companyName) {
    XWPFHeader header = doc.createHeader(HeaderFooterType.DEFAULT);
    XWPFParagraph logo = header.createParagraph();
    try {
      logo.createRun()
          .addPicture(
              new ByteArrayInputStream(BrandAssets.logoPng()),
              Document.PICTURE_TYPE_PNG,
              "bdo-insure.png",
              Units.toEMU(LOGO_HEIGHT_POINTS * BrandAssets.LOGO_RATIO),
              Units.toEMU(LOGO_HEIGHT_POINTS));
    } catch (IOException | InvalidFormatException ex) {
      throw new IllegalStateException("The brand logo cannot be placed", ex);
    }
    if (companyName != null && !companyName.isBlank()) {
      style(logo.createRun(), "   " + companyName, TextStyle.COMPANY);
    }
    goldRule(logo);
  }

  /**
   * The page footer of every page: classification and footer text, then "prefix | Page x of y".
   *
   * @param footerText administrator's footer text, blank for none
   * @param prefix text before the page number (e.g. the report code), blank for none
   */
  public void pageFooter(String footerText, String prefix) {
    XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);
    XWPFParagraph left = footer.createParagraph();
    style(left.createRun(), PdfBrandFooter.classified(footerText), TextStyle.FOOTER);
    XWPFParagraph right = footer.createParagraph();
    right.setAlignment(ParagraphAlignment.RIGHT);
    String lead = prefix == null || prefix.isBlank() ? "" : prefix + SEPARATOR;
    style(right.createRun(), lead + "Page ", TextStyle.FOOTER);
    field(right, "PAGE");
    style(right.createRun(), " of ", TextStyle.FOOTER);
    field(right, "NUMPAGES");
  }

  /**
   * Adds a paragraph.
   *
   * @param text text; line breaks become Word line breaks
   * @param textStyle style
   * @return paragraph
   */
  public XWPFParagraph paragraph(String text, TextStyle textStyle) {
    XWPFParagraph p = doc.createParagraph();
    p.setSpacingAfter(textStyle.spacingAfter());
    style(p.createRun(), text, textStyle);
    return p;
  }

  /**
   * Adds a paragraph with a gold rule below (letterhead).
   *
   * @param text text
   * @param textStyle style
   */
  public void ruledParagraph(String text, TextStyle textStyle) {
    goldRule(paragraph(text, textStyle));
  }

  /**
   * Adds a table with one heading row in Header Blue, repeated on every page when {@code headings}
   * is not null.
   *
   * @param weights relative column widths
   * @param fitToWidth true: the table fills the content width; false: natural widths
   * @param naturalTwipsPerWeight twips per weight unit for natural widths
   * @return table (its first row is the heading row, or an empty row to fill)
   */
  public XWPFTable table(double[] weights, boolean fitToWidth, double naturalTwipsPerWeight) {
    double total = 0;
    for (double w : weights) {
      total += w;
    }
    long width = page.contentWidth();
    boolean fits = fitToWidth || total * naturalTwipsPerWeight >= width;
    long tableWidth = fits ? width : Math.round(total * naturalTwipsPerWeight);
    XWPFTable table = doc.createTable(1, weights.length);
    CTTblWidth tblW = table.getCTTbl().getTblPr().getTblW();
    if (fits) {
      tblW.setType(STTblWidth.PCT);
      tblW.setW(BigInteger.valueOf(PERCENT_FULL_WIDTH));
    } else {
      tblW.setType(STTblWidth.DXA);
      tblW.setW(BigInteger.valueOf(tableWidth));
    }
    CTTblGrid grid =
        table.getCTTbl().getTblGrid() == null
            ? table.getCTTbl().addNewTblGrid()
            : table.getCTTbl().getTblGrid();
    while (grid.sizeOfGridColArray() > 0) {
      grid.removeGridCol(0);
    }
    for (int c = 0; c < weights.length; c++) {
      long colWidth = Math.round(tableWidth * weights[c] / total);
      grid.addNewGridCol().setW(BigInteger.valueOf(colWidth));
      table.getRow(0).getCell(c).setWidth(String.valueOf(colWidth));
    }
    table.setTopBorder(XWPFBorderType.SINGLE, GRID_SIZE, 0, BrandAssets.GRID);
    table.setBottomBorder(XWPFBorderType.SINGLE, GRID_SIZE, 0, BrandAssets.GRID);
    table.setLeftBorder(XWPFBorderType.SINGLE, GRID_SIZE, 0, BrandAssets.GRID);
    table.setRightBorder(XWPFBorderType.SINGLE, GRID_SIZE, 0, BrandAssets.GRID);
    table.setInsideHBorder(XWPFBorderType.SINGLE, GRID_SIZE, 0, BrandAssets.GRID);
    table.setInsideVBorder(XWPFBorderType.SINGLE, GRID_SIZE, 0, BrandAssets.GRID);
    table.setCellMargins(CELL_MARGIN, CELL_MARGIN, CELL_MARGIN, CELL_MARGIN);
    return table;
  }

  /**
   * Makes the first row the heading row: Header Blue, white bold text, repeated on every page.
   *
   * @param table table
   * @param headings column headings
   */
  public static void headingRow(XWPFTable table, List<String> headings) {
    XWPFTableRow row = table.getRow(0);
    row.setRepeatHeader(true);
    row.setCantSplitRow(true);
    for (int c = 0; c < headings.size(); c++) {
      cell(row.getCell(c), headings.get(c), TextStyle.TABLE_HEAD, false, BrandAssets.HEADER_BLUE);
    }
  }

  /**
   * Fills a cell.
   *
   * @param cell cell
   * @param text text
   * @param textStyle style
   * @param right align right (amounts)
   * @param fill background colour hex, null for none
   */
  public static void cell(
      XWPFTableCell cell, String text, TextStyle textStyle, boolean right, String fill) {
    XWPFParagraph p = cell.getParagraphs().get(0);
    p.setSpacingAfter(0);
    if (right) {
      p.setAlignment(ParagraphAlignment.RIGHT);
    }
    style(p.createRun(), text == null ? "" : text, textStyle);
    if (fill != null) {
      cell.setColor(fill);
    }
  }

  /**
   * Adds a gold line above a cell (grand totals).
   *
   * @param cell cell
   */
  public static void goldTop(XWPFTableCell cell) {
    CTTcPr pr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    CTBorder top = pr.addNewTcBorders().addNewTop();
    top.setVal(STBorder.SINGLE);
    top.setSz(BigInteger.valueOf(GOLD_RULE_SIZE));
    top.setColor(BrandAssets.GOLD);
  }

  /**
   * Makes a row one cell spanning every column (group headers).
   *
   * @param row row with one cell per column
   * @param columns number of columns
   * @return the spanning cell
   */
  public static XWPFTableCell span(XWPFTableRow row, int columns) {
    for (int c = row.getTableCells().size() - 1; c > 0; c--) {
      row.removeCell(c);
    }
    XWPFTableCell cell = row.getCell(0);
    CTTcPr pr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    pr.addNewGridSpan().setVal(BigInteger.valueOf(columns));
    return cell;
  }

  /**
   * The finished file.
   *
   * @return DOCX bytes
   */
  public byte[] bytes() {
    try (XWPFDocument d = doc;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      d.write(out);
      return out.toByteArray();
    } catch (IOException ex) {
      throw new UncheckedIOException("Word rendering failed", ex);
    }
  }

  /**
   * Writes styled text into a run; line breaks become Word line breaks.
   *
   * @param run run
   * @param text text
   * @param textStyle style
   */
  public static void style(XWPFRun run, String text, TextStyle textStyle) {
    String[] lines = text.split("\\R", -1);
    for (int i = 0; i < lines.length; i++) {
      if (i > 0) {
        run.addBreak();
      }
      run.setText(lines[i], i);
    }
    run.setFontFamily(BrandAssets.FONT);
    run.setFontSize(textStyle.points());
    run.setBold(textStyle.bold());
    run.setColor(textStyle.color());
  }

  private static void goldRule(XWPFParagraph p) {
    p.setBorderBottom(Borders.THICK);
    p.getCTP().getPPr().getPBdr().getBottom().setColor(BrandAssets.GOLD);
  }

  private static void field(XWPFParagraph p, String instruction) {
    XWPFRun begin = p.createRun();
    begin.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
    XWPFRun code = p.createRun();
    code.getCTR().addNewInstrText().setStringValue(" " + instruction + " ");
    XWPFRun separate = p.createRun();
    separate.getCTR().addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
    XWPFRun value = p.createRun();
    value.setText("1");
    value.setFontSize(FOOTER_SIZE);
    value.setColor(GREY);
    XWPFRun end = p.createRun();
    end.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);
  }

  private static BigInteger twips(long value) {
    return BigInteger.valueOf(value);
  }

  /**
   * Paper in twips (portrait width and height) and margins.
   *
   * @param width portrait width
   * @param height portrait height
   * @param landscape turn the page
   * @param margin margin on every side
   */
  public record Page(long width, long height, boolean landscape, long margin) {

    /** A4 portrait with 2 cm margins, the page of business documents. */
    public static final Page A4_PORTRAIT = new Page(11_906, 16_838, false, 1_134);

    /**
     * Width between the margins.
     *
     * @return twips
     */
    public long contentWidth() {
      return (landscape ? height : width) - 2 * margin;
    }
  }

  /** Text styles of the BDO layout. */
  public enum TextStyle {
    /** Company line. */
    COMPANY(10, true, BrandAssets.CTA_BLUE, 0),
    /** Document or report title. */
    TITLE(14, true, BrandAssets.HEADER_BLUE, 60),
    /** Metadata lines (report ID, filters, reference). */
    META(8, false, "404040", 20),
    /** Section heading. */
    HEADING(10.5, true, BrandAssets.HEADER_BLUE, 60),
    /** Body text. */
    BODY(9.5, false, "000000", 120),
    /** Label of a field. */
    LABEL(8.5, true, "404040", 0),
    /** Table heading. */
    TABLE_HEAD(8, true, "FFFFFF", 0),
    /** Table cell. */
    TABLE(8, false, "000000", 0),
    /** Emphasised table cell (group header, subtotal, total). */
    TABLE_BOLD(8, true, BrandAssets.HEADER_BLUE, 0),
    /** Page footer. */
    FOOTER(FOOTER_SIZE, false, GREY, 0);

    private final double points;
    private final boolean bold;
    private final String color;
    private final int spacingAfter;

    TextStyle(double points, boolean bold, String color, int spacingAfter) {
      this.points = points;
      this.bold = bold;
      this.color = color;
      this.spacingAfter = spacingAfter;
    }

    /**
     * Font size.
     *
     * @return points
     */
    public double points() {
      return points;
    }

    /**
     * Bold.
     *
     * @return bold
     */
    public boolean bold() {
      return bold;
    }

    /**
     * Colour.
     *
     * @return hex
     */
    public String color() {
      return color;
    }

    /**
     * Space after the paragraph.
     *
     * @return twips
     */
    public int spacingAfter() {
      return spacingAfter;
    }
  }
}
