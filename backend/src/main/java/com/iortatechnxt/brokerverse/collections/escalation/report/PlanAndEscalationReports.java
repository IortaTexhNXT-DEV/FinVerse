package com.iortatechnxt.brokerverse.collections.escalation.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Collections reports of the plans and escalations (COLLECTIONS_DESIGN section 11): escalations
 * raised in a period (BRCLXN.049/050), promises broken in a period (BRCLXN.055) and installments
 * due or overdue (BRCLXN.053).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of the reports
public final class PlanAndEscalationReports {

  private static final String ARN = "arn";
  private static final String ARN_LABEL = "ARN";
  private static final String ASSURED = "assured_name";
  private static final String ASSURED_LABEL = "Name of Assured";
  private static final String STATUS = "status";
  private static final String STATUS_LABEL = "Status";
  private static final String ALL = "ALL";

  private PlanAndEscalationReports() {}

  /** CLX-ESCALATIONS (BRCLXN.049/050). */
  @Component
  public static class Escalations implements ReportDefinition {

    private static final String SQL =
        "select e.escalation_no, e.kind, e.rule_code, e.arn, e.assured_name, e.target_level,"
            + " e.target_username, e.reason_code, e.status, e.created_at as raised,"
            + " e.created_by as raised_by, e.total_balance, e.resolved_at,"
            + " (select string_agg(i.invoice_no, ', ' order by i.invoice_no)"
            + " from clx_escalation_item i where i.escalation_id = e.id) as invoices"
            + " from clx_escalation e where e.company_id = :companyId"
            + " and cast(e.created_at at time zone 'Asia/Manila' as date) between :from and :to"
            + " and (cast(:scope as varchar) is null"
            + " or (cast(:scope as varchar) = 'OPEN' and e.status <> 'RESOLVED')"
            + " or (cast(:scope as varchar) = 'RESOLVED' and e.status = 'RESOLVED'))"
            + " order by e.status, e.id";

    private final PlanReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Escalations(PlanReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return PlanReportSql.metadata(
          "CLX-ESCALATIONS",
          "Escalated Accounts",
          "Escalations raised in a period with their target, reason and stage (BRCLXN.049/050)",
          "MONTH_START",
          ParameterSpec.select(STATUS, "Escalations", List.of(ALL, "OPEN", "RESOLVED"), ALL));
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      Map<String, Object> args = PlanReportSql.args(p);
      args.put("scope", p.optionalText(STATUS).filter(v -> !ALL.equals(v)).orElse(null));
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("escalation_no", "Escalation No."),
              ReportColumn.date("raised", "Raised On"),
              ReportColumn.text("kind", "Kind"),
              ReportColumn.text("rule_code", "Rule"),
              ReportColumn.text(ARN, ARN_LABEL),
              ReportColumn.text(ASSURED, ASSURED_LABEL),
              ReportColumn.text("invoices", "Invoices"),
              ReportColumn.amount("total_balance", "Outstanding"),
              ReportColumn.text("target_level", "Level"),
              ReportColumn.text("target_username", "Target"),
              ReportColumn.text("reason_code", "Reason"),
              ReportColumn.text("raised_by", "Raised By"),
              ReportColumn.date("resolved_at", "Resolved On"))
          .groupBy(STATUS, STATUS_LABEL)
          .rows(sql.rows(SQL, args))
          .presorted()
          .note(PlanReportSql.DRAFT_NOTE)
          .build();
    }
  }

  /** CLX-BROKEN-PROMISES (BRCLXN.055). */
  @Component
  public static class BrokenPromises implements ReportDefinition {

    private static final String SQL =
        "select p.invoice_no, p.arn, p.assured_name, p.promised_on, p.promised_date,"
            + " p.promised_amount, coalesce(p.actual_paid, 0) as actual_paid, p.status,"
            + " p.created_by as recorded_by, p.evaluated_at"
            + " from clx_promise p where p.company_id = :companyId"
            + " and p.status in ('BROKEN', 'PARTIALLY_KEPT')"
            + " and p.promised_date between :from and :to"
            + " order by p.status, p.promised_date, p.id";

    private final PlanReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public BrokenPromises(PlanReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return PlanReportSql.metadata(
          "CLX-BROKEN-PROMISES",
          "Broken Promises to Pay",
          "Promises to pay broken or partially kept, by promised date (BRCLXN.055, CQ16)",
          "MONTH_START");
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("invoice_no", "Invoice No."),
              ReportColumn.text(ARN, ARN_LABEL),
              ReportColumn.text(ASSURED, ASSURED_LABEL),
              ReportColumn.date("promised_on", "Promised On"),
              ReportColumn.date("promised_date", "Promised Date"),
              ReportColumn.amount("promised_amount", "Promised Amount"),
              ReportColumn.amount("actual_paid", "Paid in Time"),
              ReportColumn.text("recorded_by", "Recorded By"),
              ReportColumn.date("evaluated_at", "Checked On"))
          .groupBy(STATUS, STATUS_LABEL)
          .rows(sql.rows(SQL, PlanReportSql.args(p)))
          .presorted()
          .note(PlanReportSql.DRAFT_NOTE)
          .build();
    }
  }

  /** CLX-INSTALLMENTS-DUE (BRCLXN.053). */
  @Component
  public static class InstallmentsDue implements ReportDefinition {

    private static final String SQL =
        "select p.plan_no, p.arn, p.assured_name, p.frequency, cast(i.seq as varchar) as seq,"
            + " cast(i.policy_year as varchar) as policy_year,"
            + " i.invoice_no, i.due_date, i.amount, i.paid_amount, i.amount - i.paid_amount"
            + " as balance, i.status,"
            + " cast(greatest(cast(:to as date) - i.due_date, 0) as varchar) as days_overdue"
            + " from clx_installment i join clx_installment_plan p on p.id = i.plan_id"
            + " where p.company_id = :companyId and p.status = 'ACTIVE' and i.status <> 'PAID'"
            + " and i.due_date between :from and :to"
            + " order by i.status, i.due_date, p.plan_no, i.seq";

    private final PlanReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public InstallmentsDue(PlanReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return PlanReportSql.metadata(
          "CLX-INSTALLMENTS-DUE",
          "Installments Due and Overdue",
          "Unpaid installments of the live plans falling due in a period (BRCLXN.053)",
          "YEAR_START");
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("plan_no", "Plan No."),
              ReportColumn.text(ARN, ARN_LABEL),
              ReportColumn.text(ASSURED, ASSURED_LABEL),
              ReportColumn.text("frequency", "Frequency"),
              ReportColumn.text("seq", "Installment"),
              ReportColumn.text("policy_year", "Policy Year"),
              ReportColumn.text("invoice_no", "Invoice No."),
              ReportColumn.date("due_date", "Due Date"),
              ReportColumn.amount("amount", "Amount"),
              ReportColumn.amount("paid_amount", "Paid"),
              ReportColumn.amount("balance", "Balance"),
              ReportColumn.text("days_overdue", "Days Overdue"))
          .groupBy(STATUS, STATUS_LABEL)
          .rows(sql.rows(SQL, PlanReportSql.args(p)))
          .presorted()
          .note(PlanReportSql.DRAFT_NOTE)
          .build();
    }
  }
}
