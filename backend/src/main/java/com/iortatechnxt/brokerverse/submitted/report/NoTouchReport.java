package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Submitted Policies No Touch ({@value #CODE}): No Touch records per batch with the service fee
 * returned by the insurer.
 */
@Component
public class NoTouchReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-NO-TOUCH";

  private static final String SQL =
      "select b.batch_no, b.insurer_code, b.period, b.status, l.sbm_no, l.pn_no, "
          + "l.assured_name, l.policy_no, l.plate_no, l.sum_insured, l.basic_premium, "
          + "l.gross_fee, l.vat, l.wtax, b.si_no from sbm_no_touch_line l join "
          + "sbm_no_touch_batch b on b.id = l.batch_id where b.company_id = :company"
          + String.format(SbmReportSupport.RANGE, "b.created_at")
          + " order by b.batch_no, l.sbm_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public NoTouchReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Submitted Policies No Touch",
            "No Touch records per batch with the service fee returned by the insurer",
            "Exported",
            SQL,
            List.of(
                ReportColumn.text("batch_no", "Batch"),
                ReportColumn.text("insurer_code", "Insurer"),
                ReportColumn.text("period", "Month"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("pn_no", "PN No."),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.text("policy_no", "Policy No."),
                ReportColumn.text("plate_no", "Plate No."),
                ReportColumn.amount("sum_insured", "Sum Insured"),
                ReportColumn.amount("basic_premium", "Basic Premium"),
                ReportColumn.amount("gross_fee", "Gross Fee"),
                ReportColumn.amount("vat", "VAT"),
                ReportColumn.amount("wtax", "Withholding Tax"),
                ReportColumn.text("si_no", "Service Invoice"))));
  }
}
