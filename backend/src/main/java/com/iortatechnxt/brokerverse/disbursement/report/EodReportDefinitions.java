package com.iortatechnxt.brokerverse.disbursement.report;

import com.iortatechnxt.brokerverse.disbursement.report.SqlReport.Spec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * The end-of-day reports of Appendix B and DIS 3.28.2: the vouchers approved on the business date
 * (the From-To range), per group of disbursement types, and the summary of the day. The EOD run
 * produces them for its date ({@code DISB_EOD_REPORTS}); they can also be run for any range.
 */
@Configuration(proxyBeanMethods = false)
public class EodReportDefinitions {

  private static final String APPROVED_IN_RANGE =
      " where v.company_id = :companyId and v.approved_at is not null"
          + " and v.posting_status in ('POSTED', 'REVERSED')"
          + " and cast(v.approved_at at time zone 'Asia/Manila' as date) between :from and :to";

  /**
   * {@code DSB-EOD-REMIT}: remittances to insurers (withholding 2% / 15% in the tax column).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbEodRemit(NamedParameterJdbcTemplate jdbc) {
    return eod(jdbc, "DSB-EOD-REMIT", "Remittance End-of-Day", "'REMITTANCE'");
  }

  /**
   * {@code DSB-EOD-REFUND}: refunds to clients.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbEodRefund(NamedParameterJdbcTemplate jdbc) {
    return eod(jdbc, "DSB-EOD-REFUND", "Refund End-of-Day", "'REFUND', 'REFUND_FROM_INSURER'");
  }

  /**
   * {@code DSB-EOD-SUPPLIER}: payments to suppliers, government agencies and other bank units.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbEodSupplier(NamedParameterJdbcTemplate jdbc) {
    return eod(
        jdbc,
        "DSB-EOD-SUPPLIER",
        "Payment to Supplier End-of-Day",
        "'SUPPLIER', 'GOVERNMENT', 'OTHER_BANK_UNIT'");
  }

  /**
   * {@code DSB-EOD-EMPLOYEE}: employee-related payments and cash advances.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbEodEmployee(NamedParameterJdbcTemplate jdbc) {
    return eod(
        jdbc, "DSB-EOD-EMPLOYEE", "Employee-related End-of-Day", "'EMPLOYEE', 'CASH_ADVANCE'");
  }

  /**
   * {@code DSB-EOD-OTHER}: service fees, incentive pass-on, re-issued checks and other payments.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbEodOther(NamedParameterJdbcTemplate jdbc) {
    return eod(
        jdbc,
        "DSB-EOD-OTHER",
        "Other Disbursements End-of-Day",
        "'SERVICE_FEE', 'PASS_ON', 'STALE_REISSUE', 'OTHER', 'CWT2307'");
  }

  private static ReportDefinition eod(
      NamedParameterJdbcTemplate jdbc, String code, String title, String types) {
    return new SqlReport(
        new Spec(
            code,
            title,
            title + ": vouchers approved on the business date (DIS 3.28.2)",
            true,
            "select v.mode as mode_group, "
                + DisbursementReports.VOUCHER_COLUMNS
                + ", v.approved_by, v.stage"
                + DisbursementReports.VOUCHER_FROM
                + APPROVED_IN_RANGE
                + " and v.disbursement_type in ("
                + types
                + ") order by v.mode, v.id",
            DisbursementReports.voucherColumns(
                ReportColumn.text("approved_by", "Approved By"),
                ReportColumn.text("stage", "Status")),
            "mode_group",
            "Mode of Payment",
            null),
        jdbc);
  }

  /**
   * {@code DSB-EOD-SUMMARY}: the day's approved vouchers per disbursement type and mode.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbEodSummary(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-EOD-SUMMARY",
            "Disbursement Summary End-of-Day",
            "Vouchers approved on the business date per disbursement type and mode (DIS 3.28.2)",
            true,
            "select v.disbursement_type as type, v.mode, v.currency, count(*) as items,"
                + " sum(v.gross) as gross, sum(v.ewt) as ewt, sum(v.net) as net"
                + " from dsb_voucher v"
                + APPROVED_IN_RANGE
                + " group by v.disbursement_type, v.mode, v.currency order by 1, 2, 3",
            List.of(
                ReportColumn.text("mode", "Mode"),
                ReportColumn.text("currency", "Currency"),
                ReportColumn.count("items", "Vouchers"),
                ReportColumn.amount("gross", "Gross"),
                ReportColumn.amount("ewt", "Withholding Tax"),
                ReportColumn.amount("net", "Net Amount")),
            "type",
            "Disbursement Type",
            null),
        jdbc);
  }
}
