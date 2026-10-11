package com.iortatechnxt.brokerverse.docgen.service;

import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.office.PdfBrandFooter;
import com.iortatechnxt.brokerverse.common.office.PdfColumnWidths;
import com.iortatechnxt.brokerverse.common.office.PdfWordBreaks;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Section;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Renders business documents in the BDO Insure layout: the logo and company above a gold rule,
 * title and reference, field blocks, tables with Header Blue headings, text and signature lines,
 * and a page footer with the "Confidential" classification, the document's small print and "Page n
 * of m". Every document is rendered as PDF or Word with the same content (client requirement 16);
 * each composed PDF is recorded so its Word copy can be downloaded later ({@link
 * DocumentRenditionService}). Spreadsheets get a branded header row and frozen panes.
 */
// OpenPDF's Paragraph extends ArrayList (LooseCoupling false positive); PdfWriter is closed by the
// enclosing Document (CloseResource false positive).
@SuppressWarnings({"PMD.LooseCoupling", "PMD.CloseResource"})
@Component
public class DocumentComposer {

  /** The most table columns a portrait page holds; wider documents are landscape. */
  private static final int PORTRAIT_COLUMNS = 9;

  private static final Color NAVY = BrandAssets.color(BrandAssets.HEADER);
  private static final Color COMPANY_BLUE = BrandAssets.color(BrandAssets.PRIMARY);
  private static final Color GOLD = BrandAssets.color(BrandAssets.ACCENT);
  private static final Color SHADE = BrandAssets.color(BrandAssets.BACKGROUND);
  private static final Color GRID = BrandAssets.color(BrandAssets.GRID);
  private static final float LOGO_HEIGHT = 26f;
  private static final float MARGIN = 40f;
  private static final float SPACING = 8f;
  private static final float PADDING = 4f;
  private static final float RULE_WIDTH = 2f;
  private static final float RULE_HEIGHT = 4f;
  private static final float HEADING_SPACING = 12f;
  private static final float LABEL_WIDTH = 1.2f;
  private static final float VALUE_WIDTH = 2.8f;
  private static final float SIGNATURE_SPACING = 36f;

  private static final Font COMPANY = new Font(Font.HELVETICA, 11, Font.BOLD, COMPANY_BLUE);
  private static final Font TITLE = new Font(Font.HELVETICA, 15, Font.BOLD, NAVY);
  private static final Font REF = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
  private static final Font HEADING = new Font(Font.HELVETICA, 10, Font.BOLD, NAVY);
  private static final Font LABEL = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.DARK_GRAY);
  private static final Font BODY = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
  private static final Font HEAD = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.WHITE);
  private static final Font SMALL = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY);
  private static final Font SMALL_HEAD = new Font(Font.HELVETICA, 7.5f, Font.BOLD, Color.WHITE);
  private static final Font SMALL_BODY = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, Color.BLACK);

  private final Clock clock;
  private final DocumentRenditionService renditions;

  /**
   * Creates the composer.
   *
   * @param clock clock (document date)
   * @param renditions record of composed PDFs, for their Word copies
   */
  public DocumentComposer(Clock clock, DocumentRenditionService renditions) {
    this.clock = clock;
    this.renditions = renditions;
  }

  /**
   * Renders a PDF and records its content, so the same document can later be downloaded as Word.
   *
   * @param spec document
   * @return PDF bytes
   */
  public byte[] pdf(DocumentSpec spec) {
    LocalDate date = BusinessClock.today(clock);
    byte[] pdf = pdf(spec, date);
    renditions.record(pdf, spec, date);
    return pdf;
  }

  /**
   * Renders the document as Word, with the content and layout of its PDF.
   *
   * @param spec document
   * @return DOCX bytes
   */
  public byte[] docx(DocumentSpec spec) {
    return DocumentWordWriter.write(spec, BusinessClock.today(clock));
  }

  /**
   * Renders the document in a format (downloads offer PDF and Word).
   *
   * @param spec document
   * @param format format
   * @return file bytes
   */
  public byte[] render(DocumentSpec spec, DocumentFormat format) {
    return format == DocumentFormat.DOCX ? docx(spec) : pdf(spec);
  }

  private static byte[] pdf(DocumentSpec spec, LocalDate date) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Rectangle page = isWide(spec) ? PageSize.A4.rotate() : PageSize.A4;
    try (Document doc = new Document(page, MARGIN, MARGIN, MARGIN, MARGIN)) {
      PdfWriter writer = PdfWriter.getInstance(doc, out);
      doc.open();
      writer.setPageEvent(new PdfBrandFooter(DocumentText.pageFooter(spec), "", writer));
      letterhead(doc, spec, date);
      for (Section section : spec.sections()) {
        render(doc, section);
      }
      signatures(doc, spec.signatures());
    }
    return out.toByteArray();
  }

  /**
   * Whether the document is printed in landscape: a table of more than {@value #PORTRAIT_COLUMNS}
   * columns (a schedule of accounts) does not fit a portrait page without breaking words.
   */
  static boolean isWide(DocumentSpec spec) {
    return spec.sections().stream()
        .anyMatch(s -> s instanceof Table t && t.headers().size() > PORTRAIT_COLUMNS);
  }

  private static void letterhead(Document doc, DocumentSpec spec, LocalDate date) {
    doc.add(PdfBrandFooter.logo(LOGO_HEIGHT));
    doc.add(new Paragraph(spec.companyName(), COMPANY));
    PdfPTable rule = new PdfPTable(1);
    rule.setWidthPercentage(100);
    PdfPCell line = new PdfPCell(new Phrase(" ", SMALL));
    line.setBorder(PdfPCell.BOTTOM);
    line.setBorderColorBottom(GOLD);
    line.setBorderWidthBottom(RULE_WIDTH);
    line.setFixedHeight(RULE_HEIGHT);
    rule.addCell(line);
    doc.add(rule);
    Paragraph title = new Paragraph(spec.title(), TITLE);
    title.setSpacingBefore(SPACING);
    doc.add(title);
    if (spec.reference() != null) {
      doc.add(new Paragraph(DocumentText.referenceLine(spec, date), REF));
    }
  }

  private static void render(Document doc, Section section) {
    switch (section) {
      case Fields f -> fields(doc, f);
      case Table t -> table(doc, t);
      case Text t -> text(doc, t);
    }
  }

  /**
   * The heading of a section; a section without a heading keeps a gap to the text above, so that
   * its table never touches the reference line.
   */
  private static float heading(Document doc, String heading) {
    if (heading != null && !heading.isBlank()) {
      Paragraph p = new Paragraph(heading, HEADING);
      p.setSpacingBefore(HEADING_SPACING);
      p.setSpacingAfter(PADDING);
      doc.add(p);
      return 0;
    }
    return SPACING;
  }

  private static void fields(Document doc, Fields f) {
    float gap = heading(doc, f.heading());
    PdfPTable table = new PdfPTable(new float[] {LABEL_WIDTH, VALUE_WIDTH});
    table.setWidthPercentage(100);
    table.setSpacingBefore(gap);
    for (Field field : f.fields()) {
      table.addCell(cell(field.label(), LABEL, SHADE, Element.ALIGN_LEFT));
      table.addCell(
          cell(field.value() == null ? "" : field.value(), BODY, null, Element.ALIGN_LEFT));
    }
    doc.add(table);
  }

  private static void table(Document doc, Table t) {
    float gap = heading(doc, t.heading());
    float width = doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin();
    // A schedule too wide for its words at the body size is printed in the small table size.
    boolean small = !columnWidths(t, HEAD, BODY).fits(width);
    Font head = small ? SMALL_HEAD : HEAD;
    Font body = small ? SMALL_BODY : BODY;
    PdfPTable table = new PdfPTable(columnWidths(t, head, body).fit(width));
    table.setWidthPercentage(100);
    table.setSpacingBefore(gap);
    table.setHeaderRows(1);
    for (int c = 0; c < t.headers().size(); c++) {
      table.addCell(cell(t.headers().get(c), head, NAVY, align(t, c)));
    }
    for (List<String> row : t.rows()) {
      for (int c = 0; c < t.headers().size(); c++) {
        String v = c < row.size() && row.get(c) != null ? row.get(c) : "";
        table.addCell(cell(v, body, null, align(t, c)));
      }
    }
    doc.add(table);
  }

  /**
   * Column widths that break headings and values only between words ("Endorsement Number" never as
   * "Endorseme nt"), shared in proportion to the column weights; in the small table size when the
   * body size does not fit.
   */
  static float[] widths(Document doc, Table t) {
    float width = doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin();
    PdfColumnWidths regular = columnWidths(t, HEAD, BODY);
    return regular.fits(width)
        ? regular.fit(width)
        : columnWidths(t, SMALL_HEAD, SMALL_BODY).fit(width);
  }

  private static PdfColumnWidths columnWidths(Table t, Font head, Font body) {
    PdfColumnWidths widths = new PdfColumnWidths(t.columnWeights(), 2 * PADDING);
    for (int c = 0; c < t.headers().size(); c++) {
      widths.heading(c, t.headers().get(c), head);
    }
    return widths.values(t.rows(), body);
  }

  private static int align(Table t, int column) {
    return t.rightAligned().contains(column) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT;
  }

  private static void text(Document doc, Text t) {
    heading(doc, t.heading());
    for (String para : t.body().split("\\R\\s*\\R")) {
      Paragraph p = new Paragraph(para.trim(), BODY);
      p.setSpacingAfter(PADDING);
      doc.add(p);
    }
  }

  private static void signatures(Document doc, List<String> captions) {
    if (captions.isEmpty()) {
      return;
    }
    PdfPTable table = new PdfPTable(captions.size());
    table.setWidthPercentage(100);
    table.setSpacingBefore(SIGNATURE_SPACING);
    for (String caption : captions) {
      PdfPCell c = new PdfPCell(new Phrase(caption, LABEL));
      c.setBorder(PdfPCell.TOP);
      c.setBorderColorTop(Color.GRAY);
      c.setPaddingTop(PADDING);
      c.setPaddingRight(SIGNATURE_SPACING / 2);
      table.addCell(c);
    }
    doc.add(table);
  }

  private static PdfPCell cell(String text, Font font, Color background, int alignment) {
    PdfPCell c = new PdfPCell(PdfWordBreaks.phrase(text, font));
    c.setPadding(PADDING);
    c.setBorderColor(GRID);
    c.setHorizontalAlignment(alignment);
    if (background != null) {
      c.setBackgroundColor(background);
    }
    return c;
  }

  /**
   * Renders a spreadsheet.
   *
   * @param spec sheet
   * @return xlsx bytes
   */
  public byte[] xlsx(SheetSpec spec) {
    return xlsx(List.of(spec));
  }

  /**
   * Renders a workbook with one sheet per spec, in order.
   *
   * @param specs sheets (at least one)
   * @return xlsx bytes
   */
  public byte[] xlsx(List<SheetSpec> specs) {
    return SheetWriter.write(specs);
  }
}
