package com.iortatechnxt.brokerverse.cashiering.report;

import com.iortatechnxt.brokerverse.cashiering.report.SqlReport.Spec;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Cashiering reports of the legacy context (DATA_MIGRATION_DESIGN 14.4 D and F): the legacy
 * unapplied payments carried over from the old systems, with their age and disposition, and the
 * unapplied payments reclassified to other income.
 */
@Configuration(proxyBeanMethods = false)
public class LegacyCashReports {

  private static final String LEGACY_UPP =
      "select u.reference, u.source_system, u.legacy_ar_no, u.legacy_ar_date, u.payor_name,"
          + " u.client_code, u.currency, u.amount, u.balance,"
          + " cast(:to as date) - coalesce(u.legacy_ar_date, cast(u.created_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date)) as age_days, u.stage, u.disposition_hint, u.migration_batch,"
          + " (select max(b.batch_no) from csh_legacy_batch_line l join csh_legacy_batch b"
          + " on b.id = l.batch_id where l.unapplied_id = u.id and l.status <> 'FAILED'"
          + " and b.status <> 'CANCELLED') as income_batch"
          + " from csh_unapplied u where u.company_id = :company and u.ledger_context = 'LEGACY'"
          + " and coalesce(u.legacy_ar_date, cast(u.created_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date)) <= :to order by u.source_system, u.legacy_ar_date, u.reference";

  private static final String INCOME_RECLASS =
      "select cast(b.executed_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) as posted_on, b.batch_no, l.reference, l.ledger_context, l.age_days,"
          + " l.amount, l.reason, b.created_by as requested_by, b.first_approved_by,"
          + " b.final_approved_by, l.journal_batch_no"
          + " from csh_legacy_batch b join csh_legacy_batch_line l on l.batch_id = b.id"
          + " where b.company_id = :company and b.kind = 'INCOME_RECLASS' and l.status = 'POSTED'"
          + " and cast(b.executed_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) between :from and :to order by b.executed_at, b.batch_no, l.line_no";

  /**
   * Legacy unapplied payments as at the end of the period.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport legacyUnappliedReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-UPP-LEGACY",
            "Legacy Unapplied Payments",
            "Unapplied payments carried over from the legacy systems, with legacy receipt, age,"
                + " stage and disposition",
            LEGACY_UPP,
            List.of(
                ReportColumn.text("reference", "Reference"),
                ReportColumn.text("legacy_ar_no", "Legacy AR No."),
                ReportColumn.date("legacy_ar_date", "Legacy AR Date"),
                ReportColumn.text("payor_name", "Payor"),
                ReportColumn.text("client_code", "Client"),
                ReportColumn.text("currency", "Currency"),
                ReportColumn.amount("amount", "Original Amount"),
                ReportColumn.amount("balance", "Balance"),
                ReportColumn.count("age_days", "Age (days)"),
                ReportColumn.text("stage", "Stage"),
                ReportColumn.text("disposition_hint", "Disposition"),
                ReportColumn.text("income_batch", "Income Batch"),
                ReportColumn.text("migration_batch", "Migration Batch")),
            "source_system",
            "Legacy System",
            "The age runs from the legacy receipt date to the end of the period"),
        jdbc);
  }

  /**
   * Unapplied payments reclassified to other income in the period.
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  SqlReport incomeReclassReport(NamedParameterJdbcTemplate jdbc) {
    return new SqlReport(
        new Spec(
            "CSH-UPP-INCOME-RECLASS",
            "Unapplied Payments Reclassified to Income",
            "Unapplied payments, new and legacy, taken to other income after the two approvals",
            INCOME_RECLASS,
            List.of(
                ReportColumn.date("posted_on", "Posted On"),
                ReportColumn.text("reference", "Reference"),
                ReportColumn.text("ledger_context", "Ledger"),
                ReportColumn.count("age_days", "Age (days)"),
                ReportColumn.amount("amount", "Amount"),
                ReportColumn.text("reason", "Reason"),
                ReportColumn.text("requested_by", "Requested By"),
                ReportColumn.text("first_approved_by", "Team Lead"),
                ReportColumn.text("final_approved_by", "Top Management"),
                ReportColumn.text("journal_batch_no", "Journal")),
            "batch_no",
            "Batch",
            null),
        jdbc);
  }
}
