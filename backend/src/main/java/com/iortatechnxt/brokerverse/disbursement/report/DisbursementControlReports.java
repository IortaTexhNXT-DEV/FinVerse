package com.iortatechnxt.brokerverse.disbursement.report;

import com.iortatechnxt.brokerverse.disbursement.report.SqlReport.Spec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * The Disbursement control reports (DIS 3.28.1, 3.25.2, 3.28.4, 3.27.0): the payee report of
 * Appendix B, the payment requests whose payee did not match the master, the fall-out of request
 * uploads and the unregularised transactions.
 */
@Configuration(proxyBeanMethods = false)
public class DisbursementControlReports {

  private static final String PAYEE_NAME = "Payee Name";

  /**
   * {@code DSB-PAYEE}: the payee master (DIS 3.28.1): name, address, account (masked), mode of
   * payment, disbursement types, source and status.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbPayee(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-PAYEE",
            "Payee Report",
            "Payees with address, account, mode of payment, disbursement types and source (DIS 3.28.1)",
            false,
            "select p.payee_class, p.payee_code, p.name, p.address,"
                + " (select case when length(a.account_no) > 4 then repeat('*', length(a.account_no) - 4)"
                + " || right(a.account_no, 4) else a.account_no end from dsb_payee_account a"
                + " where a.payee_id = p.id and a.active order by a.primary_account desc, a.id limit 1)"
                + " as account_no, p.default_mode, p.allowed_modes, p.disbursement_types, p.currency,"
                + " p.source, p.stage from dsb_payee p where p.company_id = :companyId"
                + " order by p.payee_class, p.name",
            List.of(
                ReportColumn.text("payee_code", "Payee Code"),
                ReportColumn.text("name", PAYEE_NAME),
                ReportColumn.text("address", "Address"),
                ReportColumn.text("account_no", "Account No."),
                ReportColumn.text("default_mode", "Mode of Payment"),
                ReportColumn.text("allowed_modes", "Allowed Modes"),
                ReportColumn.text("disbursement_types", "Disbursement Types"),
                ReportColumn.text("currency", "Currency"),
                ReportColumn.text("source", "Source"),
                ReportColumn.text("stage", "Status")),
            "payee_class",
            "Payee Class",
            "Account numbers are masked; the full number is shown on the payee page to"
                + " DISB_PAYEE_VIEW_FULL."),
        jdbc);
  }

  /**
   * {@code DSB-PAYEE-NOMATCH}: payment requests of the period whose payee did not match the master
   * (DIS 3.25.2).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbPayeeNoMatch(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-PAYEE-NOMATCH",
            "Payees Not Matched",
            "Payment requests whose payee is not in the payee master (DIS 3.25.2)",
            true,
            "select r.source_module, r.request_no, r.source_ref, r.payee_code, r.payee_name,"
                + " r.disbursement_type, r.currency, r.amount, r.received_at, r.status"
                + " from dsb_request r where r.company_id = :companyId"
                + " and (r.status = 'NO_PAYEE' or r.status_reason like 'PAYEE_NOT_MAINTAINED%')"
                + " and cast(r.received_at at time zone 'Asia/Manila' as date) between :from and :to"
                + " order by r.source_module, r.id",
            List.of(
                ReportColumn.text("request_no", "Request No."),
                ReportColumn.text("source_ref", "Source Reference"),
                ReportColumn.text("payee_code", "Payee Code"),
                ReportColumn.text("payee_name", PAYEE_NAME),
                ReportColumn.text("disbursement_type", "Type"),
                ReportColumn.text("currency", "Currency"),
                ReportColumn.amount("amount", "Amount"),
                ReportColumn.date("received_at", "Received"),
                ReportColumn.text("status", "Status")),
            "source_module",
            "Requesting Module",
            null),
        jdbc);
  }

  /**
   * {@code DSB-UPLOAD-FALLOUT}: rows of the request uploads of the period that were refused or
   * failed, with their reasons (DIS 3.28.4, 2.5.1).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbUploadFallout(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-UPLOAD-FALLOUT",
            "Request Upload Fall-out",
            "Refused or failed rows of the disbursement request uploads, with reasons (DIS 3.28.4)",
            true,
            "select j.job_no || ' - ' || j.file_name as upload, r.row_no, r.status, r.messages,"
                + " j.created_by, j.created_at from bulk_row r join bulk_job j on j.id = r.job_id"
                + " where j.company_id = :companyId and j.handler_code = 'DISB_REQUESTS'"
                + " and r.status in ('INVALID', 'FAILED')"
                + " and cast(j.created_at at time zone 'Asia/Manila' as date) between :from and :to"
                + " order by j.id, r.row_no",
            List.of(
                ReportColumn.count("row_no", "Row"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text("messages", "Reasons"),
                ReportColumn.text("created_by", "Uploaded By"),
                ReportColumn.date("created_at", "Uploaded On")),
            "upload",
            "Upload",
            null),
        jdbc);
  }

  /**
   * {@code DSB-UNREGULARIZED}: vouchers whose posting or reversal failed, and cancelled approved
   * vouchers whose source was not told of the cancellation (DIS 3.27.0, 2.20.0).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition dsbUnregularized(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "DSB-UNREGULARIZED",
            "Unregularised Transactions",
            "Vouchers with a failed posting or reversal and cancellations not regularised (DIS 3.27.0)",
            true,
            "select case when v.posting_status in ('FAILED', 'REVERSAL_FAILED') then 'Posting failed'"
                + " else 'Source not regularised' end as issue, v.dv_no, v.payee_name,"
                + " v.disbursement_type, v.stage, v.posting_status, coalesce(v.posting_error,"
                + " v.cancel_reason) as detail, v.net from dsb_voucher v"
                + " join dsb_request r on r.id = v.request_id"
                + " left join ops_disbursement_request q on q.id = r.gateway_request_id"
                + " where v.company_id = :companyId"
                + " and (v.posting_status in ('FAILED', 'REVERSAL_FAILED')"
                + " or (v.stage = 'CANCELLED' and v.posting_status = 'REVERSED'"
                + " and q.id is not null and q.status <> 'CANCELLED'))"
                + " and cast(coalesce(v.updated_at, v.created_at) at time zone 'Asia/Manila' as date)"
                + " <= :to order by 1, v.id",
            List.of(
                ReportColumn.text("dv_no", "DV No."),
                ReportColumn.text("payee_name", "Payee"),
                ReportColumn.text("disbursement_type", "Type"),
                ReportColumn.text("stage", "DV Status"),
                ReportColumn.text("posting_status", "Posting"),
                ReportColumn.text("detail", "Detail"),
                ReportColumn.amount("net", "Net Amount")),
            "issue",
            "Issue",
            "Listed until the posting is redone or the source restores its records."),
        jdbc);
  }
}
