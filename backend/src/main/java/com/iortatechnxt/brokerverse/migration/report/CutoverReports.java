package com.iortatechnxt.brokerverse.migration.report;

import static com.iortatechnxt.brokerverse.migration.report.MigReport.PHT;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The cutover, run-off and legacy archive reports of the Data Migration (DATA_MIGRATION_DESIGN
 * sections 16, 17 and 21): runbook progress, the go / no-go criteria and decisions, the run-off of
 * the legacy in-force headers, the open legacy positions and the legacy archive access log.
 */
@Configuration(proxyBeanMethods = false)
public class CutoverReports {

  private static final String KIND = "kind";
  private static final String KIND_LABEL = "Kind";
  private static final String PLAN_NO = "plan_no";
  private static final String PLAN = "Plan";
  private static final String STATUS = "status";
  private static final String STATUS_LABEL = "Status";
  private static final String CURRENCY = "currency";
  private static final String FROM = "from";
  private static final String TO = "to";

  /**
   * {@code MIG-CUTOVER-STATUS}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migCutoverStatusReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-CUTOVER-STATUS",
            "Cutover Runbook Status",
            "Tasks of each mock run, dress rehearsal and production cut-over with their planned and"
                + " actual times")
        .sql(
            "select p.plan_no, p.kind, p.go_live_date, t.seq, t.phase, t.task, t.owner_role,"
                + " t.object_code, t.depends_on, to_char(t.planned_start, 'YYYY-MM-DD HH24:MI')"
                + " as planned_start, to_char(t.planned_end, 'YYYY-MM-DD HH24:MI') as planned_end,"
                + " to_char(t.actual_start, 'YYYY-MM-DD HH24:MI') as actual_start,"
                + " to_char(t.actual_end, 'YYYY-MM-DD HH24:MI') as actual_end, t.status, t.remarks"
                + " from mig_cutover_plan p join mig_cutover_task t on t.plan_id = p.id"
                + " where p.company_id = :company order by p.go_live_date, p.id, t.seq")
        .columns(
            ReportColumn.text(PLAN_NO, PLAN),
            ReportColumn.text(KIND, KIND_LABEL),
            ReportColumn.date("go_live_date", "Go-live"),
            ReportColumn.count("seq", "No."),
            ReportColumn.text("phase", "Phase"),
            ReportColumn.text("task", "Task"),
            ReportColumn.text("owner_role", "Owner"),
            ReportColumn.text("object_code", "Object"),
            ReportColumn.text("depends_on", "After"),
            ReportColumn.text("planned_start", "Planned Start"),
            ReportColumn.text("planned_end", "Planned End"),
            ReportColumn.text("actual_start", "Actual Start"),
            ReportColumn.text("actual_end", "Actual End"),
            ReportColumn.text(STATUS, STATUS_LABEL),
            ReportColumn.text("remarks", "Remarks"))
        .groupBy(PLAN_NO, PLAN)
        .row(MigReport.relabel(KIND, STATUS))
        .build();
  }

  /**
   * {@code MIG-GONOGO}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migGonogoReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-GONOGO",
            "Go / No-Go Criteria and Decisions",
            "The twelve criteria of each plan as measured or recorded, and the decision of the"
                + " go / no-go board")
        .sql(
            "select p.plan_no, c.criterion_no, c.name, c.threshold,"
                + " case when c.measure = 'MANUAL' then 'Recorded' else 'Measured' end as kind,"
                + " c.measured_value, case when c.met then 'Met' when not c.met then 'Not met'"
                + " else 'Open' end as result, c.manual_note, c.measured_by, to_char(c.measured_at"
                + PHT
                + " as measured_at, d.decision, d.decided_by, to_char(d.decided_at"
                + PHT
                + " as decided_at, d.comment"
                + " from mig_cutover_plan p join mig_gonogo_criterion c on c.plan_id = p.id"
                + " left join mig_gonogo_decision d on d.id = (select max(x.id)"
                + " from mig_gonogo_decision x where x.plan_id = p.id)"
                + " where p.company_id = :company order by p.go_live_date, p.id, c.criterion_no")
        .columns(
            ReportColumn.text(PLAN_NO, PLAN),
            ReportColumn.count("criterion_no", "No."),
            ReportColumn.text("name", "Criterion"),
            ReportColumn.text("threshold", "Threshold"),
            ReportColumn.text(KIND, KIND_LABEL),
            ReportColumn.text("measured_value", "Measured"),
            ReportColumn.text("result", "Result"),
            ReportColumn.text("manual_note", "Evidence"),
            ReportColumn.text("measured_by", "By"),
            ReportColumn.text("measured_at", "At"),
            ReportColumn.text("decision", "Decision"),
            ReportColumn.text("decided_by", "Decided by"),
            ReportColumn.text("decided_at", "Decided at"),
            ReportColumn.text("comment", "Comment"))
        .groupBy(PLAN_NO, PLAN)
        .build();
  }

  /**
   * {@code MIG-RUNOFF}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migRunoffReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-RUNOFF",
            "Legacy Run-off",
            "Legacy in-force headers by expiry month and source system at each monthly snapshot:"
                + " renewed, not renewed, lapsed and still open")
        .sql(
            "select r.snapshot_date, to_char(r.expiry_month, 'YYYY-MM') as expiry_month,"
                + " r.source_system, r.headers_in_force, r.premium_in_force, r.renewed,"
                + " r.not_renewed, r.lapsed, r.still_open"
                + " from mig_runoff_cohort r where r.company_id = :company"
                + " and (cast(:from as date) is null or r.snapshot_date >= :from)"
                + " and (cast(:to as date) is null or r.snapshot_date <= :to)"
                + " order by r.snapshot_date desc, r.expiry_month, r.source_system")
        .parameter(ParameterSpec.optional(FROM, "Snapshot from", ParameterType.DATE))
        .parameter(ParameterSpec.optional(TO, "Snapshot to", ParameterType.DATE))
        .columns(
            ReportColumn.date("snapshot_date", "Snapshot"),
            ReportColumn.text("expiry_month", "Expiry Month"),
            ReportColumn.text("source_system", "System"),
            ReportColumn.count("headers_in_force", "In Force"),
            ReportColumn.amount("premium_in_force", "Premium in Force"),
            ReportColumn.count("renewed", "Renewed"),
            ReportColumn.count("not_renewed", "Not Renewed"),
            ReportColumn.count("lapsed", "Lapsed"),
            ReportColumn.count("still_open", "Still Open"))
        .build();
  }

  /**
   * {@code MIG-LEGACY-POSITIONS}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migLegacyPositionsReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-LEGACY-POSITIONS",
            "Open Legacy Positions",
            "Legacy invoices and legacy unapplied payments still open in BIBS, with their legacy"
                + " numbers and age; the legacy context closes when this report is empty")
        .sql(
            "select 'Legacy invoice' as kind, i.invoice_no as reference, i.legacy_invoice_no"
                + " as legacy_no, i.source_system, i.client_code, i.assured_name as name,"
                + " i.currency, sum(c.balance) as balance, i.booking_date as since,"
                + " i.migration_batch"
                + " from ops_invoice i join ops_invoice_component c on c.invoice_id = i.id"
                + " where i.company_id = :company and i.ledger_context = 'LEGACY'"
                + " and not i.cancelled and c.component not in ('DTIP', 'COMMISSION',"
                + " 'COMMISSION_VAT', 'WTAX')"
                + " group by i.id having sum(c.balance) <> 0"
                + " union all "
                + "select 'Legacy unapplied payment', u.reference, u.legacy_ar_no, u.source_system,"
                + " u.client_code, u.payor_name, u.currency, u.balance, u.legacy_ar_date,"
                + " u.migration_batch from csh_unapplied u"
                + " where u.company_id = :company and u.ledger_context = 'LEGACY' and u.balance <> 0"
                + " order by 1, 9, 2")
        .columns(
            ReportColumn.text(KIND, KIND_LABEL),
            ReportColumn.text("reference", "BIBS Reference"),
            ReportColumn.text("legacy_no", "Legacy No."),
            ReportColumn.text("source_system", "System"),
            ReportColumn.text("client_code", "Client"),
            ReportColumn.text("name", "Name"),
            ReportColumn.text(CURRENCY, "Currency"),
            ReportColumn.amount("balance", "Open Balance"),
            ReportColumn.date("since", "Booked / Received"),
            ReportColumn.text("migration_batch", "Batch"))
        .groupBy(KIND, KIND_LABEL)
        .build();
  }

  /**
   * {@code MIG-ACCESS-LOG} (Compliance).
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migAccessLogReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-ACCESS-LOG",
            "Legacy Archive Access Log",
            "Every search, view, download and export of the legacy archive with the user, source"
                + " address, criteria, records and reason")
        .sql(
            "select to_char(a.accessed_at"
                + PHT
                + " as accessed_at, a.username, a.source_address, a.action, a.criteria,"
                + " a.record_keys, a.result_count, a.reason_code, a.reason_text"
                + " from mig_access_log a where a.company_id = :company"
                + " and (cast(:from as date) is null"
                + " or a.accessed_at >= cast(:from as date) at time zone 'Asia/Manila')"
                + " and (cast(:to as date) is null"
                + " or a.accessed_at < (cast(:to as date) + 1) at time zone 'Asia/Manila')"
                + " order by a.accessed_at desc, a.id desc")
        .parameter(ParameterSpec.optional(FROM, "Accessed from", ParameterType.DATE))
        .parameter(ParameterSpec.optional(TO, "Accessed to", ParameterType.DATE))
        .permission(Permission.LEGACY_ACCESS_LOG_VIEW)
        .columns(
            ReportColumn.text("accessed_at", "When"),
            ReportColumn.text("username", "User"),
            ReportColumn.text("source_address", "Address"),
            ReportColumn.text("action", "Action"),
            ReportColumn.text("criteria", "Criteria"),
            ReportColumn.text("record_keys", "Records"),
            ReportColumn.count("result_count", "Count"),
            ReportColumn.text("reason_code", "Reason"),
            ReportColumn.text("reason_text", "Details"))
        .build();
  }
}
