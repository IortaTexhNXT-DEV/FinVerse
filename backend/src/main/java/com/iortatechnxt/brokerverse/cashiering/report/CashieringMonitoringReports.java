package com.iortatechnxt.brokerverse.cashiering.report;

import com.iortatechnxt.brokerverse.cashiering.report.SqlReport.Spec;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Cashiering monitoring reports in the layouts of the client's list (FRS.CSH.09.02.10, 09.02.12 to
 * 09.02.14, 09.02.16, 09.02.17): daily cash reconciliation, direct payment, reinstatement
 * monitoring, re-application, reinstatement and CWT monitoring (premium).
 */
@Configuration(proxyBeanMethods = false)
public class CashieringMonitoringReports {

  private static final String AMOUNT = "amount";
  private static final String COUNT = "cnt";
  private static final String INVOICE = "invoice_no";
  private static final String INVOICE_LABEL = "Invoice Number";
  private static final String DAY = " at time zone '" + BusinessClock.zoneId() + "' as date)";
  private static final String REASON =
      "coalesce((select v.label from lov_value v where v.code = %s and v.type_code in"
          + " ('RECEIPT_CANCEL_REASON', 'REINSTATEMENT_REASON') order by v.effective_from desc"
          + " limit 1), %s)";

  private static final String DAILY_CASH =
      "select r.receipt_date, coalesce(u.full_name, r.created_by) as encoder, r.payor_name,"
          + " r.receipt_no, coalesce(ba.name, r.bank_account) as deposit_bank, r.amount, 1 as cnt"
          + " from csh_receipt r left join sec_user u on u.username = r.created_by"
          + " left join csh_bank_account ba on ba.company_id = r.company_id"
          + " and ba.code = r.bank_account where r.company_id = :company and r.kind = 'OR'"
          + " and r.status <> 'CANCELLED' and r.receipt_date between :from and :to"
          + " order by r.receipt_date, r.receipt_no";

  private static final String DIRECT_PAYMENT =
      "select i.invoice_no, i.assured_name, i.policy_no, (select sum(c.balance)"
          + " from ops_invoice_component c where c.invoice_id = i.id and c.component in"
          + " ('BASIC', 'DST', 'PREMIUM_TAX_VAT', 'LGT', 'FST', 'OTHER')) as ar_balance,"
          + " x.payment_mode, x.receipt_no as or_no, x.receipt_date as or_date, x.amount as or_amount,"
          + " x.reinstated_amount as reversal_amount, x.amount - x.applied_amount as or_balance,"
          + " x.source_module as requestor, coalesce(u.full_name, x.created_by) as processor,"
          + " x.remarks from ops_invoice i left join lateral (select r.* from csh_receipt r"
          + " join csh_receipt_line l on l.receipt_id = r.id where l.invoice_no = i.invoice_no"
          + " and r.kind = 'OR' order by r.id desc limit 1) x on true"
          + " left join sec_user u on u.username = x.created_by"
          + " where i.company_id = :company and i.dp_flag and i.booking_date between :from and :to"
          + " order by i.booking_date, i.invoice_no";

  private static final String REINSTATEMENT_MONITORING =
      "select x.requested_on, x.invoice_no, x.receipt_no, x.amount, x.assured, x.processed_on,"
          + " coalesce(c.full_name, x.requestor) as requestor,"
          + " coalesce(p.full_name, x.processor) as processor, x.reason, x.status from ("
          + "select cast(rr.created_at"
          + DAY
          + " as requested_on, coalesce(a.reference, rr.invoice_no) as invoice_no, rr.receipt_no,"
          + " coalesce(a.amount, rr.amount) as amount,"
          + " coalesce(rr.client_name, rr.insurer_name, rr.payor_name) as assured,"
          + " cast(rr.posted_at"
          + DAY
          + " as processed_on, rr.created_by as requestor, rr.posted_by as processor, "
          + String.format(REASON, "rr.reason_code", "rr.reason_code")
          + " as reason, case rr.stage when 'POSTED' then 'Approved' when 'RETURNED' then"
          + " 'Returned' when 'RECORD_CANCELLED' then 'Cancelled' else 'Pending approval' end"
          + " as status, rr.created_at as at from csh_receipt_record rr"
          + " left join csh_receipt_record_account a on a.record_id = rr.id"
          + " where rr.company_id = :company and rr.record_kind = 'REINSTATEMENT'"
          + " union all select cast(x.created_at"
          + DAY
          + ", x.invoice_no, r.receipt_no, x.amount, coalesce(r.assured_name, r.payor_name),"
          + " cast(x.approved_at"
          + DAY
          + ", x.created_by, x.approved_by, "
          + String.format(REASON, "x.reason_code", "x.reason_code")
          + ", case x.stage when 'POSTED' then 'Approved' when 'RETURNED' then 'Returned'"
          + " else 'Pending approval' end, x.created_at from csh_receipt_action x"
          + " join csh_receipt r on r.id = x.receipt_id where x.company_id = :company"
          + " and x.action <> 'CANCEL' and not exists (select 1 from csh_receipt_record rr"
          + " where rr.record_no = x.transaction_no)) x"
          + " left join sec_user c on c.username = x.requestor"
          + " left join sec_user p on p.username = x.processor"
          + " where x.requested_on between :from and :to order by x.at";

  private static final String REAPPLICATION =
      "select cast(rr.posted_at"
          + DAY
          + " as gl_posted_on, cast(d.completed_at"
          + DAY
          + " as applied_on, d.target_invoice_no as invoice_no,"
          + " coalesce(i.assured_name, u.payor_name) as assured, d.amount,"
          + " coalesce(rr.record_no, u.source_ref) as reinstatement_no,"
          + " coalesce(su.full_name, d.created_by) as requested_by, "
          + String.format(REASON, "rr.reason_code", "coalesce(rr.reason_code, d.remarks)")
          + " as reason, cast(d.completed_at"
          + DAY
          + " - cast(d.created_at"
          + DAY
          + " as tat_reapplication, cast(rr.posted_at"
          + DAY
          + " - cast(rr.created_at"
          + DAY
          + " as tat_reinstatement from csh_disposition d"
          + " join csh_unapplied u on u.id = d.unapplied_id"
          + " left join csh_receipt_record rr on rr.receipt_id = u.receipt_id"
          + " and rr.record_kind = 'REINSTATEMENT' and rr.stage = 'POSTED'"
          + " left join ops_invoice i on i.invoice_no = d.target_invoice_no"
          + " left join sec_user su on su.username = d.created_by"
          + " where u.company_id = :company and d.action = 'APPLY' and d.status = 'COMPLETED'"
          + " and u.origin in ('REINSTATEMENT', 'REAPPLY', 'DP_REINSTATE', 'CANCELLATION')"
          + " and cast(d.completed_at"
          + DAY
          + " between :from and :to order by d.completed_at, d.id";

  private static final String REINSTATEMENT_SUMMARY =
      "select coalesce(a.reference, rr.invoice_no) as invoice_no,"
          + " max(coalesce(rr.client_name, rr.payor_name)) as assured, rr.receipt_no,"
          + " to_char(min(rr.posted_at), 'Mon-YYYY') as months, count(*) as requests,"
          + " max(cast(rr.posted_at"
          + DAY
          + ") as processed_on, max(cast(rr.posted_at"
          + DAY
          + " - cast(rr.created_at"
          + DAY
          + ") as tat_processed, (select max(cast(d.completed_at"
          + DAY
          + ") from csh_disposition d join csh_unapplied u on u.id = d.unapplied_id"
          + " where u.receipt_id = rr.receipt_id and d.action = 'APPLY' and d.status = 'COMPLETED')"
          + " as reapplied_on, sum(case when upper(coalesce(rr.reason_code, '')) like '%MISAPPL%'"
          + " then 1 else 0 end) as misapplications from csh_receipt_record rr"
          + " left join csh_receipt_record_account a on a.record_id = rr.id"
          + " where rr.company_id = :company and rr.record_kind = 'REINSTATEMENT'"
          + " and rr.stage = 'POSTED' and cast(rr.posted_at"
          + DAY
          + " between :from and :to group by coalesce(a.reference, rr.invoice_no), rr.receipt_no,"
          + " rr.receipt_id order by invoice_no, rr.receipt_no";

  private static final String CWT =
      "select row_number() over (order by t.created_at, t.id) as item_no, t.invoice_no,"
          + " i.assured_name, a.period_from as inception, a.period_to as expiry, i.policy_no,"
          + " coalesce(ins.name, i.insurer_code) as insurer, (select c.booked"
          + " from ops_invoice_component c where c.invoice_id = i.id and c.component = 'BASIC')"
          + " as basic_premium, t.amount as withheld,"
          + " coalesce(pu.full_name, t.created_by) as prepared_by,"
          + " coalesce(ru.full_name, t.updated_by) as reviewed_by,"
          + " case when t.remitted then coalesce(ins.name, i.insurer_code) end as received_by"
          + " from csh_cwt_tag t left join ops_invoice i on i.invoice_no = t.invoice_no"
          + " left join acc_account a on a.id = i.account_id"
          + " left join pty_party ins on ins.company_id = t.company_id and ins.code = i.insurer_code"
          + " left join sec_user pu on pu.username = t.created_by"
          + " left join sec_user ru on ru.username = t.updated_by"
          + " where t.company_id = :company and cast(t.created_at"
          + DAY
          + " between :from and :to order by t.created_at, t.id";

  private static SqlReport report(
      NamedParameterJdbcTemplate jdbc,
      String[] names,
      String sql,
      List<ReportColumn> columns,
      String note) {
    return new SqlReport(
        new Spec(names[0], names[1], names[2], sql, columns, null, null, note), jdbc);
  }

  /**
   * Daily Cash Reconciliation Reports (FRS.CSH.09.02.10).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport dailyCashReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        new String[] {
          "CSH-DAILY-CASH-REC",
          "Daily Cash Reconciliation Reports",
          "ORs of each day with the encoder, payor and deposit bank"
        },
        DAILY_CASH,
        List.of(
            ReportColumn.date("receipt_date", "Date"),
            ReportColumn.text("encoder", "Encoder"),
            ReportColumn.text("payor_name", "Client/Payor Name"),
            ReportColumn.text("receipt_no", "OR Number"),
            ReportColumn.text("deposit_bank", "Deposit In Bank"),
            ReportColumn.amount(AMOUNT, "Amount"),
            ReportColumn.count(COUNT, "Count")),
        null);
  }

  /**
   * Direct Payment (FRS.CSH.09.02.12).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport directPaymentReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        new String[] {
          "CSH-DIRECT-PAYMENT", "Direct Payment", "Direct payment accounts with their commission OR"
        },
        DIRECT_PAYMENT,
        List.of(
            ReportColumn.text(INVOICE, INVOICE_LABEL),
            ReportColumn.text("assured_name", "Assured's Name"),
            ReportColumn.text("policy_no", "Policy Number"),
            ReportColumn.amount("ar_balance", "AR Balance"),
            ReportColumn.text("payment_mode", "Payment Type"),
            ReportColumn.text("or_no", "OR Number"),
            ReportColumn.date("or_date", "OR Date"),
            ReportColumn.amount("or_amount", "OR Amount"),
            ReportColumn.amount("reversal_amount", "Reversal Amount"),
            ReportColumn.amount("or_balance", "OR Balance"),
            ReportColumn.text("requestor", "Requestor"),
            ReportColumn.text("processor", "Processor"),
            ReportColumn.text("remarks", "Remarks")),
        null);
  }

  /**
   * Reinstatement Monitoring Report (FRS.CSH.09.02.13).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport reinstatementMonitoringReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        new String[] {
          "CSH-REINSTATEMENT-MON",
          "Reinstatement Monitoring Report",
          "Reinstatements requested in the period with their status, reason and approvers"
        },
        REINSTATEMENT_MONITORING,
        List.of(
            ReportColumn.date("requested_on", "Date Requested"),
            ReportColumn.text(INVOICE, INVOICE_LABEL),
            ReportColumn.text("receipt_no", "AR Number"),
            ReportColumn.amount(AMOUNT, "Amount Reinstated"),
            ReportColumn.text("assured", "Assured Name"),
            ReportColumn.date("processed_on", "Date Processed"),
            ReportColumn.text("requestor", "Requestor"),
            ReportColumn.text("processor", "Processor"),
            ReportColumn.text("reason", "Reason for Reinstatement"),
            ReportColumn.text("status", "Status")),
        null);
  }

  /**
   * Re-Application Report (FRS.CSH.09.02.14).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport reapplicationReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        new String[] {
          "CSH-REAPPLICATION",
          "Re-Application Report",
          "Payments re-applied after a reinstatement or a correction"
        },
        REAPPLICATION,
        List.of(
            ReportColumn.date("gl_posted_on", "Date Posted by GL"),
            ReportColumn.date("applied_on", "Date Applied"),
            ReportColumn.text(INVOICE, INVOICE_LABEL),
            ReportColumn.text("assured", "Assured Name"),
            ReportColumn.amount(AMOUNT, "Amount for Reapplication"),
            ReportColumn.text("reinstatement_no", "Reinstatement Transaction No."),
            ReportColumn.text("requested_by", "Requested by"),
            ReportColumn.text("reason", "Reason for Reinstatement"),
            new ReportColumn(
                "tat_reapplication", "TAT Reapplication (days)", ColumnType.NUMBER, false),
            new ReportColumn(
                "tat_reinstatement", "TAT Reinstatement process (days)", ColumnType.NUMBER, false)),
        null);
  }

  /**
   * Reinstatement (FRS.CSH.09.02.16).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport reinstatementReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        new String[] {
          "CSH-REINSTATEMENT",
          "Reinstatement",
          "Reinstatements posted per account and receipt with their turnaround"
        },
        REINSTATEMENT_SUMMARY,
        List.of(
            ReportColumn.text(INVOICE, INVOICE_LABEL),
            ReportColumn.text("assured", "Assured"),
            ReportColumn.text("receipt_no", "AR Number"),
            ReportColumn.text("months", "Months"),
            ReportColumn.count("requests", "No. of Request"),
            ReportColumn.date("processed_on", "Date Processed"),
            new ReportColumn("tat_processed", "Processing TAT (days)", ColumnType.NUMBER, false),
            ReportColumn.date("reapplied_on", "Date Re-applied"),
            ReportColumn.count("misapplications", "Instance of Mis-application")),
        null);
  }

  /**
   * CWT Monitoring Report (Premium) (FRS.CSH.09.02.17).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport cwtReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        new String[] {
          "CSH-CWT",
          "CWT Monitoring Report (Premium)",
          "Withholding tax on premium payments per invoice with its BIR 2307 tag"
        },
        CWT,
        List.of(
            new ReportColumn("item_no", "Item No.", ColumnType.NUMBER, false),
            ReportColumn.text(INVOICE, "Invoice No."),
            ReportColumn.text("assured_name", "Assured Name"),
            ReportColumn.date("inception", "Inception Date"),
            ReportColumn.date("expiry", "Expiry Date"),
            ReportColumn.text("policy_no", "Policy No."),
            ReportColumn.text("insurer", "Insurance Company"),
            ReportColumn.amount("basic_premium", "Basic Premium"),
            ReportColumn.amount("withheld", "W/held"),
            ReportColumn.text("prepared_by", "Prepared by"),
            ReportColumn.text("reviewed_by", "Reviewed by"),
            ReportColumn.text("received_by", "Received by")),
        null);
  }
}
