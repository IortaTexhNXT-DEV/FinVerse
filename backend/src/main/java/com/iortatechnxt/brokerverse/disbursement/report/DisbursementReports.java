package com.iortatechnxt.brokerverse.disbursement.report;

import com.iortatechnxt.brokerverse.disbursement.report.SqlReport.Spec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import java.time.ZoneId;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * The real-time Disbursement reports of Appendix B (DIS 2.3.0-2.3.9, 3.28.1, 3.28.3-3.28.4, 3.25.2,
 * 3.27.0): masterlist, unreleased checks, CWT on commission, authorities to debit, Miscellaneous
 * Liability stale checks, cash flow, payees, payees not matched, upload fall-out and unregularised
 * transactions. The end-of-day reports are in {@link EodReportDefinitions}.
 */
@Configuration(proxyBeanMethods = false)
public class DisbursementReports {

  /** Philippine time of the report dates. */
  static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  /** Voucher columns shared by the voucher reports. */
  static final String VOUCHER_COLUMNS =
      "v.dv_no, r.request_no, r.rfp_no, v.payee_code, v.payee_name, v.mode, i.instrument_no,"
          + " v.currency, v.gross, v.ewt, v.net";

  /** Vouchers with their request and instrument. */
  static final String VOUCHER_FROM =
      " from dsb_voucher v join dsb_request r on r.id = v.request_id"
          + " left join dsb_instrument i on i.voucher_id = v.id";

  private static final String DV_NO_KEY = "dv_no";
  private static final String AMOUNT_KEY = "amount";
  private static final String PAYEE = "Payee";
  private static final String AMOUNT = "Amount";
  private static final String DV_NO = "DV No.";
  private static final String CHECK_NO = "Check No.";
  private static final String AGEING =
      "case when d.days <= 30 then '1. Current - 30' when d.days <= 60 then '2. 31 - 60'"
          + " when d.days <= 90 then '3. 61 - 90' when d.days <= 120 then '4. 91 - 120'"
          + " when d.days <= 150 then '5. 121 - 150' when d.days <= 180 then '6. 151 - 180'"
          + " else '7. 181 and over' end";
  private static final String CHECKS_AGED =
      "select "
          + AGEING
          + " as bucket, d.* from (select i.instrument_no, v.dv_no, v.payee_name, i.printed_on,"
          + " i.status, :to - i.printed_on as days, i.amount from dsb_instrument i"
          + " join dsb_voucher v on v.id = i.voucher_id where v.company_id = :companyId"
          + " and i.mode = 'CHECK' and i.printed_on <= :to and i.status in (%s)) d"
          + " order by bucket, d.printed_on, d.instrument_no";

  /**
   * {@code DSB-MASTERLIST}: every voucher created in the period (DIS 3.28.3).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbMasterlist(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-MASTERLIST",
            "Masterlist of Disbursements",
            "Every disbursement voucher of the period with payee, mode, amounts and status (DIS 3.28.3)",
            true,
            "select v.disbursement_type as type, "
                + VOUCHER_COLUMNS
                + ", v.stage, i.status as instrument_status, v.journal_no, v.created_at"
                + VOUCHER_FROM
                + " where v.company_id = :companyId"
                + " and cast(v.created_at at time zone 'Asia/Manila' as date) between :from and :to"
                + " order by v.disbursement_type, v.id",
            voucherColumns(
                ReportColumn.text("stage", "DV Status"),
                ReportColumn.text("instrument_status", "Instrument Status"),
                ReportColumn.text("journal_no", "Journal"),
                ReportColumn.date("created_at", "Created")),
            "type",
            "Disbursement Type",
            null),
        jdbc);
  }

  /**
   * The columns of a voucher report followed by extra columns.
   *
   * @param extra extra columns
   * @return columns
   */
  static List<ReportColumn> voucherColumns(ReportColumn... extra) {
    List<ReportColumn> cols =
        new java.util.ArrayList<>(
            List.of(
                ReportColumn.text(DV_NO_KEY, DV_NO),
                ReportColumn.text("request_no", "Request No."),
                ReportColumn.text("rfp_no", "RFP No."),
                ReportColumn.text("payee_code", "Payee Code"),
                ReportColumn.text("payee_name", PAYEE),
                ReportColumn.text("mode", "Mode"),
                ReportColumn.text("instrument_no", "Instrument No."),
                ReportColumn.text("currency", "Currency"),
                ReportColumn.amount("gross", "Gross"),
                ReportColumn.amount("ewt", "Withholding Tax"),
                ReportColumn.amount("net", "Net Amount")));
    cols.addAll(List.of(extra));
    return cols;
  }

  /**
   * {@code DSB-UNRELEASED-CHECKS}: printed checks not yet released, aged from the print date to the
   * end date (current-30 ... 151-180, DIS 3.28.3).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbUnreleasedChecks(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-UNRELEASED-CHECKS",
            "Unreleased Checks",
            "Printed checks not released to the payee, aged from the print date (DIS 3.28.3)",
            true,
            String.format(CHECKS_AGED, "'PRINTED'"),
            checkColumns(),
            "bucket",
            "Ageing",
            "Aged at the To date."),
        jdbc);
  }

  /**
   * {@code DSB-ML-STALE}: checks not negotiated (printed, released or stale), aged to 181 days and
   * over; the stale ones are in Miscellaneous Liability - stale checks (DIS 3.28.3, 3.27.1).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbMlStale(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-ML-STALE",
            "Miscellaneous Liability - Stale Checks",
            "Unnegotiated checks aged to 181 days and over; stale ones in Miscellaneous Liability (DIS 3.28.3)",
            true,
            String.format(CHECKS_AGED, "'PRINTED', 'RELEASED', 'STALE'"),
            checkColumns(),
            "bucket",
            "Ageing",
            "Aged at the To date; checks become stale after DISB_STALE_DAYS."),
        jdbc);
  }

  private static List<ReportColumn> checkColumns() {
    return List.of(
        ReportColumn.text("instrument_no", CHECK_NO),
        ReportColumn.text(DV_NO_KEY, DV_NO),
        ReportColumn.text("payee_name", PAYEE),
        ReportColumn.date("printed_on", "Printed On"),
        ReportColumn.text("status", "Status"),
        ReportColumn.count("days", "Days"),
        ReportColumn.amount(AMOUNT_KEY, AMOUNT));
  }

  /**
   * {@code DSB-ATD}: authorities to debit issued in the period with their status (DIS 3.28.3).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbAtd(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-ATD",
            "Authority to Debit",
            "Authorities to debit of the period: printed, e-mailed to the branch and debited (DIS 3.28.3)",
            true,
            "select i.status, i.instrument_no, v.dv_no, v.payee_name, i.printed_at, i.emailed_at,"
                + " i.debited_at, i.reference, i.amount from dsb_instrument i"
                + " join dsb_voucher v on v.id = i.voucher_id where v.company_id = :companyId"
                + " and i.mode = 'ATD'"
                + " and cast(i.created_at at time zone 'Asia/Manila' as date) between :from and :to"
                + " order by i.status, i.id",
            List.of(
                ReportColumn.text("instrument_no", "ATD No."),
                ReportColumn.text(DV_NO_KEY, DV_NO),
                ReportColumn.text("payee_name", PAYEE),
                ReportColumn.date("printed_at", "Printed"),
                ReportColumn.date("emailed_at", "E-mailed"),
                ReportColumn.date("debited_at", "Debited"),
                ReportColumn.text("reference", "Branch Reference"),
                ReportColumn.amount(AMOUNT_KEY, AMOUNT)),
            "status",
            "Status",
            null),
        jdbc);
  }

  /**
   * {@code DSB-CASH-FLOW}: per paying account, the approved payments of the period by mode and
   * instrument status: in process, for crediting, paid (DIS 3.28.3).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbCashFlow(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-CASH-FLOW",
            "Disbursement Cash Flow",
            "Approved payments per paying account, mode and instrument status (DIS 3.28.3)",
            true,
            "select b.code || ' - ' || b.name as bank, v.mode, coalesce(i.status, 'PENDING') as"
                + " status, count(*) as items, sum(v.net) as amount from dsb_voucher v"
                + " join pay_bank_account b on b.id = v.bank_account_id"
                + " left join dsb_instrument i on i.voucher_id = v.id"
                + " where v.company_id = :companyId and v.stage = 'APPROVED'"
                + " and cast(v.approved_at at time zone 'Asia/Manila' as date) between :from and :to"
                + " group by b.code, b.name, v.mode, i.status order by b.code, v.mode, 3",
            List.of(
                ReportColumn.text("mode", "Mode"),
                ReportColumn.text("status", "Instrument Status"),
                ReportColumn.count("items", "Items"),
                ReportColumn.amount(AMOUNT_KEY, AMOUNT)),
            "bank",
            "Paying Account",
            "Inter-office transfers and savings balances come from the bank reconciliation (AQ09)."),
        jdbc);
  }

  /**
   * {@code DSB-CWT-COMMISSION}: CWT certificates tagged on the vouchers of the period, per payee
   * (insurer or supplier) and direction (DIS 3.28.3, 2.11).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbCwtCommission(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-CWT-COMMISSION",
            "CWT / BIR 2307 on Commission",
            "CWT certificates received from insurers or released to suppliers, per payee (DIS 3.28.3)",
            true,
            "select v.payee_code || ' - ' || v.payee_name as payee, t.direction, t.doc_no,"
                + " t.period_from, t.period_to, coalesce(t.received_on, t.released_on) as tagged_on,"
                + " v.dv_no, t.amount from dsb_voucher_tag t join dsb_voucher v on v.id = t.voucher_id"
                + " where v.company_id = :companyId and t.kind = 'CWT'"
                + " and coalesce(t.received_on, t.released_on) between :from and :to"
                + " order by v.payee_code, t.id",
            List.of(
                ReportColumn.text("direction", "Received / Released"),
                ReportColumn.text("doc_no", "Certificate No."),
                ReportColumn.date("period_from", "Period From"),
                ReportColumn.date("period_to", "Period To"),
                ReportColumn.date("tagged_on", "Date"),
                ReportColumn.text(DV_NO_KEY, DV_NO),
                ReportColumn.amount(AMOUNT_KEY, AMOUNT)),
            "payee",
            PAYEE,
            "Comparison with the AR-BIR on commission and incentives balances waits for the"
                + " received-certificate register (AQ16)."),
        jdbc);
  }
}
