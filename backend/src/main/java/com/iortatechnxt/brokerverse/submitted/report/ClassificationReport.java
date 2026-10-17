package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Account Classification Report ({@value #CODE}): records per segment, business type and
 * classification with their sum insured.
 */
@Component
public class ClassificationReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-CLASSIFICATION";

  private static final String SQL =
      "select p.segment, p.business_type, coalesce(p.classification, 'UNCLASSIFIED') as "
          + "classification, count(*) as records, sum(p.sum_insured) as sum_insured from "
          + "sbm_policy p where p.company_id = :company"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "p.date_received")
          + " group by p.segment, p.business_type, p.classification order by p.segment, "
          + "p.business_type, 3";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public ClassificationReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Account Classification Report",
            "Records per segment, business type and classification with their sum insured",
            "Date Received",
            SQL,
            List.of(
                ReportColumn.text("segment", "Segment"),
                ReportColumn.text("business_type", "Business Type"),
                ReportColumn.text("classification", "Classification"),
                ReportColumn.count("records", "Records"),
                ReportColumn.amount("sum_insured", "Sum Insured"))));
  }
}
