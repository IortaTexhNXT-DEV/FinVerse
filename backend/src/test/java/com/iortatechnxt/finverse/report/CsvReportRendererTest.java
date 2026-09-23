package com.iortatechnxt.finverse.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.render.CsvReportRenderer;
import com.iortatechnxt.finverse.report.render.ReportContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvReportRendererTest {

  @Test
  void escapesQuotesAndNeutralisesFormulas() {
    var result =
        new ReportResult(
            "X",
            "X",
            List.of(),
            List.of(ReportColumn.text("t", "Text"), ReportColumn.amount("a", "Amount")),
            List.of(
                ReportRow.detail(Map.of("t", "=HYPERLINK(\"x\")", "a", new BigDecimal("-1234.5")))),
            List.of());
    String csv =
        new String(
            new CsvReportRenderer().render(result, new ReportContext("C", "u", Instant.now())),
            StandardCharsets.UTF_8);
    assertThat(csv).contains("\"'=HYPERLINK(\"\"x\"\")\"");
    assertThat(csv).contains("\"(1,234.50)\"");
  }
}
