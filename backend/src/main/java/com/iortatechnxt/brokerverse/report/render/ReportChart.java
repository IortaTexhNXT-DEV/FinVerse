package com.iortatechnxt.brokerverse.report.render;

import java.math.BigDecimal;
import java.util.List;

/**
 * The chart of a report layout (FR-CR-042): a bar or line chart of one value per group, drawn in
 * the PDF and Excel files.
 *
 * @param kind bar or line
 * @param title chart title, e.g. "Premium by Insurer"
 * @param valueLabel what the values are, e.g. "Premium" or "Number of records"
 * @param points one value per group, in the order of the groups
 */
public record ReportChart(ChartKind kind, String title, String valueLabel, List<Point> points) {

  /** Copies the points. */
  public ReportChart {
    points = List.copyOf(points);
  }

  /** The kinds of chart. */
  public enum ChartKind {
    /** No chart. */
    NONE,
    /** Bar chart. */
    BAR,
    /** Line chart. */
    LINE
  }

  /**
   * One value of the chart.
   *
   * @param label group value
   * @param value total of the group
   */
  public record Point(String label, BigDecimal value) {}
}
