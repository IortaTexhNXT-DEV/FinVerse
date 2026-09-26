package com.iortatechnxt.brokerverse.cashiering.report;

import com.iortatechnxt.brokerverse.cashiering.report.SqlReport.Spec;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Cashiering reports of Annex II with the fields the BRD gives (CSHID.023): applied premium and
 * commission, PDC warehousing, minimal balance of excess payments, cancelled ORs and ARs, check
 * pick-up requests and priority posted accounts; plus the minimal balance and commission receivable
 * extracts whose layouts are drafts (OQ42).
 */
@Configuration(proxyBeanMethods = false)
public class CashieringReceiptReports {

  private static final String AR_NO = "ar_no";
  private static final String AR_NUMBER = "AR Number";
  private static final String AMOUNT = "amount";
  private static final String AMOUNT_LABEL = "Amount";
  private static final String COUNT = "cnt";
  private static final String COUNT_LABEL = "Count";
  private static final String INVOICE = "invoice_no";
  private static final String INVOICE_LABEL = "Invoice No.";
  private static final String PAYOR = "payor_name";
  private static final String INSURER = "insurer_code";
  private static final String INSURER_LABEL = "Insurance Company";

  private static final String APPLIED_PREMIUM =
      "select r.receipt_no as ar_no, a.value_date, a.amount, a.invoice_no, r.payor_name, i.risk_code,"
          + " 1 as cnt from csh_application a join csh_receipt r on r.id = a.receipt_id"
          + " left join ops_invoice i on i.invoice_no = a.invoice_no"
          + " where a.company_id = :company and a.status = 'ACTIVE' and r.kind = 'AR'"
          + " and a.value_date between :from and :to order by a.value_date, r.receipt_no, a.id";

  private static final String APPLIED_COMMISSION =
      "select r.receipt_no as or_no, l.invoice_no, count(*) over (partition by r.id) as invoices,"
          + " l.insurer_code, l.gross, l.wtax, l.vat, l.net from csh_receipt r"
          + " join csh_receipt_line l on l.receipt_id = r.id where r.company_id = :company"
          + " and r.kind = 'OR' and r.receipt_class = 'COMMISSION' and r.status <> 'CANCELLED'"
          + " and r.receipt_date between :from and :to order by r.receipt_no, l.line_no";

  private static final String PDC_WAREHOUSE =
      "select r.receipt_date as ar_date, p.receipt_no as ar_no, p.maturity_date, p.payor_name,"
          + " p.amount, p.bank_code, p.check_branch, p.check_no, p.segment, p.warehouse_no, p.status"
          + " from csh_pdc_item p left join csh_receipt r on r.receipt_no = p.receipt_no"
          + " where p.company_id = :company and p.maturity_date between :from and :to"
          + " order by p.maturity_date, p.warehouse_no";

  private static final String MINBAL_EXCESS =
      "select r.receipt_no as ar_no, r.receipt_date as ar_date, m.sales_unit as market_unit,"
          + " b.code as section_unit, m.amount, m.subject_ref, m.swept_on, 1 as cnt"
          + " from csh_minimal_balance m left join csh_unapplied u on u.reference = m.subject_ref"
          + " left join csh_receipt r on r.id = u.receipt_id left join org_branch b on b.id = u.branch_id"
          + " where m.company_id = :company and m.kind = 'EXCESS' and m.swept_on between :from and :to"
          + " order by m.swept_on, m.subject_ref";

  private static final String CANCELLED =
      "select r.receipt_date as date_issued, x.reason_code as reason, r.receipt_no,"
          + " coalesce(r.assured_name, r.payor_name) as assured,"
          + " case when r.kind = 'OR' then r.gross else r.amount end as gross, r.vat, r.wtax, r.amount,"
          + " cast(x.approved_at as date) as cancelled_on, x.transaction_no from csh_receipt r"
          + " join csh_receipt_action x on x.receipt_id = r.id and x.action = 'CANCEL'"
          + " and x.stage = 'POSTED' where r.company_id = :company and r.kind = ";

  private static final String CANCELLED_PERIOD =
      " and cast(x.approved_at as date) between :from and :to order by x.approved_at";

  private static final String PICKUP =
      "select cast(p.requested_at as date) as request_date,"
          + " to_char(p.requested_at at time zone 'Asia/Manila', 'HH24:MI') as request_time,"
          + " p.receipt_no as ar_no, cast(p.printed_at as date) as date_issued, p.amount,"
          + " p.payor_name, p.requestor, p.collection_ref, p.status, 1 as cnt from csh_pickup_request p"
          + " where p.company_id = :company and p.pickup_date between :from and :to"
          + " order by p.pickup_date, p.collection_ref";

  private static final String PRIORITY_POSTED =
      "select r.receipt_no as or_no, r.payor_name, a.amount, a.value_date as date_paid,"
          + " a.created_by as encoder, b.code as branch, a.status, a.invoice_no, i.policy_no,"
          + " i.risk_code, i.segment as corporate_dept from csh_application a"
          + " join csh_receipt r on r.id = a.receipt_id join org_branch b on b.id = r.branch_id"
          + " left join ops_invoice i on i.invoice_no = a.invoice_no where a.company_id = :company"
          + " and a.value_date between :from and :to order by a.value_date, r.receipt_no, a.id";

  private static final String COMMISSION_OUTSTANDING =
      "select i.insurer_code, i.invoice_no, i.arn, i.assured_name, i.booking_date,"
          + " c.booked as commission, c.balance as outstanding, cast(:to as date) - i.booking_date as age_days"
          + " from ops_invoice i join ops_invoice_component c on c.invoice_id = i.id"
          + " and c.component = 'COMMISSION' where i.company_id = :company and c.balance > 0"
          + " and i.booking_date between :from and :to order by i.insurer_code, i.booking_date, i.invoice_no";

  private static final String COMMISSION_YTD =
      "select i.insurer_code, i.segment, count(*) as invoices, sum(c.booked) as commission,"
          + " sum(c.balance) as outstanding from ops_invoice i join ops_invoice_component c"
          + " on c.invoice_id = i.id and c.component = 'COMMISSION' where i.company_id = :company"
          + " and c.balance > 0 and i.booking_date between cast(date_trunc('year', cast(:to as date)) as date)"
          + " and :to group by i.insurer_code, i.segment order by i.insurer_code, i.segment";

  private static final String MINBAL =
      "select m.swept_on, m.invoice_no, m.client_code, m.sales_unit, m.components, m.amount,"
          + " m.journal_batch_no, 1 as cnt from csh_minimal_balance m where m.company_id = :company"
          + " and m.swept_on between :from and :to and m.kind = ";

  private static final String MINBAL_ORDER = " order by m.swept_on, m.invoice_no";

  /**
   * Annex II #1 Applied Premium Reports.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport appliedPremiumReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-APPLIED-PREM",
            "Applied Premium Report",
            "Premium applied to invoices by AR in the period (CSHID.023 Annex II #1)",
            APPLIED_PREMIUM,
            List.of(
                ReportColumn.text(AR_NO, "AR No."),
                ReportColumn.date("value_date", "Date"),
                ReportColumn.amount(AMOUNT, "Amount Paid"),
                ReportColumn.text(INVOICE, INVOICE_LABEL),
                ReportColumn.text(PAYOR, "Payor / Client Name"),
                ReportColumn.text("risk_code", "Risk Code"),
                ReportColumn.count(COUNT, COUNT_LABEL)),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Annex II #2 Applied Commission Reports.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport appliedCommissionReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-APPLIED-COMM",
            "Applied Commission Report",
            "Commission ORs with their invoices, commission, WTAX and EVAT (CSHID.023 Annex II #2)",
            APPLIED_COMMISSION,
            List.of(
                ReportColumn.text("or_no", "OR Number"),
                ReportColumn.text(INVOICE, "Invoice Number"),
                new ReportColumn(
                    "invoices", "Number of Applied Invoices", ColumnType.NUMBER, false),
                ReportColumn.text(INSURER, INSURER_LABEL),
                ReportColumn.amount("gross", "Basic Commission"),
                ReportColumn.amount("wtax", "W/TAX"),
                ReportColumn.amount("vat", "EVAT"),
                ReportColumn.amount("net", "Net Commission")),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Annex II #3 Post-dated Checks Warehousing.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport pdcWarehouseReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-PDC-WAREHOUSE",
            "Post-dated Checks Warehousing",
            "Post-dated checks by maturity date (CSHID.008, CSHID.023 Annex II #3)",
            PDC_WAREHOUSE,
            List.of(
                ReportColumn.date("ar_date", "AR Date"),
                ReportColumn.text(AR_NO, AR_NUMBER),
                ReportColumn.date("maturity_date", "Maturity Date"),
                ReportColumn.text(PAYOR, "Client Name"),
                ReportColumn.amount(AMOUNT, AMOUNT_LABEL),
                ReportColumn.text("bank_code", "Bank Code"),
                ReportColumn.text("check_branch", "Branch"),
                ReportColumn.text("check_no", "Check Number"),
                ReportColumn.text("segment", "Market Segment"),
                ReportColumn.text("warehouse_no", "Warehouse No."),
                ReportColumn.text("status", "Status")),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Annex II #4 Minimal Balance of Unapplied Payments (Excess Payments).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport minimalExcessReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-MINBAL-EXCESS",
            "Minimal Balance of Unapplied Payments (Excess Payments)",
            "Excess payments moved to AP overages (Cashiering summary 5.f, CSHID.023 Annex II #4)",
            MINBAL_EXCESS,
            List.of(
                ReportColumn.text(AR_NO, AR_NUMBER),
                ReportColumn.date("ar_date", "AR Date"),
                ReportColumn.text("market_unit", "Market Unit"),
                ReportColumn.text("section_unit", "Section Unit"),
                ReportColumn.amount(AMOUNT, "Minimal Amount"),
                ReportColumn.text("subject_ref", "Unapplied Item"),
                ReportColumn.date("swept_on", "Swept On"),
                ReportColumn.count(COUNT, COUNT_LABEL)),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Annex II #5 Cancelled Official Receipts.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport cancelledOrReport(NamedParameterJdbcTemplate jdbc) {
    return cancelled(jdbc, "CSH-CANCELLED-OR", "Cancelled Official Receipts", "'OR'", "#5");
  }

  /**
   * Annex II #6 Cancelled Acknowledgment Receipts.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport cancelledArReport(NamedParameterJdbcTemplate jdbc) {
    return cancelled(jdbc, "CSH-CANCELLED-AR", "Cancelled Acknowledgment Receipts", "'AR'", "#6");
  }

  private static SqlReport cancelled(
      NamedParameterJdbcTemplate jdbc, String code, String title, String kind, String annex) {
    return new SqlReport(
        new Spec(
            code,
            title,
            "Receipts cancelled in the period with the reason (CSHID.012, CSHID.023 Annex II "
                + annex
                + ")",
            CANCELLED + kind + CANCELLED_PERIOD,
            List.of(
                ReportColumn.date("date_issued", "Date Issued"),
                ReportColumn.text("reason", "Reason for Cancellation"),
                ReportColumn.text("receipt_no", "Receipt Number"),
                ReportColumn.text("assured", "Assured"),
                ReportColumn.amount("gross", "Gross Amount"),
                ReportColumn.amount("vat", "VAT"),
                ReportColumn.amount("wtax", "WTAX"),
                ReportColumn.amount(AMOUNT, AMOUNT_LABEL),
                ReportColumn.date("cancelled_on", "Cancelled On"),
                ReportColumn.text("transaction_no", "Transaction No.")),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Annex II #7 Check Pick-Up Request Reports.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport checkPickupReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-CHECK-PICKUP",
            "Check Pick-Up Requests",
            "Checks tagged for pick-up by Collection and their ARs (CSHID.009, CSHID.023 Annex II #7)",
            PICKUP,
            List.of(
                ReportColumn.date("request_date", "Date of Request"),
                ReportColumn.text("request_time", "Time of Request"),
                ReportColumn.text(AR_NO, AR_NUMBER),
                ReportColumn.date("date_issued", "Date Issued"),
                ReportColumn.amount(AMOUNT, AMOUNT_LABEL),
                ReportColumn.text(PAYOR, "Assured's Name"),
                ReportColumn.text("requestor", "Collections Handler / Requestor"),
                ReportColumn.text("collection_ref", "Collection Ref."),
                ReportColumn.text("status", "Status"),
                ReportColumn.count(COUNT, COUNT_LABEL)),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Annex II #8 Priority Posted Accounts.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport priorityPostedReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-PRIORITY-POSTED",
            "Priority Posted Accounts",
            "Payments posted to invoices with encoder and branch (CSHID.023 Annex II #8)",
            PRIORITY_POSTED,
            List.of(
                ReportColumn.text("or_no", "Receipt Number"),
                ReportColumn.text(PAYOR, "Payor"),
                ReportColumn.amount(AMOUNT, "Amount Paid"),
                ReportColumn.date("date_paid", "Date Paid"),
                ReportColumn.text("encoder", "Encoder"),
                ReportColumn.text("branch", "Branch"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text(INVOICE, "Invoice Number"),
                ReportColumn.text("policy_no", "Policy Number"),
                ReportColumn.text("risk_code", "Risk Code"),
                ReportColumn.text("corporate_dept", "Corporate Depart")),
            null,
            null,
            "The meaning of 'priority' is to be confirmed by BDOI (OQ42); all postings are listed."),
        jdbc);
  }

  /**
   * Annex II #9 Unapplied Commission Receivable Extract for Mancom (draft layout).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport commissionMancomReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-UNAPPLIED-COMM-MANCOM",
            "Unapplied Commission Receivable Extract for Mancom",
            "Commission receivable not yet collected per insurer (CSHID.023 Annex II #9, draft)",
            COMMISSION_OUTSTANDING,
            List.of(
                ReportColumn.text(INVOICE, INVOICE_LABEL),
                ReportColumn.text("arn", "ARN"),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.date("booking_date", "Booking Date"),
                ReportColumn.amount("commission", "Commission"),
                ReportColumn.amount("outstanding", "Outstanding"),
                new ReportColumn("age_days", "Age (days)", ColumnType.NUMBER, false)),
            INSURER,
            INSURER_LABEL,
            SqlReport.DRAFT),
        jdbc);
  }

  /**
   * Annex II #10 Unapplied Commission Receivable Payments YTD balance per criteria (draft).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport commissionYtdReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-UNAPPLIED-COMM-YTD",
            "Unapplied Commission Receivable YTD Balance",
            "Year-to-date commission receivable balance per insurer and segment (CSHID.023 Annex II #10, draft)",
            COMMISSION_YTD,
            List.of(
                ReportColumn.text(INSURER, INSURER_LABEL),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.count("invoices", "Invoices"),
                ReportColumn.amount("commission", "Commission"),
                ReportColumn.amount("outstanding", "Outstanding")),
            null,
            null,
            SqlReport.DRAFT),
        jdbc);
  }

  /**
   * Annex II #11 Premium Minimal Balance (draft).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport premiumMinimalReport(NamedParameterJdbcTemplate jdbc) {
    return minimal(jdbc, "CSH-MINBAL-PREMIUM", "Premium Minimal Balance", "'PREMIUM'", "#11");
  }

  /**
   * Annex II #12 Commission Minimal Balance (draft; rule inactive until OQ11).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport commissionMinimalReport(NamedParameterJdbcTemplate jdbc) {
    return minimal(
        jdbc, "CSH-MINBAL-COMMISSION", "Commission Minimal Balance", "'COMMISSION'", "#12");
  }

  private static SqlReport minimal(
      NamedParameterJdbcTemplate jdbc, String code, String title, String kind, String annex) {
    return new SqlReport(
        new Spec(
            code,
            title,
            "Minimal balances reversed by the sweep (CSHID.016, CSHID.023 Annex II "
                + annex
                + ", draft)",
            MINBAL + kind + MINBAL_ORDER,
            List.of(
                ReportColumn.date("swept_on", "Swept On"),
                ReportColumn.text(INVOICE, INVOICE_LABEL),
                ReportColumn.text("client_code", "Client"),
                ReportColumn.text("sales_unit", "Market Unit"),
                ReportColumn.text("components", "Components"),
                ReportColumn.amount(AMOUNT, AMOUNT_LABEL),
                ReportColumn.text("journal_batch_no", "Journal"),
                ReportColumn.count(COUNT, COUNT_LABEL)),
            null,
            null,
            SqlReport.DRAFT),
        jdbc);
  }
}
