package com.iortatechnxt.brokerverse.cashiering.report;

import com.iortatechnxt.brokerverse.cashiering.report.SqlReport.Spec;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
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

  private static final String AMOUNT = "amount";
  private static final String AMOUNT_LABEL = "Amount";
  private static final String RECEIPT = "receipt_no";
  private static final String RECEIPT_LABEL = "Receipt No.";
  private static final String INVOICE = "invoice_no";
  private static final String INVOICE_LABEL = "Invoice No.";
  private static final String STATUS = "status";
  private static final String REFERENCE_LABEL = "Reference";
  private static final String TRANSACTION = "transaction_no";
  private static final String TRANSACTION_LABEL = "Transaction No.";

  private static final String DAILY_CASH =
      "select r.receipt_date, r.kind, r.payment_mode, r.currency, count(*) as receipts,"
          + " sum(case when r.status <> 'CANCELLED' then r.amount else 0 end) as collected,"
          + " sum(case when r.status = 'CANCELLED' then r.amount else 0 end) as cancelled,"
          + " sum(r.applied_amount) as applied from csh_receipt r where r.company_id = :company"
          + " and r.receipt_date between :from and :to group by r.receipt_date, r.kind,"
          + " r.payment_mode, r.currency order by r.receipt_date, r.kind, r.payment_mode, r.currency";

  private static final String ADVANCE =
      "select p.first_seen as payment_date, r.receipt_no as ar_no, p.arn, p.reference, p.amount,"
          + " p.status, p.rematch_count, cast(p.resolved_at as date) as resolved_on, p.remarks"
          + " from csh_prebooked p join csh_receipt r on r.id = p.receipt_id"
          + " where p.company_id = :company and p.first_seen between :from and :to"
          + " order by p.first_seen, p.id";

  private static final String REVERSALS =
      "select cast(a.reversed_at as date) as reversed_on, r.receipt_no, a.invoice_no, a.amount,"
          + " a.source, a.reversal_ref, a.reversal_reason from csh_application a"
          + " left join csh_receipt r on r.id = a.receipt_id where a.company_id = :company"
          + " and a.status = 'REVERSED' and cast(a.reversed_at as date) between :from and :to"
          + " order by a.reversed_at, a.id";

  private static final String DIRECT_PAYMENT =
      "select i.booking_date, i.invoice_no, i.arn, i.assured_name, i.insurer_code,"
          + " i.gross_premium, i.commission, i.vat_on_commission, i.remittance_status"
          + " from ops_invoice i where i.company_id = :company and i.dp_flag"
          + " and i.booking_date between :from and :to order by i.booking_date, i.invoice_no";

  private static final String REINSTATEMENTS =
      "select cast(x.created_at as date) as requested_on, cast(x.approved_at as date) as posted_on,"
          + " x.transaction_no, r.receipt_no, x.action, x.reason_code, x.amount, x.invoice_no,"
          + " x.document_no, x.payor_name, x.account_officer, x.stage, x.created_by as requested_by,"
          + " x.approved_by, x.journal_batch_no from csh_receipt_action x"
          + " join csh_receipt r on r.id = x.receipt_id where x.company_id = :company"
          + " and x.action <> 'CANCEL' and cast(x.created_at as date) between :from and :to";

  private static final String REAPPLICATIONS =
      "select cast(ra.created_at as date) as reapplied_on, ra.invoice_no, ra.source_module,"
          + " ra.source_ref, ra.unapplied, ra.excess, ra.receipt_nos, ra.unapplied_ref, ra.reason"
          + " from csh_reapplication ra where ra.company_id = :company"
          + " and cast(ra.created_at as date) between :from and :to order by ra.created_at, ra.id";

  private static final String COP =
      "select c.receipt_no as ar_no, c.policy_no, c.requesting_unit, cast(c.issued_at as date)"
          + " as issued_on, c.issued_by, 1 as cnt from csh_certificate_of_payment c"
          + " where c.company_id = :company and cast(c.issued_at as date) between :from and :to"
          + " order by c.issued_at, c.id";

  private static final String CWT_TAGS =
      "select cast(t.created_at as date) as tagged_on, t.reference, t.invoice_no, t.client_code,"
          + " t.insurer_code, t.path, t.certificate_no, t.amount, t.stage, t.receipt_no"
          + " from csh_cwt_tag t where t.company_id = :company"
          + " and cast(t.created_at as date) between :from and :to order by t.created_at, t.id";

  private static final String AR_OUTSTANDING =
      "select i.client_code, i.assured_name, i.invoice_no, i.booking_date,"
          + " cast(:to as date) - i.booking_date as age_days,"
          + " case when cast(:to as date) - i.booking_date <= 30 then '0-30'"
          + " when cast(:to as date) - i.booking_date <= 60 then '31-60'"
          + " when cast(:to as date) - i.booking_date <= 90 then '61-90'"
          + " when cast(:to as date) - i.booking_date <= 120 then '91-120' else 'Over 120' end as bucket,"
          + " sum(c.balance) as outstanding from ops_invoice i join ops_invoice_component c"
          + " on c.invoice_id = i.id and c.component in ('BASIC', 'DST', 'PREMIUM_TAX_VAT', 'LGT',"
          + " 'FST', 'OTHER') where i.company_id = :company and not i.cancelled"
          + " and i.booking_date <= :to group by i.client_code, i.assured_name, i.invoice_no,"
          + " i.booking_date having sum(c.balance) > 0 order by i.client_code, i.booking_date, i.invoice_no";

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
          + " and cast(p.created_at as date) between :from and :to"
          + " group by p.batch_ref, p.channel order by p.batch_ref";

  private static final String CWT_TXN =
      "select b.batch_no, t.reference, t.invoice_no, t.client_code, t.insurer_code, t.certificate_no,"
          + " t.period_from, t.period_to, t.amount, t.stage, t.reclass_journal_no, t.offset_journal_no,"
          + " b.disbursement_request_no from csh_cwt_tag t join csh_cwt_batch b on b.id = t.batch_id"
          + " where t.company_id = :company and cast(b.created_at as date) between :from and :to"
          + " order by b.batch_no, t.reference";

  /**
   * Annex II #13 Daily Cash Reconciliation (draft).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport dailyCashReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        "CSH-DAILY-CASH-REC",
        "Daily Cash Reconciliation",
        "Receipts per day, kind and mode with collections, cancellations and applications (Annex II #13, draft)",
        DAILY_CASH,
        List.of(
            ReportColumn.text("kind", "Kind"),
            ReportColumn.text("payment_mode", "Mode"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.count("receipts", "Receipts"),
            ReportColumn.amount("collected", "Collected"),
            ReportColumn.amount("cancelled", "Cancelled"),
            ReportColumn.amount("applied", "Applied")),
        "receipt_date",
        "Receipt Date");
  }

  /**
   * Annex II #14 Advance Payment Transaction (Auto-Credit) (draft).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport advancePaymentReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        "CSH-ADVANCE-PAYMENT",
        "Advance Payment Transactions (Auto-Credit)",
        "Payments received before booking and their automatic application (CSHID.020, Annex II #14, draft)",
        ADVANCE,
        List.of(
            ReportColumn.date("payment_date", "Payment Date"),
            ReportColumn.text("ar_no", "AR No."),
            ReportColumn.text("arn", "ARN"),
            ReportColumn.text("reference", REFERENCE_LABEL),
            ReportColumn.amount(AMOUNT, AMOUNT_LABEL),
            ReportColumn.text(STATUS, "Status"),
            ReportColumn.count("rematch_count", "Re-match Runs"),
            ReportColumn.date("resolved_on", "Applied On"),
            ReportColumn.text("remarks", "Remarks")));
  }

  /**
   * Annex II #15 Payment Reversal (draft).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport paymentReversalReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        "CSH-PAYMENT-REVERSAL",
        "Payment Reversals",
        "Applications reversed by cancellation, re-application or disposition (Annex II #15, draft)",
        REVERSALS,
        List.of(
            ReportColumn.date("reversed_on", "Reversed On"),
            ReportColumn.text(RECEIPT, RECEIPT_LABEL),
            ReportColumn.text(INVOICE, INVOICE_LABEL),
            ReportColumn.amount(AMOUNT, AMOUNT_LABEL),
            ReportColumn.text("source", "Applied By"),
            ReportColumn.text("reversal_ref", "Reversal Ref."),
            ReportColumn.text("reversal_reason", "Reason")));
  }

  /**
   * Annex II #16 Direct Payment (draft).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport directPaymentReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        "CSH-DIRECT-PAYMENT",
        "Direct Payment Accounts",
        "Invoices paid directly to the insurer with the commission to collect (Annex II #16, draft)",
        DIRECT_PAYMENT,
        List.of(
            ReportColumn.date("booking_date", "Booking Date"),
            ReportColumn.text(INVOICE, INVOICE_LABEL),
            ReportColumn.text("arn", "ARN"),
            ReportColumn.text("assured_name", "Assured"),
            ReportColumn.text("insurer_code", "Insurer"),
            ReportColumn.amount("gross_premium", "Gross Premium"),
            ReportColumn.amount("commission", "Commission"),
            ReportColumn.amount("vat_on_commission", "VAT on Commission"),
            ReportColumn.text("remittance_status", "Remittance Status")));
  }

  /**
   * Annex II #17 Reinstatement Monitoring (draft) and #20 Reinstatement (draft).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport reinstatementMonitoringReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        "CSH-REINSTATEMENT-MON",
        "Reinstatement Monitoring",
        "Reinstatement requests in every stage (CSHID.004/005, Annex II #17, draft)",
        REINSTATEMENTS + " order by x.created_at, x.id",
        reinstatementColumns(),
        "stage",
        "Stage");
  }

  /**
   * Annex II #20 Reinstatement (draft).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport reinstatementReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        "CSH-REINSTATEMENT",
        "Reinstatements",
        "Reinstatements posted with the encoded fields (CSHID.013, Annex II #20, draft)",
        REINSTATEMENTS + " and x.stage = 'POSTED' order by x.approved_at, x.id",
        reinstatementColumns());
  }

  private static List<ReportColumn> reinstatementColumns() {
    return List.of(
        ReportColumn.date("requested_on", "Requested On"),
        ReportColumn.date("posted_on", "Posted On"),
        ReportColumn.text(TRANSACTION, TRANSACTION_LABEL),
        ReportColumn.text(RECEIPT, RECEIPT_LABEL),
        ReportColumn.text("action", "Type"),
        ReportColumn.text("reason_code", "Reason"),
        ReportColumn.amount(AMOUNT, "Amount Reinstated"),
        ReportColumn.text(INVOICE, INVOICE_LABEL),
        ReportColumn.text("document_no", "AR / OR No."),
        ReportColumn.text("payor_name", "Assured / Payor"),
        ReportColumn.text("account_officer", "Account Officer"),
        ReportColumn.text("requested_by", "Requested By"),
        ReportColumn.text("approved_by", "Approved By"));
  }

  /**
   * Annex II #18 Re-Application (draft).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport reapplicationReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        "CSH-REAPPLICATION",
        "Re-Application",
        "Payments re-applied after endorsements (ADJID.009/012/013, Annex II #18, draft)",
        REAPPLICATIONS,
        List.of(
            ReportColumn.date("reapplied_on", "Re-applied On"),
            ReportColumn.text(INVOICE, INVOICE_LABEL),
            ReportColumn.text("source_module", "Module"),
            ReportColumn.text("source_ref", REFERENCE_LABEL),
            ReportColumn.amount("unapplied", "Payments Re-applied"),
            ReportColumn.amount("excess", "Excess"),
            ReportColumn.text("receipt_nos", "Receipts"),
            ReportColumn.text("unapplied_ref", "Unapplied Item"),
            ReportColumn.text("reason", "Reason")));
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
            "Certificates of Payment issued (CSHID.023 Annex II #19)",
            COP,
            List.of(
                ReportColumn.text("ar_no", "AR Number"),
                ReportColumn.text("policy_no", "Policy Number"),
                ReportColumn.text("requesting_unit", "Requesting Market Unit"),
                ReportColumn.date("issued_on", "Date (COP) Issued"),
                ReportColumn.text("issued_by", "Issued By"),
                ReportColumn.count("cnt", "Count")),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Annex II #21 CWT (draft).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport cwtReport(NamedParameterJdbcTemplate jdbc) {
    return report(
        jdbc,
        "CSH-CWT",
        "CWT",
        "BIR 2307 tags and their status (CSHID.026, Annex II #21, draft)",
        CWT_TAGS,
        List.of(
            ReportColumn.date("tagged_on", "Tagged On"),
            ReportColumn.text("reference", REFERENCE_LABEL),
            ReportColumn.text(INVOICE, INVOICE_LABEL),
            ReportColumn.text("client_code", "Client"),
            ReportColumn.text("insurer_code", "Insurer"),
            ReportColumn.text("path", "Path"),
            ReportColumn.text("certificate_no", "Certificate"),
            ReportColumn.amount(AMOUNT, AMOUNT_LABEL),
            ReportColumn.text("stage", "Stage"),
            ReportColumn.text(RECEIPT, "AR (cash path)")));
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
            "Premium receivable outstanding per client and invoice at the end date, aged from booking (KC 4.c)",
            AR_OUTSTANDING,
            List.of(
                ReportColumn.text(INVOICE, INVOICE_LABEL),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.date("booking_date", "Booking Date"),
                new ReportColumn("age_days", "Age (days)", ColumnType.NUMBER, false),
                ReportColumn.text("bucket", "Ageing Bucket"),
                ReportColumn.amount("outstanding", "Outstanding")),
            "client_code",
            "Client",
            "Ageing buckets 0-30 / 31-60 / 61-90 / 91-120 / over 120 days to be confirmed (OQ43)."),
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
            "Payment uploads with applied, excess, pre-booked, unapplied and failed records (BRQID.006)",
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
            "Validated 2307 certificates per batch with the reclass and DTIP offset journals (CSHID.027)",
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
            SqlReport.DRAFT),
        jdbc);
  }
}
