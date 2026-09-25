package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.report.core.ExportOptions;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.render.PrintOptions;
import com.iortatechnxt.brokerverse.report.render.PrintOptions.Orientation;
import com.iortatechnxt.brokerverse.report.render.PrintOptions.Paper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Print options (FRBS 2.4.9) and column filters of exports (FRBS 2.4.4). */
class ExportOptionsTest {

  private static ReportResult result() {
    return new ReportResult(
        "T-1",
        "Test",
        List.of(),
        List.of(ReportColumn.text("code", "Code"), ReportColumn.amount("amount", "Amount")),
        List.of(
            ReportRow.section("Assets"),
            ReportRow.detail(Map.of("code", "1111", "amount", new BigDecimal("1500.00"))),
            ReportRow.detail(Map.of("code", "4100", "amount", new BigDecimal("20.00")))),
        List.of());
  }

  @Test
  void printOptionsParseLeniently() {
    assertThat(PrintOptions.of(null, " ", null)).isEqualTo(PrintOptions.DEFAULT);
    assertThat(PrintOptions.DEFAULT.echo()).isEmpty();
    PrintOptions letter = PrintOptions.of("letter", "landscape", false);
    assertThat(letter.paper()).isEqualTo(Paper.LETTER);
    assertThat(letter.orientation()).isEqualTo(Orientation.LANDSCAPE);
    assertThat(letter.echo()).isEqualTo("Print: LETTER LANDSCAPE, natural width");
    assertThat(PrintOptions.of("B5", "SIDEWAYS", true)).isEqualTo(PrintOptions.DEFAULT);
  }

  @Test
  void columnFiltersKeepMatchingDetailRows() {
    assertThat(ExportOptions.NONE.filter(result()).rows()).hasSize(3);
    Map<String, String> filters = ExportOptions.parseFilters(List.of("code:11", "nocolon", ":x"));
    assertThat(filters).containsOnlyKeys("code");
    ReportResult filtered = new ExportOptions(PrintOptions.DEFAULT, filters).filter(result());
    assertThat(filtered.rows()).hasSize(1);
    assertThat(filtered.notes()).singleElement().asString().contains("1 row(s)");
    ReportResult byAmount =
        new ExportOptions(null, Map.of("amount", "1,500", "unknown", "x", "code", " "))
            .filter(result());
    assertThat(byAmount.rows()).extracting(r -> r.cells().get("code")).containsExactly("1111");
  }
}
