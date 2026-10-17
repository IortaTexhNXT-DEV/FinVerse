package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Submitted Masterlist ({@value #CODE}): the masterlist records with their loan, policy, risk,
 * classification and renewal fields.
 */
@Component
public class MasterlistReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-MASTERLIST";

  private static final String SQL =
      "select p.sbm_no, p.segment, p.business_type, p.source_code, p.date_received, "
          + "p.pn_no, p.cif, p.borrower_name, p.assured_name, p.insurer_code, p.policy_no, "
          + "p.inception_date, p.expiry_date, p.sum_insured, p.total_premium, p.plate_no, "
          + "p.property_location, p.classification, p.bucket, p.renewal_tag, p.status, "
          + "p.handler_username, p.ao_username, p.conversion_status, case when p.migrated "
          + "then 'Yes' else 'No' end as migrated from sbm_policy p where p.company_id = "
          + ":company"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "p.date_received")
          + " order by p.sbm_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public MasterlistReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Submitted Masterlist",
            "The masterlist records with their loan, policy, risk, classification and renewal fields",
            "Date Received",
            SQL,
            List.of(
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.text("business_type", "Business Type"),
                ReportColumn.text("source_code", "Source"),
                ReportColumn.date("date_received", "Date Received"),
                ReportColumn.text("pn_no", "PN No."),
                ReportColumn.text("cif", "CIF"),
                ReportColumn.text("borrower_name", "Borrower"),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.text("insurer_code", "Insurer"),
                ReportColumn.text("policy_no", "Policy No."),
                ReportColumn.date("inception_date", "Inception"),
                ReportColumn.date("expiry_date", "Expiry"),
                ReportColumn.amount("sum_insured", "Sum Insured"),
                ReportColumn.amount("total_premium", "Total Premium"),
                ReportColumn.text("plate_no", "Plate No."),
                ReportColumn.text("property_location", "Property Location"),
                ReportColumn.text("classification", "Classification"),
                ReportColumn.text("bucket", "Bucket"),
                ReportColumn.text("renewal_tag", "Renewal Tag"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text("handler_username", "Handler"),
                ReportColumn.text("ao_username", "Account Officer"),
                ReportColumn.text("conversion_status", "Conversion Status"),
                ReportColumn.text("migrated", "Migrated"))));
  }
}
