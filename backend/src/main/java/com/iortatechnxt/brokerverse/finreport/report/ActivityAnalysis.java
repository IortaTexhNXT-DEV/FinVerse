package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared definitions of the activity analysis reports (FIN-ACT-*).
 *
 * <p>BrokerVerse keeps two analysis dimensions on every posting line: the <b>line of business</b>
 * (Main Head 1) and the <b>cost centre</b> (Main Head 2). The selected head is the activity; an
 * optional activity code restricts the report to one activity. Division = branch and department =
 * cost centre, as in all finance reports.
 */
final class ActivityAnalysis {

  static final String HEAD = "mainHead";
  static final String HEAD_1 = "HEAD_1_LINE_OF_BUSINESS";
  static final String HEAD_2 = "HEAD_2_COST_CENTRE";
  static final String ACTIVITY_CODE = "activityCode";
  static final String MAIN = "mainAccount";
  static final String ACTIVITY = "activityCaption";
  static final String FC_AMOUNT = "fcAmount";
  static final String NONE = "-";

  private ActivityAnalysis() {}

  /**
   * Parameters shared by both reports.
   *
   * @return specs
   */
  static List<ParameterSpec> parameters() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(FinParams.company());
    params.add(FinParams.from());
    params.add(FinParams.to());
    params.addAll(FinParams.mainRange());
    params.add(FinParams.division());
    params.add(FinParams.department());
    params.add(ParameterSpec.select(HEAD, "Main Head Number", List.of(HEAD_1, HEAD_2), HEAD_1));
    params.add(ParameterSpec.optional(ACTIVITY_CODE, "Main Activity Code", ParameterType.TEXT));
    params.add(FinParams.flag(FinParams.COMBINE, "Combine All Main A/cs"));
    return params;
  }

  /**
   * Whether the cost centre is the activity (head 2).
   *
   * @param p parameters
   * @return true for head 2
   */
  static boolean costCentreHead(ReportParameters p) {
    return HEAD_2.equals(p.text(HEAD));
  }

  /**
   * Dimension type holding the activity names.
   *
   * @param p parameters
   * @return dimension type name
   */
  static String dimensionType(ReportParameters p) {
    return costCentreHead(p) ? "COST_CENTER" : "BUSINESS_LINE";
  }

  /**
   * Activity code of a line.
   *
   * @param p parameters
   * @param businessLine line of business
   * @param costCenter cost centre
   * @return activity code or "-"
   */
  static String activity(ReportParameters p, String businessLine, String costCenter) {
    String code = costCentreHead(p) ? costCenter : businessLine;
    return code == null ? NONE : code;
  }

  /**
   * Whether an activity passes the activity code filter.
   *
   * @param p parameters
   * @param activity activity code
   * @return true when selected
   */
  static boolean selected(ReportParameters p, String activity) {
    return p.optionalText(ACTIVITY_CODE).map(activity::equals).orElse(true);
  }

  /**
   * Activity caption "code - name".
   *
   * @param code code
   * @param names names by code
   * @return caption
   */
  static String caption(String code, Map<String, String> names) {
    return code + " - " + names.getOrDefault(code, "(not analysed)");
  }

  /**
   * Adds the Division &gt; Department &gt; Main A/c grouping (main omitted when combined).
   *
   * @param b builder
   * @param p parameters
   * @return builder
   */
  static TabularReportBuilder groups(TabularReportBuilder b, ReportParameters p) {
    b.groupBy(Vouchers.DIVISION, "Division").groupBy(Vouchers.DEPARTMENT, "Department");
    if (!p.flag(FinParams.COMBINE)) {
      b.groupBy(MAIN, "Main A/c");
    }
    return b;
  }

  /**
   * Sort key of a row by the grouping levels.
   *
   * @param row row cells
   * @param combine whether main accounts are combined
   * @return sort key
   */
  static String sortKey(Map<String, Object> row, boolean combine) {
    return row.get(Vouchers.DIVISION)
        + "|"
        + row.get(Vouchers.DEPARTMENT)
        + "|"
        + (combine ? "" : row.get(MAIN))
        + "|"
        + row.get(ACTIVITY);
  }
}
