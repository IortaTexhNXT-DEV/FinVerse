package com.iortatechnxt.brokerverse.docgen.service;

import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.office.BrandedDocx;
import com.iortatechnxt.brokerverse.common.office.BrandedDocx.Page;
import com.iortatechnxt.brokerverse.common.office.BrandedDocx.TextStyle;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Section;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

/**
 * Writes a business document as Word (DOCX) with the content and layout of its PDF (client
 * requirement 16): the BDO Insure logo and company in the page header, title, reference and date,
 * field blocks, tables with a Header Blue heading row repeated on every page, text and signature
 * lines, and a footer with the "Confidential" classification, the document's small print and "Page
 * x of y".
 */
final class DocumentWordWriter {

  private static final double LABEL_WEIGHT = 1.2;
  private static final double VALUE_WEIGHT = 2.8;
  private static final double NATURAL = 1;
  private static final int SIGNATURE_SPACE = 720;
  private static final int SIGNATURE_RULE = 6;

  private DocumentWordWriter() {}

  /**
   * Writes the document.
   *
   * @param spec document
   * @param date document date printed next to the reference
   * @return DOCX bytes
   */
  static byte[] write(DocumentSpec spec, LocalDate date) {
    BrandedDocx docx = new BrandedDocx(Page.A4_PORTRAIT, spec.title());
    docx.pageHeader(spec.companyName());
    docx.pageFooter(spec.footer(), "");
    docx.paragraph(spec.title(), TextStyle.TITLE);
    if (spec.reference() != null) {
      docx.paragraph("Reference: " + spec.reference() + "    Date: " + date, TextStyle.META);
    }
    for (Section section : spec.sections()) {
      switch (section) {
        case Fields f -> fields(docx, f);
        case Table t -> table(docx, t);
        case Text t -> text(docx, t);
      }
    }
    signatures(docx, spec.signatures());
    return docx.bytes();
  }

  private static void heading(BrandedDocx docx, String heading) {
    if (heading != null && !heading.isBlank()) {
      docx.paragraph(heading, TextStyle.HEADING);
    }
  }

  private static void fields(BrandedDocx docx, Fields f) {
    heading(docx, f.heading());
    if (f.fields().isEmpty()) {
      return;
    }
    XWPFTable table = docx.table(new double[] {LABEL_WEIGHT, VALUE_WEIGHT}, true, NATURAL);
    boolean first = true;
    for (Field field : f.fields()) {
      XWPFTableRow row = first ? table.getRow(0) : table.createRow();
      first = false;
      row.setCantSplitRow(true);
      BrandedDocx.cell(
          row.getCell(0), field.label(), TextStyle.LABEL, false, BrandAssets.BACKGROUND_BLUE);
      BrandedDocx.cell(row.getCell(1), field.value(), TextStyle.TABLE, false, null);
    }
  }

  private static void table(BrandedDocx docx, Table t) {
    heading(docx, t.heading());
    double[] weights = new double[t.headers().size()];
    Arrays.fill(weights, 1);
    XWPFTable table = docx.table(weights, true, NATURAL);
    BrandedDocx.headingRow(table, t.headers());
    for (List<String> values : t.rows()) {
      XWPFTableRow row = table.createRow();
      row.setCantSplitRow(true);
      for (int c = 0; c < t.headers().size(); c++) {
        String v = c < values.size() && values.get(c) != null ? values.get(c) : "";
        BrandedDocx.cell(row.getCell(c), v, TextStyle.TABLE, t.rightAligned().contains(c), null);
      }
    }
  }

  private static void text(BrandedDocx docx, Text t) {
    heading(docx, t.heading());
    for (String para : t.body().split("\\R\\s*\\R")) {
      docx.paragraph(para.trim(), TextStyle.BODY);
    }
  }

  private static void signatures(BrandedDocx docx, List<String> captions) {
    if (captions.isEmpty()) {
      return;
    }
    docx.paragraph("", TextStyle.BODY).setSpacingBefore(SIGNATURE_SPACE);
    double[] weights = new double[captions.size()];
    Arrays.fill(weights, 1);
    XWPFTable table = docx.table(weights, true, NATURAL);
    table.removeBorders();
    XWPFTableRow row = table.getRow(0);
    for (int c = 0; c < captions.size(); c++) {
      XWPFTableCell cell = row.getCell(c);
      BrandedDocx.cell(cell, captions.get(c), TextStyle.LABEL, false, null);
      CTBorder top =
          (cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr())
              .addNewTcBorders()
              .addNewTop();
      top.setVal(STBorder.SINGLE);
      top.setSz(BigInteger.valueOf(SIGNATURE_RULE));
      top.setColor(BrandAssets.GRID);
    }
  }
}
