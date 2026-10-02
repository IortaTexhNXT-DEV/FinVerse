package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TabularReportBuilderTest {

  private final ReportParameters params =
      ReportParameters.validate(
          new ReportMetadata(
              "T", "Test", ReportCategory.CONTROL, "", List.of(), Permission.REPORT_VIEW),
          Map.of(),
          Clock.systemUTC());

  @Test
  void groupsProduceHeadersSubtotalsAndGrandTotal() {
    var result =
        TabularReportBuilder.of(params)
            .columns(ReportColumn.text("p", "Policy"), ReportColumn.amount("amt", "Amount"))
            .groupBy("branch", "Branch")
            .groupBy("lob", "Class")
            .rows(
                List.of(
                    Map.of("branch", "HO", "lob", "FIRE", "p", "P1", "amt", new BigDecimal("100")),
                    Map.of("branch", "HO", "lob", "MOTOR", "p", "P2", "amt", new BigDecimal("50")),
                    Map.of("branch", "CEB", "lob", "FIRE", "p", "P3", "amt", new BigDecimal("25"))))
            .build();

    List<RowKind> kinds = result.rows().stream().map(ReportRow::kind).toList();
    assertThat(kinds.getFirst()).isEqualTo(RowKind.GROUP_HEADER);
    assertThat(kinds.getLast()).isEqualTo(RowKind.TOTAL);
    assertThat(result.rows().getLast().cells().get("amt")).isEqualTo(new BigDecimal("175"));
    assertThat(result.rows())
        .filteredOn(r -> r.kind() == RowKind.SUBTOTAL && r.level() == 0)
        .extracting(r -> r.cells().get("amt"))
        .containsExactly(new BigDecimal("25"), new BigDecimal("150"));
  }

  @Test
  void emptyReportHasNoTotals() {
    var result =
        TabularReportBuilder.of(params)
            .columns(ReportColumn.amount("a", "A"))
            .rows(List.of())
            .build();
    assertThat(result.rows()).isEmpty();
  }
}
