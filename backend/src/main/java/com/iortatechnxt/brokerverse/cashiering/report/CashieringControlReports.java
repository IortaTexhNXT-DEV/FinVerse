package com.iortatechnxt.brokerverse.cashiering.report;

import com.iortatechnxt.brokerverse.cashiering.report.SqlReport.Spec;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportOrigin;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Cashiering control reports (CSHID.023, Annex II #13-21 with draft layouts, OQ42), the Certificate
 * of Payment register (#19), and the reports named in the requirement rows: AR outstanding per
 * client and invoice with ageing (KC 4.c, OQ43), the batch run report (BRQID.006) and the BIR 2307
 * transaction report (CSHID.027).
 */
@Configuration(proxyBeanMethods = false)
public class CashieringControlReports {

  /** Business date of a timestamp within the report period. */
  private static final String BUSINESS_DAY_IN_PERIOD =
      "at time zone '" + BusinessClock.zoneId() + "' as date) between :from and :to";

  private static final String AMOUNT = "amount";
  private static final int SQL_CAPACITY = 512;
  private static final int[][] BRACKETS = {
    {0, 30}, {31, 60}, {61, 90}, {91, 120}, {121, 180}, {181, 365}
  };
  private static final String AMOUNT_LABEL = "Amount";
  private static final String INVOICE = "invoice_no";
  private static final String INVOICE_LABEL = "Invoice No.";
  private static final String REFERENCE_LABEL = "Reference";

  private static final String ADVANCE =
      "select cast(:to as date) - coalesce(r.receipt_date, cast(u.created_at as date)) as age_days,"
          + bracket("cast(:to as date) - coalesce(r.receipt_date, cast(u.created_at as date))")
          + " as bucket,"
          + " r.receipt_date as date_booked, coalesce(p.channel, r.payment_mode) as payment_type,"
          + " coalesce(p.value_date, r.receipt_date) as date_paid, cf.file_name, p.payment_no,"
          + " u.reference, r.receipt_no, u.payor_name, coalesce(p.reference, u.invoice_no)"
          + " as declared_ref, r.book_rate, u.amount, u.amount - u.balance as applied, u.balance,"
          + " case when u.balance = 0 then 'APPLIED' else u.origin end as payment_status,"
          + " u.invoice_no, a.market_segment, u.sales_unit, a.account_officer,"
          + " (select d.disposition_type from csh_disposition d where d.unapplied_id = u.id"
          + " order by d.created_at desc limit 1) as disposition, u.remarks,"
          + " coalesce(su.full_name, u.created_by) as processor,"
          + " coalesce(ins.name, i.insurer_code) as insurer"
          + " from csh_unapplied u left join csh_receipt r on r.id = u.receipt_id"
          + " left join csh_payment p on p.id = u.payment_id"
          + " left join csh_channel_file cf on cf.bulk_job_no = p.batch_ref"
          + " left join ops_invoice i on i.invoice_no = u.invoice_no"
          + " left join acc_account a on a.id = i.account_id"
          + " left join pty_party ins on ins.company_id = u.company_id and ins.code = i.insurer_code"
          + " left join sec_user su on su.username = u.created_by"
          + " where u.company_id = :company and cast(u.created_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) between :from and :to order by u.created_at, u.id";

  private static final String COP =
      "select c.receipt_no as ar_no, c.policy_no, c.requesting_unit, cast(c.issued_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date)"
          + " as issued_on, c.series_no, coalesce(u.full_name, c.issued_by) as issued_by, 1 as cnt"
          + " from csh_certificate_of_payment c left join sec_user u on u.username = c.issued_by"
          + " where c.company_id = :company and cast(c.issued_at "
          + BUSINESS_DAY_IN_PERIOD
          + " order by c.issued_at, c.id";

  private static final String AR_OUTSTANDING =
      "select coalesce(a.client_name, i.client_code) as client, i.client_code, i.arn,"
          + " i.invoice_no, i.policy_no, coalesce(ins.name, i.insurer_code) as insurer,"
          + " i.booking_date, cast(:to as date) - i.booking_date as age_days,"
          + bracket("cast(:to as date) - i.booking_date")
          + " as bucket,"
          + " a.market_segment as marketing_unit, a.account_officer,"
          + " sum(c.balance) as outstanding from ops_invoice i join ops_invoice_component c"
          + " on c.invoice_id = i.id and c.component in ('BASIC', 'DST', 'PREMIUM_TAX_VAT', 'LGT',"
          + " 'FST', 'OTHER') left join acc_account a on a.id = i.account_id"
          + " left join pty_party ins on ins.company_id = i.company_id and ins.code = i.insurer_code"
          + " where i.company_id = :company and not i.cancelled"
          + ReportOrigin.sql("i.origin")
          + " and i.booking_date <= :to group by a.client_name, i.client_code, i.arn, i.invoice_no,"
          + " i.policy_no, ins.name, i.insurer_code, i.booking_date, a.market_segment,"
          + " a.account_officer having sum(c.balance) > 0"
          + " order by client, i.booking_date, i.invoice_no";

  private static final String BATCH_RUN =
      "select p.batch_ref, p.channel, count(*) as payments,"
          + " sum(case when p.match_category = 'APPLIED' then 1 else 0 end) as applied,"
          + " sum(case when p.match_category = 'EXCESS' then 1 else 0 end) as excess,"
          + " sum(case when p.match_category = 'PREBOOKED' then 1 else 0 end) as prebooked,"
          + " sum(case when p.match_category = 'UNAPPLIED_NO_MATCH' then 1 else 0 end) as unapplied,"
          + " sum(case when p.match_category = 'CANCELLED_REFERENCE' then 1 else 0 end) as cancelled_ref,"
          + " (select count(*) from bulk_row br join bulk_job bj on bj.id = br.job_id"
          + " where bj.job_no = p.batch_ref and br.status in ('FAILED', 'INVALID')) as failed,"
          + " sum(p.amount) as amount, sum(p.applied_amount) as applied_amount,"
          + " sum(p.unapplied_amount) as unapplied_amount from csh_payment p"
          + " where p.company_id = :company and p.batch_ref is not null"
          + " and cast(p.created_at "
          + BUSINESS_DAY_IN_PERIOD
          + " group by p.batch_ref, p.channel order by p.batch_ref";

  private static final String CWT_TXN =
      "select b.batch_no, t.reference, t.invoice_no, t.client_code, t.insurer_code, t.certificate_no,"
          + " t.period_from, t.period_to, t.amount, t.stage, t.reclass_journal_no, t.offset_journal_no,"
          + " b.disbursement_request_no from csh_cwt_tag t join csh_cwt_batch b on b.id = t.batch_id"
          + " where t.company_id = :company and cast(b.created_at "
          + BUSINESS_DAY_IN_PERIOD
          + " order by b.batch_no, t.reference";

  /**
   * Unapplied Premium Payment Transaction (FRS.CSH.09.02.11; Annex II #14).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport advancePaymentReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        "CSH-ADVANCE-PAYMENT",
        "Unapplied Premium Payment Transaction",
        "Unapplied premium payments with their ageing, application and disposition",
        ADVANCE,
        List.of(
            new ReportColumn("age_days", "Aging (Per Date Booked)", ColumnType.NUMBER, false),
            ReportColumn.text("bucket", "Aging Bracket"),
            ReportColumn.date("date_booked", "Date Booked"),
            ReportColumn.text("payment_type", "Payment Type"),
            ReportColumn.date("date_paid", "Date Paid"),
            ReportColumn.text("file_name", "Filename"),
            ReportColumn.text("payment_no", "Transaction No"),
            ReportColumn.text("reference", "Receipt No."),
            ReportColumn.text("receipt_no", "AR #"),
            ReportColumn.text("payor_name", "Payor Name"),
            ReportColumn.text("declared_ref", "Declared Ref. No."),
            new ReportColumn("book_rate", "Dollar Rate Used", ColumnType.NUMBER, false),
            ReportColumn.amount(AMOUNT, "Amount Paid"),
            ReportColumn.amount("applied", "Amount Applied"),
            ReportColumn.amount("balance", "Amount Balance"),
            ReportColumn.text("payment_status", "Payment Status"),
            ReportColumn.text(INVOICE, "Invoice"),
            ReportColumn.text("sales_unit", "Department"),
            ReportColumn.text("market_segment", "Market Segment"),
            ReportColumn.text("account_officer", "Account Officer"),
            ReportColumn.text("disposition", "Marketing Disposition"),
            ReportColumn.text("remarks", "Remarks"),
            ReportColumn.text("processor", "Processor Name"),
            ReportColumn.text("insurer", "Insurer")));
  }

  /**
   * Annex II #19 Certification of Payment.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport certificateOfPaymentReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-CERT-OF-PAYMENT",
            "Certification of Payment",
            "Certificates of Payment issued",
            COP,
            List.of(
                ReportColumn.text("ar_no", "AR Number"),
                ReportColumn.text("policy_no", "Policy Number"),
                ReportColumn.text("requesting_unit", "Requesting Market Unit"),
                ReportColumn.date("issued_on", "Date (COP) Issued"),
                ReportColumn.text("series_no", "Series Number"),
                ReportColumn.text("issued_by", "Issued By"),
                ReportColumn.count("cnt", "Count")),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * AR outstanding per client per invoice with ageing (KC 4.c; buckets to confirm, OQ43).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport arOutstandingReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-AR-OUTSTANDING",
            "AR Outstanding with Ageing",
            "Premium receivable outstanding per client and account at the end date, aged from booking (KC 4.c)",
            AR_OUTSTANDING,
            List.of(
                ReportColumn.text("client", "Client"),
                ReportColumn.text("arn", "Account Number"),
                ReportColumn.text(INVOICE, INVOICE_LABEL),
                ReportColumn.text("policy_no", "Policy Number"),
                ReportColumn.text("insurer", "Insurer"),
                ReportColumn.date("booking_date", "Booking Date"),
                ReportColumn.amount("outstanding", "Outstanding Amount"),
                ReportColumn.text("bucket", "Ageing Bracket"),
                ReportColumn.text("marketing_unit", "Marketing Unit"),
                ReportColumn.text("account_officer", "Account Officer")),
            null,
            null,
            "Ageing brackets 0-30 / 31-60 / 61-90 / 91-120 / 121-180 / 181-365 / above 365 days."),
        jdbc);
  }

  /**
   * Batch run report (BRQID.006): payments of each upload by outcome.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport batchRunReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-BATCH-RUN",
            "Payment Batch Run Report",
            "Payment uploads with applied, excess, pre-booked, unapplied and failed records",
            BATCH_RUN,
            List.of(
                ReportColumn.text("batch_ref", "Upload Job"),
                ReportColumn.text("channel", "Channel"),
                ReportColumn.count("payments", "Payments"),
                ReportColumn.count("applied", "Applied"),
                ReportColumn.count("excess", "Excess"),
                ReportColumn.count("prebooked", "Pre-booked"),
                ReportColumn.count("unapplied", "Unapplied"),
                ReportColumn.count("cancelled_ref", "Cancelled Ref."),
                ReportColumn.count("failed", "Failed"),
                ReportColumn.amount(AMOUNT, AMOUNT_LABEL),
                ReportColumn.amount("applied_amount", "Amount Applied"),
                ReportColumn.amount("unapplied_amount", "Amount Unapplied")),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * BIR 2307 transaction report (CSHID.027).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport cwtTransactionReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-2307-TXN",
            "BIR 2307 Transaction Report",
            "Validated 2307 certificates per batch with the reclass and DTIP offset journals",
            CWT_TXN,
            List.of(
                ReportColumn.text("reference", REFERENCE_LABEL),
                ReportColumn.text(INVOICE, INVOICE_LABEL),
                ReportColumn.text("client_code", "Client"),
                ReportColumn.text("insurer_code", "Insurer"),
                ReportColumn.text("certificate_no", "Certificate"),
                ReportColumn.date("period_from", "Period From"),
                ReportColumn.date("period_to", "Period To"),
                ReportColumn.amount(AMOUNT, AMOUNT_LABEL),
                ReportColumn.text("stage", "Stage"),
                ReportColumn.text("reclass_journal_no", "Reclass Journal"),
                ReportColumn.text("offset_journal_no", "DTIP Offset Journal"),
                ReportColumn.text("disbursement_request_no", "Disbursement Request")),
            "batch_no",
            "Batch",
            null),
        jdbc);
  }

  private static SqlReport report(
      NamedParameterJdbcTemplate jdbc,
      String code,
      String title,
      String description,
      String sql,
      List<ReportColumn> columns,
      String... group) {
    return new SqlReport(
        new Spec(
            code,
            title,
            description,
            sql,
            columns,
            group.length > 0 ? group[0] : null,
            group.length > 1 ? group[1] : null,
            null),
        jdbc);
  }

  /**
   * The ageing bracket of a number of days (FRS.CSH.09.02.11): 0-30, 31-60, 61-90, 91-120, 121-180,
   * 181-365, above 365.
   *
   * @param days SQL expression of the days
   * @return SQL case expression
   */
  static String bracket(String days) {
    StringBuilder sql = new StringBuilder(SQL_CAPACITY).append(" case");
    for (int[] b : BRACKETS) {
      sql.append(" when ")
          .append(days)
          .append(" <= ")
          .append(b[1])
          .append(" then '")
          .append(b[0])
          .append('-')
          .append(b[1])
          .append('\'');
    }
    return sql.append(" else 'above 365' end").toString();
  }
}
