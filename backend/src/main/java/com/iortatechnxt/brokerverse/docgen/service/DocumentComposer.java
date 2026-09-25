package com.iortatechnxt.brokerverse.docgen.service;

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
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Renders business documents in the BDOI layout: navy letterhead with gold rule, title and
 * reference, field blocks, tables, text and signature lines, page footer. Spreadsheets get a
 * branded header row and frozen panes.
 */
// OpenPDF's Paragraph extends ArrayList (LooseCoupling false positive); PdfWriter is closed by the
// enclosing Document (CloseResource false positive).
@SuppressWarnings({"PMD.LooseCoupling", "PMD.CloseResource"})
@Component
public class DocumentComposer {

  private static final Color NAVY = new Color(0x00, 0x20, 0x5B);
  private static final Color GOLD = new Color(0xFD, 0xB9, 0x13);
  private static final Color SHADE = new Color(0xF3, 0xF5, 0xFA);
  private static final Color GRID = new Color(0xD5, 0xDB, 0xE5);
  private static final float MARGIN = 40f;
  private static final float SPACING = 8f;
  private static final float PADDING = 4f;
  private static final float RULE_WIDTH = 2f;
  private static final float RULE_HEIGHT = 4f;
  private static final float HEADING_SPACING = 12f;
  private static final float LABEL_WIDTH = 1.2f;
  private static final float VALUE_WIDTH = 2.8f;
  private static final float SIGNATURE_SPACING = 36f;
  private static final float FOOTER_Y = 22f;
  private static final int SHEET_COLUMN_WIDTH = 20 * 256;

  private static final Font COMPANY = new Font(Font.HELVETICA, 11, Font.BOLD, NAVY);
  private static final Font TITLE = new Font(Font.HELVETICA, 15, Font.BOLD, NAVY);
  private static final Font REF = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
  private static final Font HEADING = new Font(Font.HELVETICA, 10, Font.BOLD, NAVY);
  private static final Font LABEL = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.DARK_GRAY);
  private static final Font BODY = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
  private static final Font HEAD = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.WHITE);
  private static final Font SMALL = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY);

  private final Clock clock;

  /**
   * Creates the composer.
   *
   * @param clock clock (document date)
   */
  public DocumentComposer(Clock clock) {
    this.clock = clock;
  }

  /**
   * Renders a PDF.
   *
   * @param spec document
   * @return PDF bytes
   */
  public byte[] pdf(DocumentSpec spec) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Document doc = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN)) {
      PdfWriter writer = PdfWriter.getInstance(doc, out);
      writer.setPageEvent(new Footer(spec.footer()));
      doc.open();
      letterhead(doc, spec, LocalDate.now(clock));
      for (Section section : spec.sections()) {
        render(doc, section);
      }
      signatures(doc, spec.signatures());
    }
    return out.toByteArray();
  }

  private static void letterhead(Document doc, DocumentSpec spec, LocalDate date) {
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
      doc.add(new Paragraph("Reference: " + spec.reference() + "    Date: " + date, REF));
    }
  }

  private static void render(Document doc, Section section) {
    switch (section) {
      case Fields f -> fields(doc, f);
      case Table t -> table(doc, t);
      case Text t -> text(doc, t);
    }
  }

  private static void heading(Document doc, String heading) {
    if (heading != null && !heading.isBlank()) {
      Paragraph p = new Paragraph(heading, HEADING);
      p.setSpacingBefore(HEADING_SPACING);
      p.setSpacingAfter(PADDING);
      doc.add(p);
    }
  }

  private static void fields(Document doc, Fields f) {
    heading(doc, f.heading());
    PdfPTable table = new PdfPTable(new float[] {LABEL_WIDTH, VALUE_WIDTH});
    table.setWidthPercentage(100);
    for (Field field : f.fields()) {
      table.addCell(cell(field.label(), LABEL, SHADE, Element.ALIGN_LEFT));
      table.addCell(
          cell(field.value() == null ? "" : field.value(), BODY, null, Element.ALIGN_LEFT));
    }
    doc.add(table);
  }

  private static void table(Document doc, Table t) {
    heading(doc, t.heading());
    PdfPTable table = new PdfPTable(t.headers().size());
    table.setWidthPercentage(100);
    table.setHeaderRows(1);
    for (int c = 0; c < t.headers().size(); c++) {
      table.addCell(cell(t.headers().get(c), HEAD, NAVY, align(t, c)));
    }
    for (List<String> row : t.rows()) {
      for (int c = 0; c < t.headers().size(); c++) {
        String v = c < row.size() && row.get(c) != null ? row.get(c) : "";
        table.addCell(cell(v, BODY, null, align(t, c)));
      }
    }
    doc.add(table);
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
    PdfPCell c = new PdfPCell(new Phrase(text, font));
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
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      CellStyle head = wb.createCellStyle();
      org.apache.poi.ss.usermodel.Font font = wb.createFont();
      font.setBold(true);
      font.setColor(IndexedColors.WHITE.getIndex());
      head.setFont(font);
      head.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
      head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      CellStyle date = wb.createCellStyle();
      date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
      for (SheetSpec spec : specs) {
        writeSheet(wb.createSheet(spec.sheetName()), spec, head, date);
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeSheet(Sheet sheet, SheetSpec spec, CellStyle head, CellStyle date) {
    Row header = sheet.createRow(0);
    for (int c = 0; c < spec.headers().size(); c++) {
      var cell = header.createCell(c);
      cell.setCellValue(spec.headers().get(c));
      cell.setCellStyle(head);
      sheet.setColumnWidth(c, SHEET_COLUMN_WIDTH);
    }
    int r = 1;
    for (List<Object> values : spec.rows()) {
      Row row = sheet.createRow(r++);
      for (int c = 0; c < values.size(); c++) {
        write(row.createCell(c), values.get(c), date);
      }
    }
    sheet.createFreezePane(0, 1);
  }

  private static void write(org.apache.poi.ss.usermodel.Cell cell, Object value, CellStyle date) {
    switch (value) {
      case null -> cell.setBlank();
      case BigDecimal n -> cell.setCellValue(n.doubleValue());
      case Number n -> cell.setCellValue(n.doubleValue());
      case LocalDate d -> {
        cell.setCellValue(d);
        cell.setCellStyle(date);
      }
      case Boolean b -> cell.setCellValue(Boolean.TRUE.equals(b) ? "Y" : "N");
      default -> cell.setCellValue(value.toString());
    }
  }

  /** Page footer: small print and page number. */
  private static final class Footer extends PdfPageEventHelper {

    private final String text;

    Footer(String text) {
      this.text = text == null ? "" : text;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      PdfContentByte cb = writer.getDirectContent();
      ColumnText.showTextAligned(
          cb, Element.ALIGN_LEFT, new Phrase(text, SMALL), document.left(), FOOTER_Y, 0);
      ColumnText.showTextAligned(
          cb,
          Element.ALIGN_RIGHT,
          new Phrase("Page " + writer.getPageNumber(), SMALL),
          document.right(),
          FOOTER_Y,
          0);
    }
  }
}
