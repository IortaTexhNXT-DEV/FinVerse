package com.iortatechnxt.brokerverse.docgen.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.iortatechnxt.brokerverse.common.office.PdfColumnWidths;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Table headings and dates of generated documents break only between words. */
class DocumentTableWidthsTest {

  private static final Font HEAD = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.WHITE);
  private static final Font BODY = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);

  @Test
  void everyHeadingWordAndDateFitsItsColumn() {
    List<String> headers =
        List.of(
            "Invoice Number",
            "Policy Number",
            "Endorsement Number",
            "Name of Assured",
            "Risk Code",
            "Date Last Paid",
            "Date Inception",
            "Date Booked",
            "Date Expiry",
            "Paid AR",
            "Realized Commission",
            "Realized VAT",
            "WTAX",
            "DTIP",
            "Net Due");
    List<String> row =
        List.of(
            "BI-HO-2026-000001",
            "MGIC-MC-2026-98801",
            "-",
            "Santos, Maria Clara Reyes",
            "MTR10",
            "25-Sep-2026",
            "01-Sep-2026",
            "15-Sep-2026",
            "01-Sep-2027",
            "17,027.86",
            "2,379.13",
            "285.50",
            "237.91",
            "17,027.86",
            "14,601.14");
    Table table = new Table("Accounts", headers, List.of(row), List.of(9, 10, 11, 12, 13, 14));
    try (Document doc = new Document(PageSize.A4.rotate(), 40, 40, 40, 40)) {
      float[] widths = DocumentComposer.widths(doc, table);
      float sum = 0;
      for (int c = 0; c < headers.size(); c++) {
        sum += widths[c];
        assertThat(widths[c])
            .as(headers.get(c))
            .isGreaterThan(PdfColumnWidths.longestWord(headers.get(c), HEAD));
        if (row.get(c).matches("\\d{2}-\\w{3}-\\d{4}|[\\d,.]+")) {
          // Dates and amounts stay on one line.
          assertThat(widths[c])
              .as(row.get(c))
              .isGreaterThan(PdfColumnWidths.longestWord(row.get(c), BODY));
        }
      }
      assertThat(sum).isCloseTo(PageSize.A4.getHeight() - 80, within(1f));
    }
  }
}
