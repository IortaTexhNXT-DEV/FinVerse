package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.office.PdfBrandFooter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * The AR and OR in the layout of the client's forms (FRS.CSH.02.06.02 / 02.06.03; Appendix A): the
 * logo, the company name, description, address and VAT TIN; the receipt title, number and copy
 * label at the top right; the REPRINT tag and watermark of a re-printed receipt; the body fields;
 * the check details of an AR; and the footer lines.
 */
// OpenPDF's Paragraph extends ArrayList (LooseCoupling false positive); PdfWriter is closed by the
// enclosing Document (CloseResource false positive).
@SuppressWarnings({"PMD.LooseCoupling", "PMD.CloseResource"})
public final class ReceiptFormPdf {

  private static final Color NAVY = BrandAssets.color(BrandAssets.HEADER);
  private static final Color GRID = BrandAssets.color(BrandAssets.GRID);
  private static final float MARGIN = 36f;
  private static final float LOGO_HEIGHT = 28f;
  private static final float PADDING = 3f;
  private static final float GAP = 8f;
  private static final float LABEL_WIDTH = 1.3f;
  private static final float VALUE_WIDTH = 2.7f;
  private static final float HEADER_LEFT = 2.6f;
  private static final float HEADER_RIGHT = 1.4f;
  private static final float WATERMARK_SIZE = 96f;
  private static final float WATERMARK_ANGLE = 45f;
  private static final float WATERMARK_OPACITY = 0.12f;
  private static final int VAT_LINE = 3;

  private static final Font COMPANY = new Font(Font.HELVETICA, 11, Font.BOLD, NAVY);
  private static final Font HEADER = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
  private static final Font TITLE = new Font(Font.HELVETICA, 13, Font.BOLD, NAVY);
  private static final Font NUMBER = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
  private static final Font COPY = new Font(Font.HELVETICA, 8, Font.BOLDITALIC, Color.DARK_GRAY);
  private static final Font REPRINT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.RED);
  private static final Font LABEL = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.DARK_GRAY);
  private static final Font BODY = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
  private static final Font FOOTER = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);

  private ReceiptFormPdf() {}

  /**
   * Renders a receipt.
   *
   * @param form receipt and its form
   * @return PDF
   */
  public static byte[] render(FormView form) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Document doc = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN)) {
      PdfWriter writer = PdfWriter.getInstance(doc, out);
      doc.open();
      if (form.reprint()) {
        watermark(writer, doc);
      }
      doc.add(header(form));
      if (form.reprint()) {
        Paragraph tag = new Paragraph("REPRINT", REPRINT);
        tag.setAlignment(Element.ALIGN_RIGHT);
        doc.add(tag);
      }
      doc.add(fields(form.fields()));
      if (!form.checks().isEmpty()) {
        doc.add(checks(form.checks()));
      }
      for (String line : form.footer()) {
        if (line != null && !line.isBlank()) {
          Paragraph p = new Paragraph(line, FOOTER);
          p.setAlignment(Element.ALIGN_CENTER);
          p.setSpacingBefore(PADDING);
          doc.add(p);
        }
      }
    }
    return out.toByteArray();
  }

  private static PdfPTable header(FormView form) {
    PdfPTable table = new PdfPTable(new float[] {HEADER_LEFT, HEADER_RIGHT});
    table.setWidthPercentage(100);
    PdfPCell left = new PdfPCell();
    left.setBorder(PdfPCell.NO_BORDER);
    left.addElement(PdfBrandFooter.logo(LOGO_HEIGHT));
    left.addElement(new Paragraph(form.companyName(), COMPANY));
    for (String line : List.of(form.description(), form.address(), form.vat())) {
      if (line != null && !line.isBlank()) {
        left.addElement(new Paragraph(line, HEADER));
      }
    }
    table.addCell(left);
    PdfPCell right = new PdfPCell();
    right.setBorder(PdfPCell.NO_BORDER);
    right.setHorizontalAlignment(Element.ALIGN_RIGHT);
    for (Paragraph p :
        List.of(
            new Paragraph(form.title(), TITLE),
            new Paragraph(form.receiptNo(), NUMBER),
            new Paragraph(form.copyLabel(), COPY))) {
      p.setAlignment(Element.ALIGN_RIGHT);
      right.addElement(p);
    }
    table.addCell(right);
    table.setSpacingAfter(GAP);
    return table;
  }

  private static PdfPTable fields(List<String[]> fields) {
    PdfPTable table = new PdfPTable(new float[] {LABEL_WIDTH, VALUE_WIDTH});
    table.setWidthPercentage(100);
    table.setSpacingBefore(GAP);
    for (String[] field : fields) {
      table.addCell(cell(field[0], LABEL));
      table.addCell(cell(field[1], BODY));
    }
    return table;
  }

  private static PdfPTable checks(List<String[]> checks) {
    PdfPTable table = new PdfPTable(checks.get(0).length);
    table.setWidthPercentage(100);
    table.setSpacingBefore(GAP);
    table.setSpacingAfter(GAP);
    boolean head = true;
    for (String[] row : checks) {
      for (String value : row) {
        PdfPCell cell = cell(value, head ? LABEL : BODY);
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(GRID);
        table.addCell(cell);
      }
      head = false;
    }
    return table;
  }

  private static PdfPCell cell(String text, Font font) {
    PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
    cell.setBorder(PdfPCell.BOTTOM);
    cell.setBorderColor(GRID);
    cell.setPadding(PADDING);
    return cell;
  }

  private static void watermark(PdfWriter writer, Document doc) {
    PdfContentByte under = writer.getDirectContentUnder();
    PdfGState state = new PdfGState();
    state.setFillOpacity(WATERMARK_OPACITY);
    under.saveState();
    under.setGState(state);
    Font mark = new Font(Font.HELVETICA, WATERMARK_SIZE, Font.BOLD, Color.GRAY);
    ColumnText.showTextAligned(
        under,
        Element.ALIGN_CENTER,
        new Phrase("REPRINT", mark),
        (doc.left() + doc.right()) / 2,
        (doc.top() + doc.bottom()) / 2,
        WATERMARK_ANGLE);
    under.restoreState();
  }

  /**
   * A receipt in its form.
   *
   * @param title ACKNOWLEDGEMENT RECEIPT or OFFICIAL RECEIPT
   * @param receiptNo receipt number
   * @param copyLabel Client's Copy or the company's copy
   * @param reprint whether the receipt was printed before
   * @param header company name, description, address and VAT TIN line
   * @param fields body fields (label, value)
   * @param checks check details, the first row the headings; empty without a check
   * @param footer footer lines, tokens resolved
   */
  public record FormView(
      String title,
      String receiptNo,
      String copyLabel,
      boolean reprint,
      List<String> header,
      List<String[]> fields,
      List<String[]> checks,
      List<String> footer) {

    /** Defensive copies. */
    public FormView {
      header = List.copyOf(header);
      fields = List.copyOf(fields);
      checks = List.copyOf(checks);
      footer = List.copyOf(footer);
    }

    String companyName() {
      return line(0) == null ? "" : line(0);
    }

    String description() {
      return line(1);
    }

    String address() {
      return line(2);
    }

    String vat() {
      return line(VAT_LINE);
    }

    private String line(int index) {
      return header.size() > index ? header.get(index) : null;
    }
  }
}
