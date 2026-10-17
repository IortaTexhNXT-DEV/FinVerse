package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Expiring Accounts and Uninsured Loans ({@value #CODE}; FR-SP-083): part 1 lists the masterlist
 * accounts whose policy expires in the period (the reporting month), part 2 the active loans of the
 * latest loan file of each loan report on or before the end of the period that have no policy in
 * force on that day - no masterlist record and no BIBS account covering it - with the expiry of the
 * last policy known. Both parts follow the user's scope.
 */
@Component
public class ExpiringUninsuredReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-EXPIRING-UNINSURED";

  private static final String MONTH_END = "coalesce(cast(:to as date), current_date)";

  private static final String EXPIRING =
      "select 1 as part_no, 'Expiring in the period' as part, p.sbm_no, p.assured_name as name, "
          + "p.segment, p.classification, p.bucket, p.renewal_tag, p.insurer_code, p.policy_no, "
          + "p.expiry_date, p.handler_username as handler, p.status, cast(null as varchar) as "
          + "loan_report, p.pn_no, p.loan_application_no, p.referring_branch as branch "
          + "from sbm_policy p where p.company_id = :company"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "p.expiry_date");

  private static final String UNINSURED =
      "select 2 as part_no, 'Active loan without a policy in force' as part, "
          + "cast(null as varchar) as sbm_no, l.borrower_name as name, l.segment, "
          + "cast(null as varchar) as classification, cast(null as varchar) as bucket, "
          + "cast(null as varchar) as renewal_tag, cast(null as varchar) as insurer_code, "
          + "cast(null as varchar) as policy_no, (select max(q.expiry_date) from sbm_policy q "
          + "where q.company_id = l.company_id and (q.pn_no = l.pn_no or q.loan_application_no = "
          + "l.loan_application_no)) as expiry_date, cast(null as varchar) as handler, "
          + "l.loan_status as status, l.loan_report, l.pn_no, l.loan_application_no, l.branch "
          + "from sbm_lamd_loan l join (select loan_report, max(snapshot_date) as latest from "
          + "sbm_lamd_loan where company_id = :company and snapshot_date <= "
          + MONTH_END
          + " group by loan_report) f on f.loan_report = l.loan_report and f.latest = "
          + "l.snapshot_date where l.company_id = :company and l.loan_status = 'ACTIVE'"
          + SbmReportSupport.LOAN_SCOPE
          + " and not exists (select 1 from sbm_policy q where q.company_id = l.company_id and "
          + "(q.pn_no = l.pn_no or q.loan_application_no = l.loan_application_no) and "
          + "q.inception_date <= "
          + MONTH_END
          + " and q.expiry_date >= "
          + MONTH_END
          + ") and not exists (select 1 from acc_account a left join acc_account_pn n on "
          + "n.account_id = a.id where a.company_id = l.company_id and (upper(n.pn_number) = "
          + "upper(l.pn_no) or a.loan_application_no = l.loan_application_no) and a.period_from "
          + "<= "
          + MONTH_END
          + " and a.period_to >= "
          + MONTH_END
          + ")";

  private static final String SQL =
      EXPIRING + " union all " + UNINSURED + " order by part_no, expiry_date, sbm_no, pn_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public ExpiringUninsuredReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Expiring Accounts and Uninsured Loans",
            "Part 1: masterlist accounts whose policy expires in the period. Part 2: active loans"
                + " of the latest loan files without a policy in force at the end of the period",
            "Reporting Period",
            SQL,
            List.of(
                ReportColumn.text("part", "Part"),
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("name", "Assured / Borrower"),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.text("classification", "Classification"),
                ReportColumn.text("bucket", "Bucket"),
                ReportColumn.text("renewal_tag", "Renewal Tag"),
                ReportColumn.text("insurer_code", "Insurer"),
                ReportColumn.text("policy_no", "Policy No."),
                ReportColumn.date("expiry_date", "Policy Expiry / Last Policy Expiry"),
                ReportColumn.text("handler", "Handler"),
                ReportColumn.text("status", "Renewal / Loan Status"),
                ReportColumn.text("loan_report", "Loan Report"),
                ReportColumn.text("pn_no", "PN No."),
                ReportColumn.text("loan_application_no", "Loan Application No."),
                ReportColumn.text("branch", "Branch"))));
  }
}
