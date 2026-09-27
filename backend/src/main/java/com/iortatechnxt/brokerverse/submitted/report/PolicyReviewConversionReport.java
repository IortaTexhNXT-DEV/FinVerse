package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Policy Review Conversion ({@value #CODE}): reviewed records by adequacy and conversion status.
 */
@Component
public class PolicyReviewConversionReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-PR-CONVERSION";

  private static final String SQL =
      "select p.segment, v.adequacy, coalesce(p.conversion_status, '') as "
          + "conversion_status, count(distinct p.id) as records, sum(p.sum_insured) as "
          + "sum_insured from sbm_iaaf_review v join sbm_policy p on p.id = v.policy_id where "
          + "p.company_id = :company"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "v.review_date")
          + " group by p.segment, v.adequacy, p.conversion_status order by p.segment, "
          + "v.adequacy";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public PolicyReviewConversionReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Policy Review Conversion",
            "Reviewed records by adequacy and conversion status",
            "Review",
            SQL,
            List.of(
                ReportColumn.text("segment", "Segment"),
                ReportColumn.text("adequacy", "Adequacy"),
                ReportColumn.text("conversion_status", "Conversion Status"),
                ReportColumn.count("records", "Records"),
                ReportColumn.amount("sum_insured", "Sum Insured"))));
  }
}
