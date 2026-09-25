package com.iortatechnxt.brokerverse.commission.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The commission receivables reports (OPERATIONS_DESIGN 11): commission receivable direct payment
 * vs regular (CMR-COMMISSION-RECEIVABLE, CMRID.004), yearly production per branch and insurer with
 * estimated items (CMR-PRODUCTION-YEARLY, CMRID.014, RMTID.037), DP accounts per tag
 * (CMR-DP-STATUS, CMRID.008), incentive runs (CMR-INCENTIVE, CMRID.005/006), insurer feedback
 * timeline (CMR-FEEDBACK-SLA, CMRID.011) and BIR certificate submissions (CMR-BIR-CERT, CMRID.015).
 * Operations category: view and export permissions, archived runs. The layouts are drafts until
 * BDOI gives them (OQ42).
 */
@Configuration(proxyBeanMethods = false)
public class CommissionReports {

  private static final String COMPANY = "companyId";
  private static final String FROM = "from";
  private static final String TO = "to";
  private static final String INSURER = "insurer";
  private static final String INSURER_LABEL = "Insurance Company";
  private static final String INSURER_FILTER =
      " and (cast(:insurer as varchar) is null or %s = :insurer)";
  private static final String INVOICE = "invoice_no";
  private static final String INVOICE_LABEL = "Invoice No.";

  /**
   * Commission receivable, direct payment vs regular (CMRID.004).
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition commissionReceivableReport(NbReportJdbc jdbc) {
    return new Sql(
        jdbc,
        metadata(
            "CMR-COMMISSION-RECEIVABLE",
            "Commission Receivable - Direct Payment vs Regular",
            "Commission booked, collected and outstanding, net of VAT and withholding tax, per insurer"
                + " (CMRID.004)"),
        "select i.insurer_code as insurer,"
            + " case when i.dp_flag then 'Direct payment' else 'Regular' end as kind,"
            + " count(*) as invoices, sum(c.booked + c.adjusted) as commission,"
            + " sum(c.booked + c.adjusted - c.balance) as collected, sum(c.balance) as outstanding,"
            + " sum(c.balance + v.balance - w.balance) as net_outstanding"
            + " from ops_invoice i"
            + " join ops_invoice_component c on c.invoice_id = i.id and c.component = 'COMMISSION'"
            + " join ops_invoice_component v on v.invoice_id = i.id and v.component = 'COMMISSION_VAT'"
            + " join ops_invoice_component w on w.invoice_id = i.id and w.component = 'WTAX'"
            + " where i.company_id = :companyId and i.booking_date between :from and :to"
            + INSURER_FILTER.formatted("i.insurer_code")
            + " group by 1, 2 order by 1, 2",
        INSURER,
        INSURER_LABEL,
        List.of(
            ReportColumn.text("kind", "Commission"),
            ReportColumn.count("invoices", "Invoices"),
            ReportColumn.amount("commission", "Commission Booked"),
            ReportColumn.amount("collected", "Collected / Remitted"),
            ReportColumn.amount("outstanding", "Outstanding"),
            ReportColumn.amount("net_outstanding", "Net Outstanding")));
  }

  /**
   * Yearly production per branch and insurer with estimated items (CMRID.014).
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition productionYearlyReport(NbReportJdbc jdbc) {
    return new Sql(
        jdbc,
        metadata(
            "CMR-PRODUCTION-YEARLY",
            "Production per Branch and Insurer - Yearly",
            "Total production and commission per branch, insurer and year, estimated items apart"
                + " (CMRID.014, RMTID.037)"),
        "select coalesce(b.name, 'Unassigned') as branch, i.insurer_code as insurer,"
            + " cast(extract(year from i.booking_date) as varchar) as year, count(*) as invoices,"
            + " sum(i.gross_premium) as production, sum(i.commission) as commission,"
            + " count(*) filter (where i.estimated) as estimated_items,"
            + " coalesce(sum(i.gross_premium) filter (where i.estimated), 0) as estimated_amount"
            + " from ops_invoice i left join org_branch b on b.id = i.branch_id"
            + " where i.company_id = :companyId and i.booking_date between :from and :to"
            + INSURER_FILTER.formatted("i.insurer_code")
            + " group by 1, 2, 3 order by 1, 2, 3",
        "branch",
        "Branch",
        List.of(
            ReportColumn.text(INSURER, INSURER_LABEL),
            ReportColumn.text("year", "Year"),
            ReportColumn.count("invoices", "Invoices"),
            ReportColumn.amount("production", "Total Production"),
            ReportColumn.amount("commission", "Commission"),
            ReportColumn.count("estimated_items", "Estimated Items"),
            ReportColumn.amount("estimated_amount", "Estimated Amount")));
  }

  /**
   * Direct payment accounts per tag (CMRID.008).
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition dpStatusReport(NbReportJdbc jdbc) {
    return new Sql(
        jdbc,
        metadata(
            "CMR-DP-STATUS",
            "Direct Payment Accounts per Status",
            "DP accounts received in the period by tag, with billing, feedback and OR (CMRID.008)"),
        "select d.tag, d.insurer_code as insurer, d.branch_code as branch, d.invoice_no,"
            + " d.assured_name as assured, d.sanitation, d.net_commission, b.billing_no,"
            + " d.feedback_reason, d.or_no, d.collected_on"
            + " from cmr_dp_item d left join cmr_billing b on b.id = d.billing_id"
            + " where d.company_id = :companyId"
            + " and cast(d.created_at as date) between :from and :to"
            + INSURER_FILTER.formatted("d.insurer_code")
            + " order by d.tag, d.insurer_code, d.id",
        "tag",
        "Status",
        List.of(
            ReportColumn.text(INSURER, INSURER_LABEL),
            ReportColumn.text("branch", "Branch"),
            ReportColumn.text(INVOICE, INVOICE_LABEL),
            ReportColumn.text("assured", "Assured Name"),
            ReportColumn.text("sanitation", "Sanitation"),
            ReportColumn.amount("net_commission", "Net Commission"),
            ReportColumn.text("billing_no", "Billing No."),
            ReportColumn.text("feedback_reason", "Insurer Reason"),
            ReportColumn.text("or_no", "OR No."),
            ReportColumn.date("collected_on", "Collected On")));
  }

  /**
   * Incentive runs with their lines (CMRID.005/006).
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition incentiveReport(NbReportJdbc jdbc) {
    return new Sql(
        jdbc,
        metadata(
            "CMR-INCENTIVE",
            "Incentive Runs",
            "Incentive runs ending in the period with the incentive per invoice and exclusions"
                + " (CMRID.003/005/006)"),
        "select s.name || ' ' || r.run_no || ' (' || r.status || ')' as run, l.insurer_code as insurer,"
            + " l.sales_unit, l.invoice_no, l.gross_premium, l.basic_premium,"
            + " coalesce(l.exclusion_reason, '') as exclusion, l.incentive"
            + " from cmr_incentive_run r join cmr_incentive_scheme s on s.id = r.scheme_id"
            + " join cmr_incentive_run_line l on l.run_id = r.id"
            + " where r.company_id = :companyId and r.period_to between :from and :to"
            + INSURER_FILTER.formatted("l.insurer_code")
            + " order by r.id, l.id",
        "run",
        "Run",
        List.of(
            ReportColumn.text(INSURER, INSURER_LABEL),
            ReportColumn.text("sales_unit", "Branch / Unit"),
            ReportColumn.text(INVOICE, INVOICE_LABEL),
            ReportColumn.amount("gross_premium", "Gross Premium"),
            ReportColumn.amount("basic_premium", "Basic Premium"),
            ReportColumn.text("exclusion", "Excluded By"),
            ReportColumn.amount("incentive", "Incentive")));
  }

  /**
   * Insurer feedback timeline of the billings (CMRID.011).
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition feedbackSlaReport(NbReportJdbc jdbc) {
    return new Sql(
        jdbc,
        metadata(
            "CMR-FEEDBACK-SLA",
            "Insurer Feedback Timeline",
            "Billings sent in the period with the feedback due date and the days late (CMRID.011)"),
        "select b.insurer_code as insurer, b.billing_no, b.handler, cast(b.sent_at as date) as sent,"
            + " b.sla_due, cast(b.responded_at as date) as responded, b.stage, b.item_count,"
            + " b.total_net, cast(greatest(0, coalesce(cast(b.responded_at as date), current_date)"
            + " - b.sla_due) as varchar) as days_late"
            + " from cmr_billing b where b.company_id = :companyId and b.sent_at is not null"
            + " and cast(b.sent_at as date) between :from and :to"
            + INSURER_FILTER.formatted("b.insurer_code")
            + " order by b.insurer_code, b.id",
        INSURER,
        INSURER_LABEL,
        List.of(
            ReportColumn.text("billing_no", "Billing No."),
            ReportColumn.text("handler", "Handler"),
            ReportColumn.date("sent", "Sent"),
            ReportColumn.date("sla_due", "Feedback Due"),
            ReportColumn.date("responded", "Answered"),
            ReportColumn.text("days_late", "Days Late"),
            ReportColumn.text("stage", "Stage"),
            ReportColumn.count("item_count", "Accounts"),
            ReportColumn.amount("total_net", "Net Commission")));
  }

  /**
   * BIR certificate submissions (CMRID.015).
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition birCertificateReport(NbReportJdbc jdbc) {
    return new Sql(
        jdbc,
        metadata(
            "CMR-BIR-CERT",
            "BIR Certificate Submissions",
            "Withholding tax certificates tagged to ORs and submitted to Comptrollership (CMRID.015)"),
        "select c.insurer_code as insurer, c.submission_no, c.certificate_form, c.certificate_no,"
            + " c.period_from, c.period_to, c.tax_withheld,"
            + " (select string_agg(o.or_no, ', ' order by o.or_index) from cmr_certificate_or o"
            + " where o.certificate_id = c.id) as receipts, c.stage, c.submitted_count,"
            + " cast(c.decided_at as date) as decided, c.decided_by, c.reject_reason"
            + " from cmr_certificate c where c.company_id = :companyId"
            + " and cast(c.created_at as date) between :from and :to"
            + INSURER_FILTER.formatted("c.insurer_code")
            + " order by c.insurer_code, c.id",
        INSURER,
        INSURER_LABEL,
        List.of(
            ReportColumn.text("submission_no", "Submission No."),
            ReportColumn.text("certificate_form", "Form"),
            ReportColumn.text("certificate_no", "Certificate No."),
            ReportColumn.date("period_from", "Period From"),
            ReportColumn.date("period_to", "Period To"),
            ReportColumn.amount("tax_withheld", "Tax Withheld"),
            ReportColumn.text("receipts", "ORs"),
            ReportColumn.text("stage", "Status"),
            ReportColumn.count("submitted_count", "Submissions"),
            ReportColumn.date("decided", "Decided"),
            ReportColumn.text("decided_by", "Decided By"),
            ReportColumn.text("reject_reason", "Reason")));
  }

  private static ReportMetadata metadata(String code, String title, String description) {
    return ReportMetadata.operations(
        code,
        title,
        description,
        List.of(
            ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("YEAR_START"),
            ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"),
            ParameterSpec.optional(INSURER, "Insurer Code", ParameterType.TEXT)));
  }

  /**
   * A report of one SQL statement grouped by one column.
   *
   * @param jdbc report SQL
   * @param metadata metadata
   * @param sql constant SQL
   * @param groupKey grouping column
   * @param groupLabel grouping label
   * @param columns other columns
   */
  private record Sql(
      NbReportJdbc jdbc,
      ReportMetadata metadata,
      String sql,
      String groupKey,
      String groupLabel,
      List<ReportColumn> columns)
      implements ReportDefinition {

    @Override
    public ReportResult generate(ReportParameters p) {
      Map<String, Object> args = new HashMap<>();
      args.put(COMPANY, p.longValue(COMPANY));
      args.put(FROM, p.date(FROM));
      args.put(TO, p.date(TO));
      args.put(
          INSURER,
          p.optionalText(INSURER)
              .filter(v -> !v.isBlank())
              .map(v -> v.strip().toUpperCase(Locale.ROOT))
              .orElse(null));
      return TabularReportBuilder.of(p)
          .columns(columns)
          .groupBy(groupKey, groupLabel)
          .rows(jdbc.rows(sql, args))
          .presorted()
          .build();
    }
  }
}
