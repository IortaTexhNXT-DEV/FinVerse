package com.iortatechnxt.brokerverse.renewal.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import org.springframework.stereotype.Component;

/**
 * Expiring list (RNW-EXPIRY-LIST; FR-RN-014, BRRN.004/007/008): the rows and columns of the Expiry
 * List grid (FRS "Expiring list columns") with the chosen criteria.
 */
@Component
public class ExpiryListReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-EXPIRY-LIST";

  private static final String SQL =
      "select c.renewal_ref, c.expiring_invoice_no, c.cover_no, c.version_no, c.branch_code,"
          + " c.department_code, c.unit_head, c.client_name, c.assured_name, c.business_origin,"
          + " c.account_officer, c.expiring_policy_no, c.insurer_code, c.product_code, c.product_name,"
          + " c.stage, c.disposition, c.disposition_remarks, c.bucket, c.expiry_date, c.gross_premium,"
          + " (select r.message from rnw_check_result r where r.run_id = c.last_check_run_id"
          + " and r.check_code = 'CLAIMS') as claims,"
          + " (select r.detail from rnw_check_result r where r.run_id = c.last_check_run_id"
          + " and r.check_code = 'OUTSTANDING_PREMIUM' and r.outcome = 'FAIL') as outstanding,"
          + " concat_ws(', ', case when c.urgent then 'Urgent' end, case when c.returned then 'Returned' end,"
          + " case when c.transferred then 'Transferred' end, case when c.endorsement_pending then 'Endorsed' end,"
          + " case when c.claims_flag then 'Claims' end, case when c.outstanding_flag then 'Outstanding' end,"
          + " case when c.kyc_due then 'KYC due' end, case when c.nrns then 'NRNS' end) as flags"
          + " from rnw_candidate c where 1 = 1"
          + RenewalReportSupport.FILTERS
          + " order by c.expiry_date, c.renewal_ref";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public ExpiryListReport(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE,
        "Renewal Expiring List",
        "The expiring accounts of the Expiry List with their status");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var rows =
        jdbc.rows(SQL, support.args(p).map()).stream().map(RenewalReportSupport::labels).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("renewal_ref", "Renewal Reference"),
            ReportColumn.text("expiring_invoice_no", "Expiring Invoice"),
            ReportColumn.text("cover_no", "Cover No."),
            ReportColumn.text("branch_code", "Invoicing Branch"),
            ReportColumn.text("department_code", "Department"),
            ReportColumn.text("unit_head", "Unit Head"),
            ReportColumn.text("client_name", "Client"),
            ReportColumn.text("assured_name", "Assured"),
            ReportColumn.text("business_origin", "Business Origin"),
            ReportColumn.text("account_officer", "Account Officer"),
            ReportColumn.text("expiring_policy_no", "Expiring Policy No."),
            ReportColumn.text("insurer_code", "Insurance Company"),
            ReportColumn.text("product_code", "Risk Code"),
            ReportColumn.text("product_name", "Risk Name"),
            ReportColumn.text("stage", "Renewal Status"),
            ReportColumn.text("disposition", "Disposition"),
            ReportColumn.text("disposition_remarks", "Disposition Remarks"),
            ReportColumn.text("claims", "Claims"),
            ReportColumn.text("outstanding", "Outstanding Premium"),
            ReportColumn.text("bucket", "Classification"),
            ReportColumn.text("flags", "Flags"),
            ReportColumn.date("expiry_date", "Expiry Date"),
            ReportColumn.amount("gross_premium", "Gross Premium"))
        .rows(rows)
        .presorted()
        .build();
  }
}
