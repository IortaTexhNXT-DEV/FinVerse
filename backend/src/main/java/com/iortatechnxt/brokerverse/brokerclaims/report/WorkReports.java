package com.iortatechnxt.brokerverse.brokerclaims.report;

import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The work registers of Claims Handling: Pending Actions per handler (BRCLM.034, FR-CM-061) and the
 * Insurer Claim Numbers register (BRCLM.043 AC5, FR-CM-066).
 */
public final class WorkReports {

  /** Pending Actions. */
  public static final String PENDING = "BCL-PENDING-ACTIONS";

  /** Insurer Claim Numbers. */
  public static final String INSURER_CLAIMS = "BCL-INSURER-CLAIMS";

  private static final String STATUS_JOIN =
      " left join lov_value s on s.type_code = 'BCL_CLAIM_STATUS' and s.code = c.status_code";

  private static final String PENDING_SQL =
      "select c.handler as person, 'Follow-up' as kind, c.claim_no, c.assured_name,"
          + " coalesce(s.label, c.status_code) as status, c.next_follow_up_date as due_date,"
          + " (cast(:asOf as date) - c.next_follow_up_date) as days_overdue,"
          + " c.next_action_plan as action from bcl_claim c"
          + STATUS_JOIN
          + " where c.company_id = :companyId and c.phase <> 'CLOSED'"
          + " and c.next_follow_up_date <= :asOf"
          + BclReportSql.FILTERS
          + " union all"
          + " select coalesce(d.assignee, d.created_by), 'Diary: ' || coalesce(t.label, d.entry_type),"
          + " c.claim_no, c.assured_name, coalesce(s.label, c.status_code), d.due_date,"
          + " (cast(:asOf as date) - d.due_date), d.text"
          + " from bcl_diary_entry d join bcl_claim c on c.id = d.claim_id"
          + STATUS_JOIN
          + " left join lov_value t on t.type_code = 'BCL_DIARY_TYPE' and t.code = d.entry_type"
          + " where c.company_id = :companyId and d.done_at is null and d.due_date <= :asOf"
          + BclReportSql.FILTERS
          + " order by 1, 6, 3";

  private static final String INSURER_CLAIMS_SQL =
      "select c.claim_no, c.assured_name, c.arn, c.policy_no, ic.insurer_code,"
          + " coalesce((select i.name from cat_insurer i where i.company_id = c.company_id"
          + " and i.party_code = ic.insurer_code limit 1), ic.insurer_code) as insurer_name,"
          + " ic.share_pct, ic.insurer_claim_no, ic.reported_to_insurer_on, c.currency,"
          + " ic.reserve_amount, ic.settled_amount,"
          + " coalesce(a.label, ic.adjuster_code) as adjuster, coalesce(s.label, c.status_code)"
          + " as status, c.reported_date from bcl_insurer_claim ic"
          + " join bcl_claim c on c.id = ic.claim_id"
          + STATUS_JOIN
          + " left join lov_value a on a.type_code = 'BCL_ADJUSTER' and a.code = ic.adjuster_code"
          + " where c.company_id = :companyId"
          + " and (cast(:reportedFrom as date) is null or c.reported_date >= :reportedFrom)"
          + " and (cast(:reportedTo as date) is null or c.reported_date <= :reportedTo)"
          + BclReportSql.FILTERS
          + " order by c.claim_no, ic.id";

  private WorkReports() {}

  /** Pending Actions (BRCLM.034). */
  @Component
  public static class PendingActions implements ReportDefinition {

    private final BclReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public PendingActions(BclReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ReportMetadata.claimsHandling(
          PENDING,
          "Pending Actions",
          "Follow-ups due or overdue and open diary entries due, per handler (BRCLM.034)",
          BclReportSql.asOfFilters());
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      sql.asOf(p);
      Map<String, Object> args = BclReportSql.args(p);
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("kind", "Pending Action"),
              ReportColumn.text("claim_no", "Claim Number"),
              ReportColumn.text("assured_name", "Assured's Name"),
              ReportColumn.text("status", "Claim Status"),
              ReportColumn.date("due_date", "Due Date"),
              new ReportColumn("days_overdue", "Days Overdue", ColumnType.NUMBER, false),
              ReportColumn.text("action", "Action / Remarks"))
          .groupBy("person", "Handler")
          .rows(sql.rows(PENDING_SQL, args))
          .presorted()
          .build();
    }
  }

  /** Insurer Claim Numbers (BRCLM.043). */
  @Component
  public static class InsurerClaims implements ReportDefinition {

    private final BclReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public InsurerClaims(BclReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ReportMetadata.claimsHandling(
          INSURER_CLAIMS,
          "Insurer Claim Numbers",
          "Every insurer claim number under its BDOI claim with share, reserve and settled amount"
              + " (BRCLM.043)",
          BclReportSql.rangeFilters("reportedFrom", "reportedTo", "Date reported", false));
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("insurer_name", "Insurer"),
              ReportColumn.text("insurer_claim_no", "Insurer Claim No."),
              ReportColumn.percent("share_pct", "Share"),
              ReportColumn.date("reported_to_insurer_on", "Reported to Insurer"),
              ReportColumn.text("currency", "Currency"),
              ReportColumn.amount("reserve_amount", "Insurer Reserve"),
              ReportColumn.amount("settled_amount", "Settled Amount"),
              ReportColumn.text("adjuster", "Adjuster"),
              ReportColumn.text("assured_name", "Assured's Name"),
              ReportColumn.text("policy_no", "Policy No."),
              ReportColumn.text("status", "Claim Status"))
          .groupBy("claim_no", "BDOI Claim")
          .rows(sql.rows(INSURER_CLAIMS_SQL, BclReportSql.args(p)))
          .presorted()
          .build();
    }
  }
}
