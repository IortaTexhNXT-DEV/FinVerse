package com.iortatechnxt.brokerverse.migration.report;

import static com.iortatechnxt.brokerverse.migration.report.MigReport.PHT;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The GL reports of the Data Migration (DATA_MIGRATION_DESIGN 14.2, 17.7 and 21): Migration
 * Clearing and the legacy control accounts against the open legacy invoices, the register of the
 * FY2027 opening-balance adjustments and their reconciliation.
 */
@Configuration(proxyBeanMethods = false)
public class GlReports {

  private static final String CURRENCY = "currency";
  private static final String CURRENCY_LABEL = "Currency";
  private static final String STATUS = "status";

  /**
   * {@code MIG-GL-CLEARING}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migGlClearingReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-GL-CLEARING",
            "Migration Clearing and Legacy Control Accounts",
            "Migration Clearing per branch and currency (zero after the opening) and each legacy"
                + " control account against the open legacy invoices")
        .sql(
            "select 'Migration Clearing' as check_name, b.code as branch, e.currency, a.code as account,"
                + " sum(e.debit_fc - e.credit_fc) as gl_balance, 0 as detail,"
                + " sum(e.debit_fc - e.credit_fc) as difference"
                + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
                + " join org_branch b on b.id = e.branch_id"
                + " where e.company_id = :company and a.code = 'LGC-CLR'"
                + " group by b.code, e.currency, a.code"
                + " union all "
                + "select 'Legacy control account', 'All', g.currency, g.account_code, g.gl_balance,"
                + " coalesce(d.detail, 0), g.gl_balance - coalesce(d.detail, 0)"
                + " from (select a.code as account_code, e.currency,"
                + " sum(case when a.code = 'LGC-DTIP' then e.credit_fc - e.debit_fc"
                + " else e.debit_fc - e.credit_fc end) as gl_balance"
                + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
                + " where e.company_id = :company and a.code in ('1215.01', '1215.02', '1215.03',"
                + " '1215.04', '1215.05', '1215.06', '1216', 'LGC-DTIP', 'LGC-COMM')"
                + " group by a.code, e.currency) g"
                + " left join (select case c.component when 'BASIC' then '1215.01'"
                + " when 'DST' then '1215.02' when 'PREMIUM_TAX_VAT' then '1215.03'"
                + " when 'LGT' then '1215.04' when 'FST' then '1215.05' when 'OTHER' then '1215.06'"
                + " when 'PR2307' then '1216' when 'DTIP' then 'LGC-DTIP' else 'LGC-COMM' end"
                + " as account_code, i.currency,"
                + " sum(case when c.component <> 'WTAX' then c.balance"
                + " when i.origin = 'MIGRATED' then -c.balance else 0 end) as detail"
                + " from ops_invoice i join ops_invoice_component c on c.invoice_id = i.id"
                + " where i.company_id = :company and i.ledger_context = 'LEGACY'"
                + " group by 1, i.currency) d"
                + " on d.account_code = g.account_code and d.currency = g.currency"
                + " order by 1 desc, 2, 3, 4")
        .columns(
            ReportColumn.text("check_name", "Check"),
            ReportColumn.text("branch", "Branch"),
            ReportColumn.text(CURRENCY, CURRENCY_LABEL),
            ReportColumn.text("account", "Account"),
            ReportColumn.amountNoTotal("gl_balance", "GL Balance"),
            ReportColumn.amountNoTotal("detail", "Open Detail"),
            ReportColumn.amountNoTotal("difference", "Difference"))
        .build();
  }

  /**
   * {@code MIG-TRUEUP-REGISTER}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migTrueupRegisterReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-TRUEUP-REGISTER",
            "Opening-Balance Adjustments",
            "FY2027 true-ups with their batches, postings, approvals and sign-off")
        .sql(
            "select t.reference, t.trueup_no, t.as_of, t.status, b.batch_no, tb.batch_no as tb_batch,"
                + " t.journals_posted, t.items_adjusted, t.prepared_by,"
                + " to_char(t.prepared_at"
                + PHT
                + " as prepared_at, t.approved_by, to_char(t.approved_at"
                + PHT
                + " as approved_at, to_char(t.posted_at"
                + PHT
                + " as posted_at, t.signed_by, to_char(t.signed_at"
                + PHT
                + " as signed_at, t.remarks"
                + " from mig_trueup t left join mig_batch b on b.id = t.batch_id"
                + " left join mig_batch tb on tb.id = t.tb_batch_id"
                + " where t.company_id = :company order by t.id")
        .columns(
            ReportColumn.text("reference", "Reference"),
            ReportColumn.text("trueup_no", "No."),
            ReportColumn.date("as_of", "As of"),
            ReportColumn.text(STATUS, "Status"),
            ReportColumn.text("batch_no", "Adjustment Batch"),
            ReportColumn.text("tb_batch", "Trial Balance Batch"),
            ReportColumn.count("journals_posted", "Journals"),
            ReportColumn.count("items_adjusted", "Items Adjusted"),
            ReportColumn.text("prepared_by", "Prepared by"),
            ReportColumn.text("prepared_at", "Prepared at"),
            ReportColumn.text("approved_by", "Approved by"),
            ReportColumn.text("approved_at", "Approved at"),
            ReportColumn.text("posted_at", "Posted at"),
            ReportColumn.text("signed_by", "Signed by"),
            ReportColumn.text("signed_at", "Signed at"),
            ReportColumn.text("remarks", "Remarks"))
        .row(MigReport.relabel(STATUS))
        .build();
  }

  /**
   * {@code MIG-TRUEUP-RECON}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migTrueupReconReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-TRUEUP-RECON",
            "Opening-Balance Adjustment Reconciliation",
            "Movement against the legacy trial balances, Migration Clearing and the counts of each"
                + " FY2027 true-up")
        .sql(
            "select t.reference, x.run_no, l.level, l.measure, l.currency, l.source_value,"
                + " l.target_value, l.difference, l.status, l.explanation"
                + " from mig_trueup t join mig_recon_run x on x.batch_id = t.batch_id"
                + " join mig_recon_line l on l.run_id = x.id"
                + " where t.company_id = :company"
                + " and x.id = (select max(y.id) from mig_recon_run y where y.batch_id = t.batch_id)"
                + " order by t.id, l.level, l.id")
        .columns(
            ReportColumn.text("reference", "Adjustment"),
            ReportColumn.text("run_no", "Run"),
            ReportColumn.text("level", "Level"),
            ReportColumn.text("measure", "Check"),
            ReportColumn.text(CURRENCY, CURRENCY_LABEL),
            ReportColumn.amountNoTotal("source_value", "Legacy"),
            ReportColumn.amountNoTotal("target_value", "BIBS"),
            ReportColumn.amountNoTotal("difference", "Difference"),
            ReportColumn.text(STATUS, "Status"),
            ReportColumn.text("explanation", "Explanation"))
        .groupBy("reference", "Adjustment")
        .row(MigReport.relabel(STATUS))
        .build();
  }
}
