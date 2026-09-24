package com.iortatechnxt.brokerverse.report.render;

import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * PDF export in the corporate report layout: company, title, a metadata block (report ID, user, run
 * date and the filters applied, BRNB.031), repeated column headings, group headers, subtotals,
 * grand total, "*** End of Report ***" and a page footer with the administrator's footer text
 * ({@code REPORT_FOOTER_TEXT}) and "Page n of m".
 */
// OpenPDF's Paragraph extends ArrayList (LooseCoupling false positive); PdfWriter is closed by the
// enclosing Document (CloseResource false positive).
@SuppressWarnings({"PMD.LooseCoupling", "PMD.CloseResource"})
@Component
public class PdfReportRenderer implements ReportRenderer {

  // BDO style guide: Header Blue #004EA8, CTA Blue #0072D8, Background Blue #E5F5FF.
  private static final Color BRAND_NAVY = new Color(0x00, 0x4E, 0xA8);
  private static final Color BRAND_BLUE = new Color(0x00, 0x72, 0xD8);
  private static final Color BRAND_GOLD = new Color(0xFD, 0xB9, 0x13);
  private static final Color GROUP_BG = new Color(0xE5, 0xF5, 0xFF);
  private static final Color SUBTOTAL_BG = new Color(0xF4, 0xF6, 0xFA);
  private static final int LANDSCAPE_THRESHOLD = 7;
  private static final float MARGIN = 28f;
  private static final float BODY_SIZE = 7.5f;
  private static final float LABEL_WEIGHT = 1.6f;
  private static final float TEXT_WEIGHT = 2f;
  private static final float NUMBER_WEIGHT = 1.2f;
  private static final float HEAD_PADDING = 3f;
  private static final float CELL_PADDING = 2.5f;
  private static final float GRID_WIDTH = 0.4f;
  private static final float END_SPACING = 8f;
  private static final Color GRID = new Color(0xD5, 0xDB, 0xE5);
  private static final DateTimeFormatter STAMP =
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.of("Asia/Manila"));

  private static final Font TITLE = new Font(Font.HELVETICA, 13, Font.BOLD, BRAND_NAVY);
  private static final Font COMPANY = new Font(Font.HELVETICA, 10, Font.BOLD, BRAND_BLUE);
  private static final Font META = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, Color.DARK_GRAY);
  private static final Font HEAD = new Font(Font.HELVETICA, BODY_SIZE, Font.BOLD, Color.WHITE);
  private static final Font BODY = new Font(Font.HELVETICA, BODY_SIZE, Font.NORMAL, Color.BLACK);
  private static final Font BOLD = new Font(Font.HELVETICA, BODY_SIZE, Font.BOLD, BRAND_NAVY);

  @Override
  public ExportFormat format() {
    return ExportFormat.PDF;
  }

  @Override
  public byte[] render(ReportResult result, ReportContext context) {
    boolean landscape = result.columns().size() > LANDSCAPE_THRESHOLD;
    Rectangle size = landscape ? PageSize.A4.rotate() : PageSize.A4;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Document doc = new Document(size, MARGIN, MARGIN, MARGIN, MARGIN + 10)) {
      PdfWriter writer = PdfWriter.getInstance(doc, out);
      doc.open();
      // The footer's "of m" template can only be created once the document is open; page 1
      // has not ended yet, so the footer still applies to every page.
      writer.setPageEvent(new PageFooter(result.code(), context.footerText(), writer));
      addHeader(doc, result, context);
      doc.add(table(result));
      for (String note : result.notes()) {
        doc.add(new Paragraph("Note: " + note, META));
      }
      Paragraph end = new Paragraph("*** End of Report ***", META);
      end.setAlignment(Element.ALIGN_CENTER);
      end.setSpacingBefore(END_SPACING);
      doc.add(end);
    }
    return out.toByteArray();
  }

  private static void addHeader(Document doc, ReportResult result, ReportContext ctx) {
    doc.add(new Paragraph(ctx.companyName(), COMPANY));
    Paragraph title = new Paragraph(result.title(), TITLE);
    title.setSpacingAfter(2);
    doc.add(title);
    doc.add(
        new Paragraph(
            "Report ID: "
                + result.code()
                + "    User ID: "
                + ctx.generatedBy()
                + "    Run Date: "
                + STAMP.format(ctx.generatedAt()),
            META));
    // Print metadata block (BRNB.031): report, user and time above, then the filters applied.
    if (!result.parameterEcho().isEmpty()) {
      doc.add(new Paragraph("Filters: " + String.join("    ", result.parameterEcho()), META));
    }
    Paragraph spacer = new Paragraph(" ", META);
    spacer.setSpacingAfter(2);
    doc.add(spacer);
  }

  private static PdfPTable table(ReportResult result) {
    List<ReportColumn> cols = result.columns();
    PdfPTable table = new PdfPTable(cols.size() + 1);
    table.setWidthPercentage(100);
    float[] widths = new float[cols.size() + 1];
    widths[0] = LABEL_WEIGHT;
    for (int i = 0; i < cols.size(); i++) {
      widths[i + 1] = cols.get(i).type() == ColumnType.TEXT ? TEXT_WEIGHT : NUMBER_WEIGHT;
    }
    table.setWidths(widths);
    table.setHeaderRows(1);
    table.addCell(headCell(""));
    cols.forEach(c -> table.addCell(headCell(c.label())));
    for (ReportRow row : result.rows()) {
      addRow(table, row, cols);
    }
    return table;
  }

  private static void addRow(PdfPTable table, ReportRow row, List<ReportColumn> cols) {
    if (row.kind() == RowKind.GROUP_HEADER || row.kind() == RowKind.SECTION) {
      PdfPCell cell = cell(indent(row) + row.label(), BOLD, Element.ALIGN_LEFT, GROUP_BG);
      cell.setColspan(cols.size() + 1);
      table.addCell(cell);
    } else {
      addValueRow(table, row, cols);
    }
  }

  private static void addValueRow(PdfPTable table, ReportRow row, List<ReportColumn> cols) {
    boolean emphasis = row.kind() == RowKind.SUBTOTAL || row.kind() == RowKind.TOTAL;
    Color bg = emphasis ? SUBTOTAL_BG : null;
    Font font = emphasis ? BOLD : BODY;
    String label = row.label() == null ? "" : indent(row) + row.label();
    table.addCell(cell(label, font, Element.ALIGN_LEFT, bg));
    for (ReportColumn c : cols) {
      String text = CellFormatter.format(row.cells().get(c.key()), c.type());
      PdfPCell cell = cell(text, font, alignment(c.type()), bg);
      if (row.kind() == RowKind.TOTAL) {
        cell.setBorderWidthTop(1f);
        cell.setBorderColorTop(BRAND_GOLD);
      }
      table.addCell(cell);
    }
  }

  private static int alignment(ColumnType type) {
    return type == ColumnType.TEXT || type == ColumnType.DATE
        ? Element.ALIGN_LEFT
        : Element.ALIGN_RIGHT;
  }

  private static String indent(ReportRow row) {
    return "  ".repeat(row.level());
  }

  private static PdfPCell headCell(String text) {
    PdfPCell cell = new PdfPCell(new Phrase(text, HEAD));
    cell.setBackgroundColor(BRAND_NAVY);
    cell.setPadding(HEAD_PADDING);
    cell.setBorderColor(BRAND_NAVY);
    return cell;
  }

  private static PdfPCell cell(String text, Font font, int align, Color bg) {
    PdfPCell cell = new PdfPCell(new Phrase(text, font));
    cell.setHorizontalAlignment(align);
    cell.setPadding(CELL_PADDING);
    cell.setBorderColor(GRID);
    cell.setBorderWidth(GRID_WIDTH);
    if (bg != null) {
      cell.setBackgroundColor(bg);
    }
    return cell;
  }

  /**
   * Writes the footer text (left, shortened with an ellipsis when it would reach the page number)
   * and "iNXT BrokerVerse | report code | Page n of m" (right) at the foot of every page.
   */
  private static final class PageFooter extends PdfPageEventHelper {

    private static final float FOOTER_Y = 16f;
    private static final float TEMPLATE_WIDTH = 30f;
    private static final float TEMPLATE_HEIGHT = 12f;
    private static final float FONT_SIZE = 7f;
    private static final float GAP = 12f;
    private static final String ELLIPSIS = "...";

    private final String code;
    private final String footerText;
    private final PdfTemplate total;
    private final BaseFont font;

    PageFooter(String code, String footerText, PdfWriter writer) {
      this.code = code;
      this.footerText = footerText;
      this.total = writer.getDirectContent().createTemplate(TEMPLATE_WIDTH, TEMPLATE_HEIGHT);
      this.font = new Font(Font.HELVETICA).getCalculatedBaseFont(false);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      PdfContentByte cb = writer.getDirectContent();
      String text = "iNXT BrokerVerse  |  " + code + "  |  Page " + writer.getPageNumber() + " of ";
      float x = document.right() - font.getWidthPoint(text, FONT_SIZE) - TEMPLATE_WIDTH;
      cb.beginText();
      cb.setFontAndSize(font, FONT_SIZE);
      cb.setColorFill(Color.GRAY);
      cb.setTextMatrix(x, FOOTER_Y);
      cb.showText(text);
      String left = fit(footerText, x - GAP - document.left());
      if (!left.isEmpty()) {
        cb.setTextMatrix(document.left(), FOOTER_Y);
        cb.showText(left);
      }
      cb.endText();
      cb.addTemplate(total, x + font.getWidthPoint(text, FONT_SIZE), FOOTER_Y);
    }

    /** The text, shortened with an ellipsis to the available width (empty when nothing fits). */
    private String fit(String text, float width) {
      if (font.getWidthPoint(text, FONT_SIZE) <= width) {
        return text;
      }
      String shortened = text;
      while (!shortened.isEmpty() && font.getWidthPoint(shortened + ELLIPSIS, FONT_SIZE) > width) {
        shortened = shortened.substring(0, shortened.length() - 1);
      }
      return shortened.isEmpty() ? "" : shortened.strip() + ELLIPSIS;
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
      total.beginText();
      total.setFontAndSize(font, FONT_SIZE);
      total.setColorFill(Color.GRAY);
      total.setTextMatrix(0, 0);
      total.showText(String.valueOf(writer.getPageNumber()));
      total.endText();
    }
  }
}
