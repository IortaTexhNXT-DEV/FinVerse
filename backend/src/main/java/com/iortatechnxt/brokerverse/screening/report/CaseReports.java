package com.iortatechnxt.brokerverse.screening.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The case reports of the Compliance category (SNSRP-901, 903, 405; spec section 7): case status
 * monitoring, SLA reminders and breaches, and the screening audit log. Periods are Philippine days.
 */
public final class CaseReports {

  /** Case status monitoring. */
  public static final String CASE_STATUS = "SCR-CASE-STATUS";

  /** SLA reminders and breaches. */
  public static final String SLA_BREACHES = "SCR-SLA-BREACHES";

  /** Screening audit log. */
  public static final String AUDIT_LOG = "SCR-AUDIT-LOG";

  private static final String PERIOD = " between :fromDate and :toDate";
  private static final String CASE_NO = "case_no";
  private static final String STAGE = "stage";
  private static final String ASSIGNEE = "assignee";
  private static final String REMARKS = "remarks";

  private CaseReports() {}

  /** Case Status Monitoring (p.8 "Status Monitoring reports"; SNSRP-901). */
  @Component
  public static class CaseStatus implements ReportDefinition {

    private static final String SQL =
        "select k.case_no, k.client_name || ' (' || k.client_code || ')' as client,"
            + " k.case_type || coalesce(' / ' || k.risk_category, '') as case_type, k.stage,"
            + " k.assignee, cast(k.created_at"
            + ScrReportSql.DAY
            + " as created,"
            + " cast(extract(day from (cast(:now as timestamptz) - k.stage_entered_at)) as integer)"
            + " as days_in_stage, k.due_at,"
            + " case when k.status = 'CLOSED' or k.due_at is null then ''"
            + " when k.breached or k.due_at <= cast(:now as timestamptz) then 'Breached'"
            + " when k.remind_at <= cast(:now as timestamptz) then 'Due soon' else 'On time' end"
            + " as sla_state, k.disposition,"
            + " coalesce(k.marketing_unit, '') || coalesce(' / ' || k.unit_head, '') as unit"
            + " from scr_case k where k.company_id = :companyId"
            + " and cast(k.created_at"
            + ScrReportSql.DAY
            + PERIOD
            + ScrReportSql.CASE_FILTERS
            + " and (cast(:caseType as varchar) is null or k.case_type = :caseType)"
            + " order by k.stage, k.case_no";

    private final ScrReportSql sql;
    private final Clock clock;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     * @param clock clock (SLA state)
     */
    public CaseStatus(ScrReportSql sql, Clock clock) {
      this.sql = sql;
      this.clock = clock;
    }

    @Override
    public ReportMetadata metadata() {
      List<ParameterSpec> params = new ArrayList<>();
      params.add(ScrReportSql.company());
      params.addAll(ScrReportSql.period());
      params.addAll(ScrReportSql.caseFilters());
      params.add(ParameterSpec.optional("caseType", "Case Type", ParameterType.TEXT));
      return ReportMetadata.compliance(
          CASE_STATUS,
          "Case Status Monitoring",
          "Screening cases created in the period by stage, assignee, age and SLA state (SNSRP-901)",
          params);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      Map<String, Object> args = ScrReportSql.args(p);
      args.put("caseType", ScrReportSql.text(p, "caseType"));
      args.put("now", Timestamp.from(clock.instant()));
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text(CASE_NO, "Case No."),
              ReportColumn.text("client", "Client"),
              ReportColumn.text("case_type", "Case Type / Risk Category"),
              ReportColumn.text(STAGE, "Stage"),
              ReportColumn.text(ASSIGNEE, "Assignee"),
              ReportColumn.date("created", "Created"),
              ScrReportSql.number("days_in_stage", "Days in Stage"),
              ReportColumn.text("due_at", "Due"),
              ReportColumn.text("sla_state", "SLA State"),
              ReportColumn.text("disposition", "Disposition"),
              ReportColumn.text("unit", "Marketing Unit / Unit Head"))
          .groupBy(STAGE, "Stage")
          .rows(sql.rows(SQL, args))
          .presorted()
          .withoutGrandTotal()
          .note(ScrReportSql.LAYOUT_NOTE)
          .build();
    }
  }

  /** SLA Reminders and Breaches (SNSRP-405, 108). */
  @Component
  public static class SlaBreaches implements ReportDefinition {

    private static final String SQL =
        "select e.occurred_at, e.event, k.case_no, e.from_stage as stage, k.assignee,"
            + " e.remarks, e.to_value as notified"
            + " from scr_case_event e join scr_case k on k.id = e.case_id"
            + " where k.company_id = :companyId"
            + " and e.event in ('REMINDER', 'BREACH', 'DOCUMENT_REMINDER')"
            + " and cast(e.occurred_at"
            + ScrReportSql.DAY
            + PERIOD
            + " and (cast(:stage as varchar) is null or e.from_stage = :stage)"
            + " and (cast(:team as varchar) is null or k.team_code = :team"
            + " or k.marketing_unit = :team)"
            + " order by e.occurred_at, e.id";

    private final ScrReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public SlaBreaches(ScrReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      List<ParameterSpec> params = new ArrayList<>();
      params.add(ScrReportSql.company());
      params.addAll(ScrReportSql.period());
      params.add(ParameterSpec.optional(STAGE, "Stage", ParameterType.TEXT));
      params.add(ParameterSpec.optional("team", "Team / Marketing Unit", ParameterType.TEXT));
      return ReportMetadata.compliance(
          SLA_BREACHES,
          "SLA Reminders and Breaches",
          "Reminders, breaches and escalations of screening cases in the period (SNSRP-405)",
          params);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      Map<String, Object> args = ScrReportSql.args(p);
      args.put(STAGE, ScrReportSql.text(p, STAGE));
      args.put("team", ScrReportSql.text(p, "team"));
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("occurred_at", "Date / Time"),
              ReportColumn.text("event", "Event"),
              ReportColumn.text(CASE_NO, "Case No."),
              ReportColumn.text(STAGE, "Stage"),
              ReportColumn.text(ASSIGNEE, "Assignee"),
              ReportColumn.text(REMARKS, "SLA / Due"),
              ReportColumn.text("notified", "Notified / Escalated To"))
          .rows(sql.rows(SQL, args))
          .presorted()
          .withoutGrandTotal()
          .note(ScrReportSql.LAYOUT_NOTE)
          .build();
    }
  }

  /**
   * Screening Audit Log (SNSRP-902, 903): case timeline events with the case filters, and, without
   * a case filter, the audit trail of configuration versions, watchlist entries and runs, matches,
   * screening runs and STR extractions. Viewed and exported with SCR_AUDIT_VIEW.
   */
  @Component
  public static class AuditLog implements ReportDefinition {

    private static final String SQL =
        "select * from ("
            + " select e.occurred_at, 'Case' as area, k.case_no as reference, e.event,"
            + " concat_ws(' ', e.from_stage, e.from_value) as from_value,"
            + " concat_ws(' ', e.to_stage, e.to_value) as to_value,"
            + " concat_ws(': ', e.reason_code, e.remarks) as remarks, e.actor, e.id as seq"
            + " from scr_case_event e join scr_case k on k.id = e.case_id"
            + " where k.company_id = :companyId"
            + " and cast(e.occurred_at"
            + ScrReportSql.DAY
            + PERIOD
            + ScrReportSql.CASE_FILTERS
            + " union all"
            + " select a.occurred_at, case a.entity_type when 'ScreeningConfigVersion' then 'Configuration'"
            + " when 'ScreeningRun' then 'Screening run' when 'ScreeningMatch' then 'Match'"
            + " when 'ScreeningStrExtraction' then 'STR extraction' else 'Watchlist' end,"
            + " a.entity_id, a.action, null, null, a.summary, a.username, a.id"
            + " from audit_log a"
            + " where a.entity_type in ('ScreeningConfigVersion', 'WatchlistEntry',"
            + " 'WatchlistIngestionRun', 'ScreeningListSource', 'ScreeningMatch', 'ScreeningRun',"
            + " 'ScreeningStrExtraction')"
            + " and cast(a.occurred_at"
            + ScrReportSql.DAY
            + PERIOD
            + " and cast(:marketingUnit as varchar) is null and cast(:unitHead as varchar) is null"
            + " and cast(:disposition as varchar) is null and cast(:caseStatus as varchar) is null"
            + ") r where (cast(:user as varchar) is null or lower(r.actor) = lower(:user))"
            + " order by r.occurred_at, r.seq";

    private final ScrReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public AuditLog(ScrReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      List<ParameterSpec> params = new ArrayList<>();
      params.add(ScrReportSql.company());
      params.addAll(ScrReportSql.period());
      params.addAll(ScrReportSql.caseFilters());
      params.add(ParameterSpec.optional("user", "User", ParameterType.TEXT));
      return new ReportMetadata(
          AUDIT_LOG,
          "Screening Audit Log",
          ReportCategory.COMPLIANCE,
          "Case, configuration, watchlist and screening events with from / to values (SNSRP-903)",
          params,
          Permission.SCR_AUDIT_VIEW,
          Permission.SCR_AUDIT_VIEW,
          true);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      Map<String, Object> args = ScrReportSql.args(p);
      args.put("user", ScrReportSql.text(p, "user"));
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("occurred_at", "Date / Time"),
              ReportColumn.text("area", "Area"),
              ReportColumn.text("reference", "Reference"),
              ReportColumn.text("event", "Event"),
              ReportColumn.text("from_value", "From"),
              ReportColumn.text("to_value", "To"),
              ReportColumn.text(REMARKS, "Reason / Remarks"),
              ReportColumn.text("actor", "User"))
          .rows(sql.rows(SQL, args))
          .presorted()
          .withoutGrandTotal()
          .note(ScrReportSql.LAYOUT_NOTE)
          .build();
    }
  }
}
