package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Handling Fee Payment Classification ({@value #CODE}): handling fees billed with their payment
 * channel, status and official receipt.
 */
@Component
public class HandlingFeeReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-HANDLING-FEE";

  private static final String SQL =
      "select f.fee_no, f.pn_no, f.location_ref, f.billing_date, f.amount, f.currency, "
          + "f.channel, f.status, f.unapplied_ref, f.or_no, f.tagged_by, f.tagged_at, "
          + "f.applied_at from sbm_handling_fee f where f.company_id = :company"
          + String.format(SbmReportSupport.RANGE, "f.billing_date")
          + " order by f.billing_date, f.fee_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public HandlingFeeReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Handling Fee Payment Classification",
            "Handling fees billed with their payment channel, status and official receipt",
            "Billing",
            SQL,
            List.of(
                ReportColumn.text("fee_no", "Fee No."),
                ReportColumn.text("pn_no", "PN No."),
                ReportColumn.text("location_ref", "Location"),
                ReportColumn.date("billing_date", "Billing Date"),
                ReportColumn.amount("amount", "Amount"),
                ReportColumn.text("currency", "Currency"),
                ReportColumn.text("channel", "Channel"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text("unapplied_ref", "Payment"),
                ReportColumn.text("or_no", "OR No."),
                ReportColumn.text("tagged_by", "Tagged By"),
                ReportColumn.date("tagged_at", "Tagged"),
                ReportColumn.date("applied_at", "Applied"))));
  }
}
