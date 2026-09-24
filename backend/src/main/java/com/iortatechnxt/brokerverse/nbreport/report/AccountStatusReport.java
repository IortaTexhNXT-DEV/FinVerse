package com.iortatechnxt.brokerverse.nbreport.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.nbreport.service.StallRule;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Account Status Report (NB-ACC-STATUS, BRNB.075 / 115): the current stage of every account, how
 * long it has been there, the stage SLA and whether it is breached, and whether the account is
 * stalled (no stage movement for {@code NB_STALLED_DAYS} days). Grouped by stage in workflow order;
 * the exceptions filter keeps only SLA breaches or stalled accounts.
 */
@Component
public class AccountStatusReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-ACC-STATUS";

  private static final String STATUS = "status";
  private static final String OFFICER = "officer";
  private static final String EXCEPTIONS = "exceptions";
  private static final String BREACH = "SLA_BREACH";
  private static final String STALLED = "STALLED";
  private static final String STALLED_KEY = "stalled";

  private static final String SQL =
      "select a.arn, a.client_name as client, a.product_code as product,"
          + " a.account_officer as officer, s.sort_order, s.name as stage,"
          + " c.stage_entered_at as since, c.due_at as due, c.assignee,"
          + " round(cast(extract(epoch from (cast(:now as timestamptz) - c.stage_entered_at))"
          + " / 86400 as numeric), 1) as age,"
          + " (not s.terminal and s.owner_permission is not null and c.due_at < :now) as breached,"
          + " (not s.terminal and s.owner_permission is not null"
          + " and c.stage_entered_at < :stalledBefore) as stalled"
          + " from acc_account a"
          + " join wf_case c on c.entity_type = 'Account' and c.entity_id = cast(a.id as varchar)"
          + " join wf_stage s on s.workflow_code = c.workflow_code and s.stage_code = c.stage_code"
          + " where a.company_id = :company and a.status <> 'VOIDED'"
          + " and (cast(:status as varchar) is null or a.status = :status)"
          + " and (cast(:officer as varchar) is null"
          + " or lower(a.account_officer) = lower(cast(:officer as varchar)))"
          + " and (cast(:exceptions as varchar) is null"
          + " or (cast(:exceptions as varchar) = 'SLA_BREACH'"
          + " and not s.terminal and s.owner_permission is not null"
          + " and c.due_at < :now)"
          + " or (cast(:exceptions as varchar) = 'STALLED'"
          + " and not s.terminal and s.owner_permission is not null"
          + " and c.stage_entered_at < :stalledBefore))"
          + " order by s.sort_order, c.stage_entered_at, a.arn";

  private static final List<String> STATUSES =
      List.of(
          NbReportSupport.ALL,
          "DRAFT",
          "SUBMITTED",
          "RETURNED_TO_MARKETING",
          "AWAITING_PAYMENT",
          "READY_FOR_PLACEMENT",
          "PLACED",
          "RETURNED_BY_INSURER",
          "PLACEMENT_CANCELLED",
          "POLICY_ISSUED",
          "BOOKED",
          "CANCELLED");

  private final NbReportJdbc jdbc;
  private final StallRule stall;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param stall stalled-account threshold
   */
  public AccountStatusReport(NbReportJdbc jdbc, StallRule stall) {
    this.jdbc = jdbc;
    this.stall = stall;
  }

  @Override
  public ReportMetadata metadata() {
    return NbReportSupport.metadata(
        CODE,
        "Account Status Report",
        "Current status per account with stage age, SLA breach and stalled flag (BRNB.075/115)",
        Permission.ACCOUNT_VIEW,
        false,
        ParameterSpec.select(STATUS, "Status", STATUSES, NbReportSupport.ALL),
        ParameterSpec.select(
            EXCEPTIONS, "Exceptions", List.of(NbReportSupport.ALL, BREACH, STALLED), "ALL"),
        ParameterSpec.optional(OFFICER, "Account Officer", ParameterType.TEXT));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var args =
        NbReportSupport.args(p)
            .with(STATUS, NbReportSupport.selected(p, STATUS))
            .with(EXCEPTIONS, NbReportSupport.selected(p, EXCEPTIONS))
            .with(OFFICER, p.optionalText(OFFICER).map(String::strip).orElse(null))
            .with("now", stall.now())
            .with("stalledBefore", stall.stalledBefore());
    List<Map<String, Object>> rows =
        jdbc.rows(SQL, args.map()).stream()
            .map(r -> NbReportSupport.flag(NbReportSupport.flag(r, "breached"), STALLED_KEY))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("arn", "ARN"),
            ReportColumn.text("client", "Client"),
            ReportColumn.text("product", "Product"),
            ReportColumn.text(OFFICER, "Officer"),
            ReportColumn.date("since", "In Stage Since"),
            ReportColumn.amountNoTotal("age", "Age (days)"),
            ReportColumn.date("due", "SLA Due"),
            ReportColumn.text("breached", "SLA Breached"),
            ReportColumn.text(STALLED_KEY, "Stalled"),
            ReportColumn.text("assignee", "Assignee"))
        .groupBy("stage", "Stage")
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .note(
            "Stalled: no stage movement for "
                + stall.days()
                + " days or more (parameter NB_STALLED_DAYS). SLA per stage from the workflow.")
        .build();
  }
}
