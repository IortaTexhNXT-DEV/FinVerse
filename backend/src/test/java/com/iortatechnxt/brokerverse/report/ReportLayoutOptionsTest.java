package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportLayoutOptions;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ChartImage;
import com.iortatechnxt.brokerverse.report.render.ReportChart;
import com.iortatechnxt.brokerverse.report.render.ReportChart.ChartKind;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/**
 * The layout of a report on the Layout panel (FR-CR-042): one or two grouping fields with a
 * subtotal and the number of records per group, a bar or line chart of a grouped amount or count,
 * no layout for a report whose layout is prescribed and no chart without a grouping field.
 */
class ReportLayoutOptionsTest {

  private static final UnaryOperator<String> SAME = UnaryOperator.identity();

  private static ReportResult premiums() {
    return new ReportResult(
        "NB-PREMIUM",
        "Premium Register",
        List.of("Company: HO"),
        List.of(
            ReportColumn.text("insurer", "Insurer"),
            ReportColumn.text("branch", "Branch"),
            ReportColumn.amount("premium", "Premium")),
        List.of(
            ReportRow.detail(Map.of("insurer", "Mutual", "branch", "Makati", "premium", 100)),
            ReportRow.detail(Map.of("insurer", "Pioneer", "branch", "Cebu", "premium", 50)),
            ReportRow.detail(Map.of("insurer", "Mutual", "branch", "Cebu", "premium", 25)),
            new ReportRow(RowKind.TOTAL, 0, "Total", Map.of("premium", 175))),
        List.of());
  }

  private static ReportMetadata metadata(ReportCategory category) {
    return new ReportMetadata(
        "NB-PREMIUM",
        "Premium Register",
        category,
        "Premiums",
        List.of(),
        Permission.REPORT_VIEW,
        null,
        false);
  }

  @Test
  void groupsTheRowsWithSubtotalsAndCounts() {
    ReportLayoutOptions layout =
        ReportLayoutOptions.of(Map.of(ReportLayoutOptions.GROUP_1, "insurer"));
    ReportResult grouped = layout.apply(premiums(), SAME);

    assertThat(grouped.rows())
        .filteredOn(r -> r.kind() == RowKind.GROUP_HEADER)
        .extracting(ReportRow::label)
        .containsExactly("Insurer : Mutual", "Insurer : Pioneer");
    assertThat(grouped.rows())
        .filteredOn(r -> r.kind() == RowKind.SUBTOTAL)
        .extracting(ReportRow::label)
        .containsExactly(
            "Total Insurer : Mutual (2 records)", "Total Insurer : Pioneer (1 record)");
    assertThat(grouped.rows())
        .filteredOn(r -> r.kind() == RowKind.SUBTOTAL)
        .extracting(r -> r.cells().get("premium"))
        .containsExactly(new BigDecimal("125"), new BigDecimal("50"));
    assertThat(grouped.rows())
        .filteredOn(r -> r.kind() == RowKind.TOTAL)
        .singleElement()
        .extracting(r -> r.cells().get("premium"))
        .isEqualTo(new BigDecimal("175"));
    assertThat(grouped.parameterEcho()).containsExactly("Company: HO");
  }

  @Test
  void groupsByTwoFieldsAndNamesThemWithTheMisLabels() {
    ReportLayoutOptions layout =
        ReportLayoutOptions.of(
            Map.of(ReportLayoutOptions.GROUP_1, "insurer", ReportLayoutOptions.GROUP_2, "branch"));
    ReportResult grouped =
        layout.apply(premiums(), own -> "Insurer".equals(own) ? "Insurer Name" : own);

    assertThat(grouped.rows())
        .filteredOn(r -> r.kind() == RowKind.GROUP_HEADER && r.level() == 1)
        .extracting(ReportRow::label)
        .containsExactly("Branch : Cebu", "Branch : Makati", "Branch : Cebu");
    assertThat(grouped.rows().get(0).label()).isEqualTo("Insurer Name : Mutual");
  }

  @Test
  void chartsTheGroupedAmountOrTheCount() {
    ReportChart amounts =
        ReportLayoutOptions.of(
                Map.of(
                    ReportLayoutOptions.GROUP_1, "insurer",
                    ReportLayoutOptions.CHART, "BAR",
                    ReportLayoutOptions.CHART_VALUE, "premium"))
            .chart(premiums(), SAME)
            .orElseThrow();
    assertThat(amounts.kind()).isEqualTo(ChartKind.BAR);
    assertThat(amounts.title()).isEqualTo("Premium by Insurer");
    assertThat(amounts.points())
        .extracting(ReportChart.Point::label, p -> p.value().intValue())
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Mutual", 125),
            org.assertj.core.groups.Tuple.tuple("Pioneer", 50));

    ReportChart counts =
        ReportLayoutOptions.of(
                Map.of(ReportLayoutOptions.GROUP_1, "branch", ReportLayoutOptions.CHART, "LINE"))
            .chart(premiums(), SAME)
            .orElseThrow();
    assertThat(counts.valueLabel()).isEqualTo("Number of records");
    assertThat(counts.points()).extracting(p -> p.value().intValue()).containsExactly(2, 1);
    assertThat(ChartImage.png(counts)).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
  }

  @Test
  void refusesAChartWithoutGroupingAndAPrescribedLayout() {
    ReportMetadata register = metadata(ReportCategory.NEW_BUSINESS);
    ReportLayoutOptions chartOnly =
        ReportLayoutOptions.of(Map.of(ReportLayoutOptions.CHART, "BAR"));
    assertThatThrownBy(() -> chartOnly.check(register))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Select a grouping field for the chart");

    ReportLayoutOptions grouped =
        ReportLayoutOptions.of(Map.of(ReportLayoutOptions.GROUP_1, "insurer"));
    assertThatThrownBy(() -> grouped.check(metadata(ReportCategory.TAX_STATUTORY)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("is prescribed");
    assertThat(metadata(ReportCategory.TAX_STATUTORY).layoutFixed()).isTrue();
    assertThat(register.layoutFixed()).isFalse();
    assertThat(ReportLayoutOptions.of(Map.of()).isEmpty()).isTrue();
  }

  @Test
  void keepsTheReportParametersApart() {
    assertThat(
            ReportLayoutOptions.reportParameters(
                Map.of("companyId", "1", ReportLayoutOptions.GROUP_1, "insurer")))
        .containsOnlyKeys("companyId");
  }
}
