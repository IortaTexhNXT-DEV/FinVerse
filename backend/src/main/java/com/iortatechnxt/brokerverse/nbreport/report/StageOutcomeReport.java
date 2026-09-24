package com.iortatechnxt.brokerverse.nbreport.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Successful and Fall-out Accounts per Stage (NB-STAGE-OUTCOME, BRNB.075). For each stage of the
 * account workflow in the period: accounts that entered it, moved on successfully, were returned
 * (by Processing or the insurer), were voided or had their placement cancelled, and those still in
 * it today. A second section counts the bulk upload rows per handler: committed, rejected, and the
 * rejected rows that were duplicate fall-outs (BRNB.032 / 051 / 066).
 */
@Component
public class StageOutcomeReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-STAGE-OUTCOME";

  private static final String SECTION = "section";

  private static final String STAGES =
      "select 'Account workflow stages' as section, s.sort_order, s.name as stage,"
          + " count(*) filter (where h.to_stage = s.stage_code) as entered,"
          + " count(*) filter (where h.from_stage = s.stage_code and h.action not in"
          + " ('return', 'insurer_return', 'void', 'cancel_placement', 'cancel')) as successful,"
          + " count(*) filter (where h.from_stage = s.stage_code"
          + " and h.action in ('return', 'insurer_return')) as returned,"
          + " count(*) filter (where h.from_stage = s.stage_code and h.action = 'void') as voided,"
          + " count(*) filter (where h.from_stage = s.stage_code"
          + " and h.action in ('cancel_placement', 'cancel')) as cancelled,"
          + " cast(0 as bigint) as duplicates, cast(0 as bigint) as rejected,"
          + " (select count(*) from wf_case o where o.company_id = :company and not o.closed"
          + " and o.workflow_code = s.workflow_code and o.stage_code = s.stage_code) as open"
          + " from wf_stage s"
          + " left join (wf_case_history h join wf_case c on c.id = h.case_id"
          + " and c.company_id = :company and c.workflow_code = 'NB_ACCOUNT'"
          + " and cast(h.occurred_at at time zone 'Asia/Manila' as date) between :from and :to)"
          + " on h.to_stage = s.stage_code or h.from_stage = s.stage_code"
          + " where s.workflow_code = 'NB_ACCOUNT'"
          + " group by s.workflow_code, s.stage_code, s.sort_order, s.name"
          + " order by s.sort_order";

  private static final String BULK =
      "select 'Bulk uploads' as section, 0 as sort_order, j.handler_code as stage,"
          + " count(r.id) as entered,"
          + " count(r.id) filter (where r.status = 'COMMITTED') as successful,"
          + " cast(0 as bigint) as returned, cast(0 as bigint) as voided,"
          + " cast(0 as bigint) as cancelled,"
          + " count(r.id) filter (where r.status in ('INVALID', 'FAILED')"
          + " and lower(coalesce(r.messages, '')) like '%duplicate%') as duplicates,"
          + " count(r.id) filter (where r.status in ('INVALID', 'FAILED')) as rejected,"
          + " count(r.id) filter (where r.status = 'VALID') as open"
          + " from bulk_job j join bulk_row r on r.job_id = j.id"
          + " where j.company_id = :company"
          + " and cast(j.created_at at time zone 'Asia/Manila' as date) between :from and :to"
          + " group by j.handler_code order by j.handler_code";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public StageOutcomeReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return NbReportSupport.metadata(
        CODE,
        "Successful and Fall-out Accounts per Stage",
        "Accounts entering, passing, returned, voided or cancelled per stage, and bulk upload"
            + " rejects and duplicate fall-outs (BRNB.075)",
        Permission.ACCOUNT_VIEW,
        true);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<String, Object> args = NbReportSupport.args(p).map();
    List<Map<String, Object>> rows = new ArrayList<>(jdbc.rows(STAGES, args));
    rows.addAll(jdbc.rows(BULK, args));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("stage", "Stage / Upload"),
            ReportColumn.count("entered", "Entered / Rows"),
            ReportColumn.count("successful", "Successful"),
            ReportColumn.count("returned", "Returned"),
            ReportColumn.count("voided", "Voided"),
            ReportColumn.count("cancelled", "Cancelled"),
            ReportColumn.count("rejected", "Rejected"),
            ReportColumn.count("duplicates", "Duplicate Fall-outs"),
            ReportColumn.count("open", "Open Now"))
        .groupBy(SECTION, "Section")
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .note(
            "Successful = moved to the next stage; returned = returned to Marketing or by the"
                + " insurer. Accounts refused as duplicates on the account screen are never"
                + " created; duplicate fall-outs are counted from bulk uploads.")
        .build();
  }
}
