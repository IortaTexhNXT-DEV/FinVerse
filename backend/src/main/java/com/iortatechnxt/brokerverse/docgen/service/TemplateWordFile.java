package com.iortatechnxt.brokerverse.docgen.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.office.BrandedDocx;
import com.iortatechnxt.brokerverse.common.office.BrandedDocx.Page;
import com.iortatechnxt.brokerverse.common.office.BrandedDocx.TextStyle;
import com.iortatechnxt.brokerverse.docgen.domain.DocTemplate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/**
 * A document template as a Word file for the System Administrator (BRNB.004, client requirement
 * 16): the first paragraph is the title, every following paragraph a paragraph of the text, with
 * the {@code {{placeholders}}} as typed. The file is edited in Word and uploaded as the draft of a
 * new version.
 */
final class TemplateWordFile {

  private static final String INVALID = "TEMPLATE_WORD_INVALID";

  private TemplateWordFile() {}

  /**
   * Writes a template version.
   *
   * @param t version
   * @return DOCX bytes
   */
  static byte[] write(DocTemplate t) {
    BrandedDocx docx = new BrandedDocx(Page.A4_PORTRAIT, t.getTitle());
    docx.pageHeader("");
    docx.pageFooter(t.getCode() + " v" + t.getVersionNo(), "");
    docx.paragraph(t.getTitle(), TextStyle.TITLE);
    for (String para : t.getBody().split("\\R\\s*\\R")) {
      docx.paragraph(para.strip(), TextStyle.BODY);
    }
    return docx.bytes();
  }

  /**
   * Reads an edited template: the first non-empty paragraph is the title, the others the text.
   *
   * @param content DOCX bytes
   * @return title and text
   * @throws BusinessRuleException {@code TEMPLATE_WORD_INVALID} for a file that is not Word or has
   *     no text
   */
  static Draft read(byte[] content) {
    List<String> paragraphs = new ArrayList<>();
    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
      for (XWPFParagraph p : doc.getParagraphs()) {
        String text = p.getText().replace("\r", "").strip();
        if (!text.isEmpty()) {
          paragraphs.add(text);
        }
      }
    } catch (IOException | RuntimeException ex) {
      throw new BusinessRuleException(INVALID, "The file is not a Word (.docx) document", ex);
    }
    if (paragraphs.size() < 2) {
      throw new BusinessRuleException(
          INVALID, "The Word file needs the title in its first paragraph and the text below it");
    }
    return new Draft(
        paragraphs.get(0), String.join("\n\n", paragraphs.subList(1, paragraphs.size())));
  }

  /**
   * Title and text read from a Word file.
   *
   * @param title title
   * @param body text with placeholders
   */
  record Draft(String title, String body) {}
}
