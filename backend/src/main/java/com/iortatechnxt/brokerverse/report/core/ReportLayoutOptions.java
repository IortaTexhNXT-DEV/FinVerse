package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.report.render.ReportChart;
import com.iortatechnxt.brokerverse.report.render.ReportChart.ChartKind;
import com.iortatechnxt.brokerverse.report.render.ReportChart.Point;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * The layout a user gives a report on the Layout panel of the report runner (FR-CR-042): one or two
 * grouping fields with subtotals and counts per group, and one bar or line chart over a grouped
 * amount or count. It travels with the parameters of the run under the {@code layout.} keys, so it
 * is saved in a variant and applied to the screen, the exports and the subscriptions. It changes
 * the presentation only: the rows are those of the report.
 *
 * @param groups grouping column keys, outermost first (at most two)
 * @param chart chart kind, NONE for no chart
 * @param chartValue column key of the charted amount, or {@link #COUNT}
 */
public record ReportLayoutOptions(List<String> groups, ChartKind chart, String chartValue) {

  /** Parameter of the first grouping field. */
  public static final String GROUP_1 = "layout.group1";

  /** Parameter of the second grouping field. */
  public static final String GROUP_2 = "layout.group2";

  /** Parameter of the chart kind (BAR, LINE). */
  public static final String CHART = "layout.chart";

  /** Parameter of the charted value (an amount column, or COUNT). */
  public static final String CHART_VALUE = "layout.chartValue";

  /** Exception code of a layout the report cannot take. */
  private static final String LAYOUT_CODE = "REPORT_LAYOUT";

  /** Charted value: the number of records of each group. */
  public static final String COUNT = "COUNT";

  /** No layout: the report as it is defined. */
  public static final ReportLayoutOptions NONE =
      new ReportLayoutOptions(List.of(), ChartKind.NONE, null);

  /** Copies the groups. */
  public ReportLayoutOptions {
    groups = List.copyOf(groups);
    chart = chart == null ? ChartKind.NONE : chart;
  }

  /**
   * Reads the layout from the parameters of a run.
   *
   * @param raw parameters of the run
   * @return layout, {@link #NONE} when none is given
   */
  public static ReportLayoutOptions of(Map<String, String> raw) {
    if (raw == null) {
      return NONE;
    }
    List<String> groups = new ArrayList<>();
    for (String key : List.of(GROUP_1, GROUP_2)) {
      String value = raw.get(key);
      if (value != null && !value.isBlank() && !groups.contains(value.strip())) {
        groups.add(value.strip());
      }
    }
    String value = raw.get(CHART_VALUE);
    return new ReportLayoutOptions(
        groups,
        chartKind(raw.get(CHART)),
        value == null || value.isBlank() ? COUNT : value.strip());
  }

  private static ChartKind chartKind(String chart) {
    if (chart == null || chart.isBlank()) {
      return ChartKind.NONE;
    }
    return switch (chart.strip()) {
      case "BAR" -> ChartKind.BAR;
      case "LINE" -> ChartKind.LINE;
      case "NONE" -> ChartKind.NONE;
      default -> throw new BusinessRuleException(LAYOUT_CODE, "Choose a bar or line chart");
    };
  }

  /**
   * Whether the layout changes nothing.
   *
   * @return true without grouping and chart
   */
  public boolean isEmpty() {
    return groups.isEmpty() && chart == ChartKind.NONE;
  }

  /**
   * The parameters of the run without the layout keys (what the report itself reads).
   *
   * @param raw parameters of the run
   * @return parameters of the report
   */
  public static Map<String, String> reportParameters(Map<String, String> raw) {
    Map<String, String> own = new LinkedHashMap<>();
    if (raw != null) {
      raw.forEach(
          (k, v) -> {
            if (k != null && !k.startsWith("layout.")) {
              own.put(k, v);
            }
          });
    }
    return own;
  }

  /**
   * Checks the layout against a report: a report whose layout is prescribed takes none, a chart
   * needs a grouping field.
   *
   * @param metadata report
   */
  public void check(ReportMetadata metadata) {
    if (isEmpty()) {
      return;
    }
    if (metadata.layoutFixed()) {
      throw new BusinessRuleException(
          "REPORT_LAYOUT_FIXED", "The layout of " + metadata.title() + " is prescribed");
    }
    if (groups.size() > 2) {
      throw new BusinessRuleException(LAYOUT_CODE, "Choose at most two grouping fields");
    }
    if (chart != ChartKind.NONE && groups.isEmpty()) {
      throw new BusinessRuleException(LAYOUT_CODE, "Select a grouping field for the chart");
    }
  }

  /**
   * Lays the result out again: the detail rows grouped by the grouping fields, with a subtotal and
   * the count of records per group and a grand total.
   *
   * @param result result of the report
   * @param labels the label a column is shown with (an MIS field label where one names it)
   * @return result laid out, the same result without grouping
   */
  public ReportResult apply(ReportResult result, UnaryOperator<String> labels) {
    if (groups.isEmpty()) {
      return result;
    }
    TabularReportBuilder b = TabularReportBuilder.regroup(result);
    for (String key : groups) {
      b.groupBy(key, labels.apply(columnOf(result, key).label()));
    }
    return b.build();
  }

  /**
   * The chart of the layout: one point per value of the first grouping field, in the order of the
   * groups, with the total of the charted amount or the number of records.
   *
   * @param result result of the report
   * @param labels the label a column is shown with
   * @return chart, empty without a chart
   */
  public Optional<ReportChart> chart(ReportResult result, UnaryOperator<String> labels) {
    if (chart == ChartKind.NONE || groups.isEmpty()) {
      return Optional.empty();
    }
    ReportColumn group = columnOf(result, groups.get(0));
    boolean count = COUNT.equals(chartValue);
    ReportColumn value = count ? null : columnOf(result, chartValue);
    if (value != null && !value.summed()) {
      throw new BusinessRuleException(
          LAYOUT_CODE, "Chart an amount that the report totals, or the number of records");
    }
    List<Point> list = new ArrayList<>();
    totals(result, group, value).forEach((label, total) -> list.add(new Point(label, total)));
    String valueLabel = count ? "Number of records" : labels.apply(value.label());
    return Optional.of(
        new ReportChart(
            chart, valueLabel + " by " + labels.apply(group.label()), valueLabel, list));
  }

  /** The total of the value (or the count, without a value) per group, in group order. */
  private static Map<String, BigDecimal> totals(
      ReportResult result, ReportColumn group, ReportColumn value) {
    Map<String, BigDecimal> points = new LinkedHashMap<>();
    result.rows().stream()
        .filter(r -> r.kind() == RowKind.DETAIL)
        .sorted(
            (a, b) ->
                String.valueOf(a.cells().get(group.key()))
                    .compareTo(String.valueOf(b.cells().get(group.key()))))
        .forEach(
            r ->
                points.merge(
                    String.valueOf(TabularReportBuilder.codeLabel(r.cells().get(group.key()))),
                    value == null ? BigDecimal.ONE : amount(r.cells().get(value.key())),
                    BigDecimal::add));
    return points;
  }

  private static BigDecimal amount(Object v) {
    return v instanceof Number n ? new BigDecimal(n.toString()) : BigDecimal.ZERO;
  }

  /**
   * The words of the layout for the notes of the result.
   *
   * @param result result of the report
   * @param labels the label a column is shown with
   * @return note, empty without a layout
   */
  public Optional<String> note(ReportResult result, UnaryOperator<String> labels) {
    if (isEmpty()) {
      return Optional.empty();
    }
    List<String> names =
        groups.stream().map(k -> labels.apply(columnOf(result, k).label())).toList();
    String text = "Layout: grouped by " + String.join(", then ", names);
    if (chart != ChartKind.NONE) {
      String what =
          COUNT.equals(chartValue)
              ? "number of records"
              : labels.apply(columnOf(result, chartValue).label());
      text +=
          "; "
              + (chart == ChartKind.BAR ? "bar" : "line")
              + " chart of the "
              + what
              + " in the PDF and Excel files";
    }
    return Optional.of(text);
  }

  private static ReportColumn columnOf(ReportResult result, String key) {
    return result.columns().stream()
        .filter(c -> c.key().equals(key))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    LAYOUT_CODE, "The report has no field " + key + " to group or chart"));
  }
}
