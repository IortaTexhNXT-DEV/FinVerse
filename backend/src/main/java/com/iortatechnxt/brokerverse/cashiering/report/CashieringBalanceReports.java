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
 * Cashiering reports in the layouts of the client's list (FRS.CSH.09.02.04 to 09.02.09): the
 * cancelled ORs and ARs with the reason, the unapplied commission receivable extract and its
 * year-to-date balance per criteria (ageing bracket), and the premium and commission minimal
 * balances with the receipt, market unit and section unit.
 */
@Configuration(proxyBeanMethods = false)
public class CashieringBalanceReports {

  private static final String AMOUNT = "amount";
  private static final String COUNT = "cnt";
  private static final String COUNT_LABEL = "Count";
  private static final String KIND = ":kind";
  private static final String RECEIPT_NO = "receipt_no";
  private static final String ZONE = BusinessClock.zoneId();

  private static final String CANCELLED =
      "select r.receipt_date as date_issued, r.receipt_no, coalesce(r.assured_name, r.payor_name)"
          + " as assured, case when r.kind = 'OR' then r.gross else r.amount end as gross,"
          + " r.vat, r.wtax, r.amount,"
          + " coalesce((select v.label from lov_value v where v.code = x.reason_code"
          + " and v.type_code = 'RECEIPT_CANCEL_REASON' order by v.effective_from desc limit 1),"
          + " x.reason_code) || coalesce(': ' || x.reason_text, '') as reason, 1 as cnt"
          + " from csh_receipt r join csh_receipt_action x on x.receipt_id = r.id"
          + " and x.action = 'CANCEL' and x.stage = 'POSTED'"
          + " where r.company_id = :company and r.kind = :kind"
          + " and cast(coalesce(x.approved_at, x.updated_at, x.created_at) at time zone '"
          + ZONE
          + "' as date) between :from and :to order by coalesce(x.approved_at, x.created_at), r.id";

  private static final String COMMISSION_UNAPPLIED =
      " from csh_unapplied u left join csh_receipt r on r.id = u.receipt_id"
          + " left join ops_invoice i on i.invoice_no = u.invoice_no"
          + " left join ops_invoice_component c on c.invoice_id = i.id and c.component = 'COMMISSION'"
          + " left join acc_account a on a.id = i.account_id"
          + " left join pty_party ins on ins.company_id = u.company_id and ins.code = i.insurer_code"
          + " where u.company_id = :company and u.balance > 0"
          + " and u.origin in ('AP_UNAPPLIED_COMMISSION', 'COMMISSION_RECEIVABLE')"
          + " and cast(u.created_at at time zone '"
          + ZONE
          + "' as date) <= :to";

  private static final String MANCOM =
      "select coalesce(ins.name, i.insurer_code) as insurer, coalesce(i.assured_name, u.payor_name)"
          + " as assured, r.receipt_no as or_no, u.invoice_no,"
          + " (select count(*) from csh_receipt_line l where l.receipt_id = r.id) as invoices,"
          + " r.payment_mode as payment_type, c.booked as booked_commission,"
          + " c.booked + c.adjusted as commission_setup, r.amount as check_amount,"
          + " u.amount - u.balance as applied, u.balance as or_balance, r.receipt_date as or_date,"
          + " i.policy_no, (select rr.unit_head from csh_receipt_record rr where rr.receipt_id = r.id"
          + " order by rr.id limit 1) as unit_head, a.market_segment as department,"
          + " a.client_name as corporate_name, coalesce(su.full_name, u.created_by) as encoder,"
          + " cast(:to as date) - coalesce(r.receipt_date, cast(u.created_at as date)) as age"
          + COMMISSION_UNAPPLIED.replace(
              " where u.company_id",
              " left join sec_user su on su.username = u.created_by where u.company_id")
          + " order by insurer, or_date, u.id";

  private static final String YTD =
      "select x.criteria_no, x.description, count(*) as cnt, sum(x.balance) as amount from ("
          + "select u.balance, cast(:to as date) - coalesce(r.receipt_date, cast(u.created_at as date))"
          + " as age, case"
          + " when cast(:to as date) - coalesce(r.receipt_date, cast(u.created_at as date)) <= 30"
          + " then 1 when cast(:to as date) - coalesce(r.receipt_date, cast(u.created_at as date))"
          + " <= 60 then 2 when cast(:to as date) - coalesce(r.receipt_date,"
          + " cast(u.created_at as date)) <= 90 then 3 when cast(:to as date) -"
          + " coalesce(r.receipt_date, cast(u.created_at as date)) <= 120 then 4"
          + " when cast(:to as date) - coalesce(r.receipt_date, cast(u.created_at as date)) <= 180"
          + " then 5 when cast(:to as date) - coalesce(r.receipt_date, cast(u.created_at as date))"
          + " <= 365 then 6 else 7 end as criteria_no,"
          + " case when cast(:to as date) - coalesce(r.receipt_date, cast(u.created_at as date))"
          + " <= 365 then 'Unapplied 365 days or less, by age' else 'Unapplied above 365 days' end"
          + " as description"
          + COMMISSION_UNAPPLIED
          + " and cast(u.created_at at time zone '"
          + ZONE
          + "' as date) >= cast(date_trunc('year', cast(:to as date)) as date)) x"
          + " group by x.criteria_no, x.description order by x.criteria_no";

  private static final String MINIMAL =
      "select x.receipt_no, x.receipt_date, m.invoice_no, m.sales_unit as market_unit,"
          + " b.code as section_unit, m.amount, 1 as cnt from csh_minimal_balance m"
          + " left join ops_invoice i on i.invoice_no = m.invoice_no"
          + " left join org_branch b on b.id = i.branch_id"
          + " left join lateral (select r.receipt_no, r.receipt_date from csh_receipt r"
          + " where r.kind = :kind and (exists (select 1 from csh_application a"
          + " where a.receipt_id = r.id and a.invoice_no = m.invoice_no)"
          + " or exists (select 1 from csh_receipt_line l where l.receipt_id = r.id"
          + " and l.invoice_no = m.invoice_no)) order by r.id desc limit 1) x on true"
          + " where m.company_id = :company and m.swept_on between :from and :to"
          + " and m.kind = :minimalKind order by m.swept_on, m.invoice_no";

  /**
   * Cancelled Official Receipts (FRS.CSH.09.02.04).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport cancelledOrReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-CANCELLED-OR",
            "Cancelled Official Receipts",
            "ORs cancelled in the period with the reason",
            CANCELLED.replace(KIND, "'OR'"),
            List.of(
                ReportColumn.date("date_issued", "Date Issued"),
                ReportColumn.text(RECEIPT_NO, "Official Receipt Number"),
                ReportColumn.text("assured", "Assured"),
                ReportColumn.amount("gross", "Gross Amount"),
                ReportColumn.amount("vat", "VAT"),
                ReportColumn.amount("wtax", "WTAX"),
                ReportColumn.amount(AMOUNT, "Amount"),
                ReportColumn.text("reason", "Reason for Cancellation"),
                ReportColumn.count(COUNT, COUNT_LABEL)),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Cancelled Acknowledgement Receipts (FRS.CSH.09.02.05).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport cancelledArReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-CANCELLED-AR",
            "Cancelled Acknowledgment Receipts",
            "ARs cancelled in the period with the reason",
            CANCELLED.replace(KIND, "'AR'"),
            List.of(
                ReportColumn.date("date_issued", "Date Issued"),
                ReportColumn.text("reason", "Reason for Cancellation"),
                ReportColumn.text(RECEIPT_NO, "Acknowledgment Receipt Number"),
                ReportColumn.text("assured", "Assured"),
                ReportColumn.amount("gross", "Gross Amount"),
                ReportColumn.amount("vat", "VAT"),
                ReportColumn.amount("wtax", "WTAX"),
                ReportColumn.amount(AMOUNT, "Amount"),
                ReportColumn.count(COUNT, COUNT_LABEL)),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Unapplied Commission Receivable Extract for Mancom Reports (FRS.CSH.09.02.06).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport commissionMancomReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-UNAPPLIED-COMM-MANCOM",
            "Unapplied Commission Receivable Extract for Mancom Reports",
            "Commission ORs with an OR balance at the end date",
            MANCOM,
            List.of(
                ReportColumn.text("insurer", "Insurance Company Name"),
                ReportColumn.text("assured", "Assured Name"),
                ReportColumn.text("or_no", "OR Number"),
                ReportColumn.text("invoice_no", "Invoice Number"),
                new ReportColumn("invoices", "Number of Invoices", ColumnType.NUMBER, false),
                ReportColumn.text("payment_type", "Payment Type"),
                ReportColumn.amount("booked_commission", "Booked Commission"),
                ReportColumn.amount("commission_setup", "Commission Set-Up"),
                ReportColumn.amount("check_amount", "Check Amount"),
                ReportColumn.amount("applied", "Applied Amount"),
                ReportColumn.amount("or_balance", "OR Balance"),
                ReportColumn.date("or_date", "OR Date"),
                ReportColumn.text("policy_no", "Policy Number"),
                ReportColumn.text("unit_head", "Marketing Unit Head"),
                ReportColumn.text("department", "Corporate Department"),
                ReportColumn.text("corporate_name", "Corporate Name"),
                ReportColumn.text("encoder", "Encoder"),
                new ReportColumn("age", "Age", ColumnType.NUMBER, false)),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Unapplied Commission Receivable Payments YTD balance per Criteria (FRS.CSH.09.02.07); the
   * criteria are the ageing brackets of FRS.CSH.09.02.11 until the client gives its own.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport commissionYtdReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-UNAPPLIED-COMM-YTD",
            "Unapplied Commission Receivable Payments YTD balance per Criteria",
            "Year-to-date unapplied commission receivable payments per criteria",
            YTD,
            List.of(
                new ReportColumn("criteria_no", "Criteria No.", ColumnType.NUMBER, false),
                ReportColumn.text("description", "Description"),
                ReportColumn.count(COUNT, COUNT_LABEL),
                ReportColumn.amount(AMOUNT, "Amount Balance")),
            null,
            null,
            "Criteria 1 to 7: ageing 0-30, 31-60, 61-90, 91-120, 121-180, 181-365, above 365 days."),
        jdbc);
  }

  /**
   * Unapplied Premium Minimal Balance (FRS.CSH.09.02.08).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport premiumMinimalReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-MINBAL-PREMIUM",
            "Unapplied Premium Minimal Balance",
            "ARs with a premium minimal balance reversed in the period",
            MINIMAL.replace(KIND, "'AR'").replace(":minimalKind", "'PREMIUM'"),
            List.of(
                ReportColumn.text(RECEIPT_NO, "AR Number"),
                ReportColumn.date("receipt_date", "AR Date"),
                ReportColumn.text("market_unit", "Market Unit"),
                ReportColumn.text("section_unit", "Section Unit"),
                ReportColumn.amount(AMOUNT, "Minimal Amount"),
                ReportColumn.count(COUNT, COUNT_LABEL)),
            null,
            null,
            null),
        jdbc);
  }

  /**
   * Unapplied Commission Minimal Balance (FRS.CSH.09.02.09).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport commissionMinimalReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-MINBAL-COMMISSION",
            "Unapplied Commission Minimal Balance",
            "ORs with a commission minimal balance reversed in the period",
            MINIMAL.replace(KIND, "'OR'").replace(":minimalKind", "'COMMISSION'"),
            List.of(
                ReportColumn.text(RECEIPT_NO, "OR Number"),
                ReportColumn.text("invoice_no", "Invoice Number"),
                ReportColumn.date("receipt_date", "OR Date"),
                ReportColumn.text("market_unit", "Market Unit"),
                ReportColumn.text("section_unit", "Section Unit"),
                ReportColumn.amount(AMOUNT, "Minimal Amount"),
                ReportColumn.count(COUNT, COUNT_LABEL)),
            null,
            null,
            null),
        jdbc);
  }
}
