package com.iortatechnxt.brokerverse.cashiering.report;

import com.iortatechnxt.brokerverse.cashiering.report.SqlReport.Spec;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Cashiering reports of the posting and disposition records (FRS.CSH.09.02.18, 09.02.21): the
 * payment reversals (cancellation and reinstatement records posted, and the cancellations and
 * reinstatements approved on the receipt) and the summary of the unapplied payments per
 * disposition.
 */
@Configuration(proxyBeanMethods = false)
public class CashieringPostingReports {

  private static final String AMOUNT = "amount";
  private static final String ZONE = "' as date)";

  private static final String REVERSALS =
      "select x.record_no, x.kind, x.receipt_no, x.account_no, x.assured, x.amount, x.reason,"
          + " coalesce(c.full_name, x.created_by) as created_by,"
          + " coalesce(p.full_name, x.posted_by) as posted_by, x.posted_on from ("
          + "select r.record_no, case r.record_kind when 'CANCELLATION' then 'Cancellation'"
          + " else 'Reinstatement' end as kind, r.receipt_no,"
          + " coalesce(a.reference, r.invoice_no) as account_no,"
          + " coalesce(r.client_name, r.insurer_name, r.payor_name) as assured,"
          + " coalesce(a.amount, r.amount) as amount,"
          + " coalesce((select v.label from lov_value v where v.code = r.reason_code"
          + " and v.type_code in ('RECEIPT_CANCEL_REASON', 'REINSTATEMENT_REASON')"
          + " order by v.effective_from desc limit 1), r.reason_code)"
          + " || coalesce(': ' || r.reason_text, '') as reason,"
          + " r.created_by, r.posted_by, cast(r.posted_at at time zone '"
          + BusinessClock.zoneId()
          + ZONE
          + " as posted_on, r.posted_at as at from csh_receipt_record r"
          + " left join csh_receipt_record_account a on a.record_id = r.id"
          + " where r.company_id = :company and r.stage = 'POSTED'"
          + " and r.record_kind in ('CANCELLATION', 'REINSTATEMENT')"
          + " union all select x.transaction_no, case x.action when 'CANCEL' then 'Cancellation'"
          + " else 'Reinstatement' end, rc.receipt_no, x.invoice_no,"
          + " coalesce(rc.assured_name, rc.payor_name), coalesce(x.amount, rc.amount),"
          + " coalesce((select v.label from lov_value v where v.code = x.reason_code"
          + " and v.type_code in ('RECEIPT_CANCEL_REASON', 'REINSTATEMENT_REASON')"
          + " order by v.effective_from desc limit 1), x.reason_code),"
          + " x.created_by, x.approved_by, cast(x.approved_at at time zone '"
          + BusinessClock.zoneId()
          + ZONE
          + ", x.approved_at from csh_receipt_action x join csh_receipt rc on rc.id = x.receipt_id"
          + " where x.company_id = :company and x.stage = 'POSTED'"
          + " and not exists (select 1 from csh_receipt_record rr"
          + " where rr.record_no = x.transaction_no)"
          + ") x left join sec_user c on c.username = x.created_by"
          + " left join sec_user p on p.username = x.posted_by"
          + " where x.posted_on between :from and :to order by x.at, x.record_no";

  private static final String DISPOSITIONS =
      "select coalesce(t.description, d.disposition_type) as disposition_type, u.reference,"
          + " i.policy_no, coalesce(u.payor_name, u.client_code) as client_name,"
          + " coalesce(ins.name, i.insurer_code) as insurer_name, d.amount,"
          + " coalesce(d.remarks, d.reversal_reason) as reason,"
          + " coalesce(cu.full_name, d.created_by) as requested_by,"
          + " case d.status when 'FOR_APPROVAL' then 'For approval' when 'IN_PROCESS' then"
          + " 'Approved, in process' when 'COMPLETED' then 'Approved' when 'REVERSED' then"
          + " 'Reversed' when 'WITHDRAWN' then 'Withdrawn' when 'FOR_REVERSAL' then 'For reversal'"
          + " else 'Monitoring' end as approval_status,"
          + " cast(coalesce(d.completed_at, d.approved_at, d.created_at) at time zone '"
          + BusinessClock.zoneId()
          + ZONE
          + " as action_on, d.remarks, 1 as cnt from csh_disposition d"
          + " join csh_unapplied u on u.id = d.unapplied_id"
          + " left join csh_disposition_type_rule t on t.type_code = d.disposition_type"
          + " left join ops_invoice i on i.invoice_no = d.target_invoice_no"
          + " left join pty_party ins on ins.company_id = u.company_id and ins.code = i.insurer_code"
          + " left join sec_user cu on cu.username = d.created_by"
          + " where u.company_id = :company and cast(d.created_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) between :from and :to"
          + " order by disposition_type, d.created_at, d.id";

  /**
   * Payment Reversals (FRS.CSH.09.02.21).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport paymentReversalReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-PAYMENT-REVERSAL",
            "Payment Reversals",
            "Cancellation and reinstatement records posted in the period",
            REVERSALS,
            List.of(
                ReportColumn.text("record_no", "Record Number"),
                ReportColumn.text("kind", "Type"),
                ReportColumn.text("receipt_no", "AR/OR Number"),
                ReportColumn.text("account_no", "Account Number"),
                ReportColumn.text("assured", "Assured"),
                ReportColumn.amount(AMOUNT, "Amount Reversed"),
                ReportColumn.text("reason", "Reason"),
                ReportColumn.text("created_by", "Created by"),
                ReportColumn.text("posted_by", "Posted by"),
                ReportColumn.date("posted_on", "Date Posted")),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Summary Per Disposition - Remittance (FRS.CSH.09.02.18).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport dispositionSummaryReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-DISPOSITION-SUMMARY",
            "Summary Per Disposition - Remittance",
            "Unapplied payments grouped by disposition type",
            DISPOSITIONS,
            List.of(
                ReportColumn.text("reference", "Reference Number"),
                ReportColumn.text("policy_no", "Policy Number"),
                ReportColumn.text("client_name", "Client Name"),
                ReportColumn.text("insurer_name", "Insurer Name"),
                ReportColumn.amount(AMOUNT, "Amount"),
                ReportColumn.text("reason", "Reason for Disposition"),
                ReportColumn.text("requested_by", "Requested By"),
                ReportColumn.text("approval_status", "Approval Status"),
                ReportColumn.date("action_on", "Date of Action"),
                ReportColumn.text("remarks", "Remarks"),
                ReportColumn.count("cnt", "Count")),
            "disposition_type",
            "Disposition Type",
            null),
        jdbc);
  }
}
