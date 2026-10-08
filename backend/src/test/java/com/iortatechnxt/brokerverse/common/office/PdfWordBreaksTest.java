package com.iortatechnxt.brokerverse.common.office;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Lines of a table cell break between words and after reference hyphens, never inside a date. */
class PdfWordBreaksTest {

  @Test
  void theHyphensOfADateAreNotBreakPoints() {
    char[] text = "09-Oct-2026 to 09-Oct-2027".toCharArray();
    assertThat(PdfWordBreaks.inDate(text, 2)).isTrue();
    assertThat(PdfWordBreaks.inDate(text, 6)).isTrue();
    assertThat(PdfWordBreaks.inDate(text, 17)).isTrue();
    char[] reference = "BI-HO-2026-000001".toCharArray();
    assertThat(PdfWordBreaks.inDate(reference, 2)).isFalse();
    assertThat(PdfWordBreaks.inDate(reference, 10)).isFalse();
  }

  @Test
  void aPeriodInANarrowCellBreaksBetweenItsDates() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Document doc = new Document(PageSize.A4)) {
      PdfWriter.getInstance(doc, out);
      doc.open();
      PdfPTable table = new PdfPTable(1);
      table.setTotalWidth(90);
      table.setLockedWidth(true);
      table.addCell(
          new PdfPCell(
              PdfWordBreaks.phrase("09-Oct-2026 to 09-Oct-2027", new Font(Font.HELVETICA, 9))));
      doc.add(table);
    }
    try (PdfReader reader = new PdfReader(out.toByteArray())) {
      String page = new PdfTextExtractor(reader).getTextFromPage(1);
      assertThat(page).contains("09-Oct-2027").doesNotContain("09-\nOct");
    }
  }
}
