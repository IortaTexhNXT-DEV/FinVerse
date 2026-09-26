package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.OdsReportRenderer;
import com.iortatechnxt.brokerverse.report.render.PdfReportRenderer;
import com.iortatechnxt.brokerverse.report.render.ReportContext;
import com.iortatechnxt.brokerverse.report.render.XmlReportRenderer;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/** ODS and XML exports (BRNB.037) and the PDF print metadata (BRNB.031). */
class OdsXmlReportRendererTest {

  private static final ReportContext CONTEXT =
      new ReportContext("SIT Brokers <&>", "tester", Instant.parse("2026-09-24T01:00:00Z"), "");

  private static ReportResult result() {
    Map<String, Object> cells = new HashMap<>();
    cells.put("t", "Santos & Co <b>");
    cells.put("a", new BigDecimal("-1234.50"));
    cells.put("d", LocalDate.parse("2026-09-15"));
    return new ReportResult(
        "NB-X",
        "Test report",
        List.of("From : 2026-09-01", "To : 2026-09-30"),
        List.of(
            ReportColumn.text("t", "Text"),
            ReportColumn.amount("a", "Amount"),
            ReportColumn.date("d", "Date")),
        List.of(
            ReportRow.detail(cells),
            new ReportRow(
                RowKind.TOTAL, 0, "Grand Total", Map.of("a", new BigDecimal("-1234.50")))),
        List.of("A note"));
  }

  @Test
  void odsIsAnOpenDocumentSpreadsheetWithTypedCells() throws IOException {
    byte[] ods = new OdsReportRenderer().render(result(), CONTEXT);
    String content = null;
    String mimetype = null;
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(ods))) {
      for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
        String text = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
        if ("mimetype".equals(e.getName())) {
          mimetype = text;
        } else if ("content.xml".equals(e.getName())) {
          content = text;
        }
      }
    }
    assertThat(mimetype).isEqualTo("application/vnd.oasis.opendocument.spreadsheet");
    assertThat(content)
        .contains("SIT Brokers &lt;&amp;")
        .contains("Santos &amp; Co &lt;b")
        .contains("office:value=\"-1234.50\"")
        .contains("office:date-value=\"2026-09-15\"")
        .contains("From : 2026-09-01")
        .contains("Grand Total")
        .contains("Note: A note");
  }

  @Test
  void xmlCarriesMetadataColumnsAndPlainValues() {
    String xml =
        new String(new XmlReportRenderer().render(result(), CONTEXT), StandardCharsets.UTF_8);
    assertThat(xml)
        .startsWith("<?xml")
        .contains("<report code=\"NB-X\" title=\"Test report\">")
        .contains("<generatedBy>tester</generatedBy>")
        .contains("<filter>From : 2026-09-01</filter>")
        .contains("<column key=\"a\" label=\"Amount\" type=\"AMOUNT\"")
        .contains("<cell key=\"a\">-1234.50</cell>")
        .contains("<cell key=\"d\">2026-09-15</cell>")
        .contains("Santos &amp; Co &lt;b")
        .contains("<row kind=\"TOTAL\" level=\"0\" label=\"Grand Total\">")
        .contains("<note>A note</note>");
  }

  @Test
  void pdfPrintsTheMetadataBlockWithTheFilters() throws IOException {
    byte[] pdf = new PdfReportRenderer().render(result(), CONTEXT);
    try (PdfReader reader = new PdfReader(pdf)) {
      String page = new PdfTextExtractor(reader).getTextFromPage(1);
      assertThat(page)
          .contains("Report ID: NB-X")
          .contains("User ID: tester")
          .contains("Run Date: 24-09-2026 09:00")
          .contains("Filters: From : 2026-09-01");
    }
  }
}
