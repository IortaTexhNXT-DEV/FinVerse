package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.render.PdfReportRenderer;
import com.iortatechnxt.brokerverse.report.render.ReportContext;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdfReportRendererTest {

  private static final ReportResult RESULT =
      new ReportResult(
          "T-1",
          "Footer test",
          List.of(),
          List.of(ReportColumn.text("t", "Text")),
          List.of(ReportRow.detail(Map.of("t", "row"))),
          List.of());

  private static String firstPage(String footer) throws IOException {
    byte[] pdf =
        new PdfReportRenderer()
            .render(RESULT, new ReportContext("Company", "tester", Instant.now(), footer));
    try (PdfReader reader = new PdfReader(pdf)) {
      return new PdfTextExtractor(reader).getTextFromPage(1);
    }
  }

  @Test
  void printsTheFooterTextNextToThePageNumber() throws IOException {
    String page = firstPage("  Confidential - internal use  ");
    assertThat(page).contains("Confidential - internal use").contains("Page 1 of");
  }

  @Test
  void shortensAFooterThatWouldReachThePageNumber() throws IOException {
    String page = firstPage("Long footer ".repeat(40));
    assertThat(page).contains("Long footer Long footer").contains("...").contains("Page 1 of");
  }

  @Test
  void printsNoFooterTextWhenBlank() throws IOException {
    assertThat(firstPage(null)).contains("Page 1 of").doesNotContain("...");
  }
}
