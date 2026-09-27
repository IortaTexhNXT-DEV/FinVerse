package com.iortatechnxt.brokerverse.migration.report;

import static com.iortatechnxt.brokerverse.migration.report.MigReport.BATCH_FILTER;
import static com.iortatechnxt.brokerverse.migration.report.MigReport.OBJECT_FILTER;
import static com.iortatechnxt.brokerverse.migration.report.MigReport.PHT;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The reports of the migration pipeline (DATA_MIGRATION_DESIGN section 21): object register,
 * decisions, code maps, data-quality issues, rejects, batch log, reconciliation, client matching
 * and sign-off status.
 */
@Configuration(proxyBeanMethods = false)
public class MigrationReports {

  private static final String DECISION = "decision";

  private static final String OBJECT = "object_code";
  private static final String OBJECT_LABEL = "Object";
  private static final String STATUS = "status";
  private static final String STATUS_LABEL = "Status";
  private static final String BATCH = "batch_no";
  private static final String BATCH_LABEL = "Batch";
  private static final String SOURCE = "source_system";
  private static final String SOURCE_LABEL = "Source";

  /**
   * {@code MIG-OBJECT-REGISTER}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migObjectRegisterReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-OBJECT-REGISTER",
            "Data Object Register",
            "Objects with their class, the four criteria, owners, dependencies and status")
        .byObject()
        .sql(
            "select x.code as object_code, x.name, x.category, x.source_systems,"
                + " coalesce(x.decided_class, x.proposed_class) as migration_class,"
                + " case when x.decided_class is null then 'Proposed' else 'Decided' end as basis,"
                + " case when x.day1_need then 'Yes' else 'No' end as day1,"
                + " case when x.compliance_need then 'Yes' else 'No' end as compliance,"
                + " case when x.archival_option then 'Yes' else 'No' end as archival,"
                + " x.data_trust, x.business_owner, x.data_steward, x.depends_on, x.load_order,"
                + " x.status from mig_data_object x where 1 = 1"
                + OBJECT_FILTER.replace("x.object_code", "x.code")
                + " and :company is not null order by x.load_order, x.code")
        .columns(
            ReportColumn.text(OBJECT, OBJECT_LABEL),
            ReportColumn.text("name", "Name"),
            ReportColumn.text("category", "Category"),
            ReportColumn.text("source_systems", "Sources"),
            ReportColumn.text("migration_class", "Class"),
            ReportColumn.text("basis", "Basis"),
            ReportColumn.text("day1", "Day-1 need"),
            ReportColumn.text("compliance", "Compliance need"),
            ReportColumn.text("archival", "Archive option"),
            ReportColumn.text("data_trust", "Data trust"),
            ReportColumn.text("business_owner", "Business owner"),
            ReportColumn.text("data_steward", "Data steward"),
            ReportColumn.text("depends_on", "Depends on"),
            ReportColumn.count("load_order", "Load order"),
            ReportColumn.text(STATUS, STATUS_LABEL))
        .row(MigReport.relabel("migration_class", STATUS, "category", "data_trust"))
        .build();
  }

  /**
   * {@code MIG-DECISIONS}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migDecisionsReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-DECISIONS",
            "Migration Decisions",
            "Decision history of the data objects with the submitter and the business owner's decision")
        .byObject()
        .sql(
            "select x.decision_no, x.object_code, x.proposed_class, x.condition_text, x.rationale,"
                + " x.status, x.submitted_by, to_char(x.submitted_at"
                + PHT
                + " as submitted_at, x.decided_by, to_char(x.decided_at"
                + PHT
                + " as decided_at, x.return_reason from mig_object_decision x"
                + " where x.company_id = :company"
                + OBJECT_FILTER
                + " order by x.object_code, x.submitted_at")
        .columns(
            ReportColumn.text("decision_no", "Decision"),
            ReportColumn.text("proposed_class", "Class"),
            ReportColumn.text("condition_text", "Condition"),
            ReportColumn.text("rationale", "Rationale"),
            ReportColumn.text(STATUS, STATUS_LABEL),
            ReportColumn.text("submitted_by", "Submitted by"),
            ReportColumn.text("submitted_at", "Submitted"),
            ReportColumn.text("decided_by", "Decided by"),
            ReportColumn.text("decided_at", "Decided"),
            ReportColumn.text("return_reason", "Return reason"))
        .groupBy(OBJECT, OBJECT_LABEL)
        .row(MigReport.relabel("proposed_class", STATUS))
        .build();
  }

  /**
   * {@code MIG-UNMAPPED-CODES}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migUnmappedCodesReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-UNMAPPED-CODES",
            "Unmapped Legacy Codes",
            "Legacy codes without an approved code map entry, per code map and source system")
        .byObject()
        .byBatch()
        .sql(
            "select substring(i.message from '^Code .* of (\\S+) is not mapped$') as map_set,"
                + " e.source_system, i.value as legacy_code, x.object_code,"
                + " count(*) as occurrences, count(distinct b.batch_no) as batches,"
                + " min(b.batch_no) as first_batch from mig_issue i"
                + " join mig_stage_row x on x.id = i.stage_row_id"
                + " join mig_extract e on e.id = x.extract_id"
                + " join mig_batch b on b.id = i.batch_id"
                + " where b.company_id = :company and i.rule_code = 'DQ-003' and i.resolution = 'OPEN'"
                + OBJECT_FILTER
                + BATCH_FILTER
                + " group by 1, 2, 3, 4 order by 1, 2, 3")
        .columns(
            ReportColumn.text(SOURCE, SOURCE_LABEL),
            ReportColumn.text("legacy_code", "Legacy code"),
            ReportColumn.text(OBJECT, OBJECT_LABEL),
            ReportColumn.count("occurrences", "Rows"),
            ReportColumn.count("batches", "Batches"),
            ReportColumn.text("first_batch", "First batch"))
        .groupBy("map_set", "Code map")
        .build();
  }

  /**
   * {@code MIG-MAP-VERSIONS}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migMapVersionsReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-MAP-VERSIONS",
            "Code Map Versions",
            "Versions of each code map with entries, approvals and the batches that used them")
        .sql(
            "select v.set_code, v.version_no, v.status,"
                + " (select count(*) from mig_code_map_entry n where n.version_id = v.id) as entries,"
                + " v.submitted_by, v.approved_by, to_char(v.approved_at"
                + PHT
                + " as approved_at,"
                + " (select string_agg(b.batch_no, ', ' order by b.batch_no) from mig_batch_map_version m"
                + " join mig_batch b on b.id = m.batch_id where m.set_code = v.set_code"
                + " and m.version_no = v.version_no and b.company_id = :company) as batches"
                + " from mig_code_map_version v order by v.set_code, v.version_no")
        .columns(
            ReportColumn.count("version_no", "Version"),
            ReportColumn.text(STATUS, STATUS_LABEL),
            ReportColumn.count("entries", "Entries"),
            ReportColumn.text("submitted_by", "Submitted by"),
            ReportColumn.text("approved_by", "Approved by"),
            ReportColumn.text("approved_at", "Approved"),
            ReportColumn.text("batches", "Used by batches"))
        .groupBy("set_code", "Code map")
        .row(MigReport.relabel(STATUS))
        .build();
  }

  /**
   * {@code MIG-DQ-ISSUES}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migDqIssuesReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-DQ-ISSUES",
            "Data-Quality Issues",
            "Validation findings per rule and severity with their resolution")
        .byObject()
        .byBatch()
        .sql(
            "select b.batch_no, x.object_code, i.rule_code, i.severity, i.resolution,"
                + " count(*) as issues, count(distinct i.stage_row_id) as rows_affected,"
                + " min(i.message) as example from mig_issue i"
                + " join mig_batch b on b.id = i.batch_id join mig_stage_row x on x.id = i.stage_row_id"
                + " where b.company_id = :company"
                + OBJECT_FILTER
                + BATCH_FILTER
                + " group by 1, 2, 3, 4, 5 order by 1, 3, 4, 5")
        .columns(
            ReportColumn.text(OBJECT, OBJECT_LABEL),
            ReportColumn.text("rule_code", "Rule"),
            ReportColumn.text("severity", "Severity"),
            ReportColumn.text("resolution", "Resolution"),
            ReportColumn.count("issues", "Issues"),
            ReportColumn.count("rows_affected", "Rows"),
            ReportColumn.text("example", "Example message"))
        .groupBy(BATCH, BATCH_LABEL)
        .row(MigReport.relabel("severity", "resolution"))
        .build();
  }

  /**
   * {@code MIG-REJECTS}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migRejectsReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-REJECTS",
            "Rejected Rows",
            "Invalid and rejected rows of each batch with the reasons")
        .byObject()
        .byBatch()
        .sql(
            "select b.batch_no, x.object_code, x.layout_code, x.row_no, x.legacy_key, x.status,"
                + " coalesce(x.message, (select string_agg(i.rule_code || ' ' || i.message, '; ')"
                + " from mig_issue i where i.stage_row_id = x.id and i.severity = 'ERROR')) as reason"
                + " from mig_stage_row x join mig_batch b on b.id = x.batch_id"
                + " where b.company_id = :company and x.status in ('INVALID', 'REJECTED')"
                + OBJECT_FILTER
                + BATCH_FILTER
                + " order by b.batch_no, x.layout_code, x.row_no")
        .columns(
            ReportColumn.text("layout_code", "Layout"),
            ReportColumn.count("row_no", "Row"),
            ReportColumn.text("legacy_key", "Legacy key"),
            ReportColumn.text(STATUS, STATUS_LABEL),
            ReportColumn.text("reason", "Reason"))
        .groupBy(BATCH, BATCH_LABEL)
        .row(MigReport.relabel(STATUS))
        .build();
  }

  /**
   * {@code MIG-BATCH-LOG}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migBatchLogReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-BATCH-LOG",
            "Migration Batches",
            "Batches with their timings, counts, error rate and approvals")
        .byObject()
        .byBatch()
        .sql(
            "select b.batch_no, b.object_code, b.mode, b.environment_class, b.status,"
                + " b.staged_count, b.valid_count, b.invalid_count, b.loaded_count, b.skipped_count,"
                + " b.rejected_count, b.error_rate, b.load_approved_by, b.loaded_by,"
                + " to_char(b.started_at"
                + PHT
                + " as started_at, to_char(b.ended_at"
                + PHT
                + " as ended_at, round(extract(epoch from (b.ended_at - b.started_at)))::int as seconds"
                + " from mig_batch b where b.company_id = :company"
                + OBJECT_FILTER.replace("x.", "b.")
                + BATCH_FILTER
                + " order by b.id")
        .columns(
            ReportColumn.text(BATCH, BATCH_LABEL),
            ReportColumn.text(OBJECT, OBJECT_LABEL),
            ReportColumn.text("mode", "Mode"),
            ReportColumn.text("environment_class", "Environment"),
            ReportColumn.text(STATUS, STATUS_LABEL),
            ReportColumn.count("staged_count", "Staged"),
            ReportColumn.count("valid_count", "Valid"),
            ReportColumn.count("invalid_count", "Invalid"),
            ReportColumn.count("loaded_count", "Loaded"),
            ReportColumn.count("skipped_count", "Skipped"),
            ReportColumn.count("rejected_count", "Rejected"),
            ReportColumn.percent("error_rate", "Error rate"),
            ReportColumn.text("load_approved_by", "Load approved by"),
            ReportColumn.text("loaded_by", "Loaded by"),
            ReportColumn.text("started_at", "Started"),
            ReportColumn.text("ended_at", "Ended"),
            ReportColumn.count("seconds", "Seconds"))
        .row(MigReport.relabel(STATUS, "environment_class", "mode"))
        .build();
  }

  /**
   * {@code MIG-RECON-SUMMARY}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migReconSummaryReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-RECON-SUMMARY",
            "Reconciliation Summary",
            "Latest reconciliation of each batch by level with matched, explained and open lines")
        .byObject()
        .byBatch()
        .sql(
            "select b.batch_no, x.object_code, x.run_no, l.level,"
                + " count(*) filter (where l.status = 'MATCHED') as matched,"
                + " count(*) filter (where l.status = 'BREAK') as open_breaks,"
                + " count(*) filter (where l.status = 'EXPLAINED') as explained,"
                + " count(*) filter (where l.status = 'APPROVED') as approved"
                + " from mig_recon_run x join mig_batch b on b.id = x.batch_id"
                + " join mig_recon_line l on l.run_id = x.id"
                + " where x.company_id = :company and x.id = (select max(r2.id) from mig_recon_run r2"
                + " where r2.batch_id = x.batch_id)"
                + OBJECT_FILTER
                + BATCH_FILTER
                + " group by 1, 2, 3, 4 order by 1, 4")
        .columns(
            ReportColumn.text(OBJECT, OBJECT_LABEL),
            ReportColumn.text("run_no", "Run"),
            ReportColumn.text("level", "Level"),
            ReportColumn.count("matched", "Matched"),
            ReportColumn.count("open_breaks", "Open breaks"),
            ReportColumn.count("explained", "Explained"),
            ReportColumn.count("approved", "Approved"))
        .groupBy(BATCH, BATCH_LABEL)
        .build();
  }

  /**
   * {@code MIG-RECON-DETAIL}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migReconDetailReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-RECON-DETAIL",
            "Reconciliation Detail",
            "Reconciliation lines with source, staged and loaded values, breaks and explanations")
        .byObject()
        .byBatch()
        .sql(
            "select b.batch_no, x.run_no, l.level, l.measure, l.currency, l.source_value,"
                + " l.staged_value, l.target_value, l.difference, l.status, l.break_reason,"
                + " l.explanation, l.explained_by, l.approved_by"
                + " from mig_recon_run x join mig_batch b on b.id = x.batch_id"
                + " join mig_recon_line l on l.run_id = x.id where x.company_id = :company"
                + OBJECT_FILTER
                + BATCH_FILTER
                + " order by b.batch_no, x.run_no, l.level, l.id")
        .columns(
            ReportColumn.text("run_no", "Run"),
            ReportColumn.text("level", "Level"),
            ReportColumn.text("measure", "Measure"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.amountNoTotal("source_value", "Source"),
            ReportColumn.amountNoTotal("staged_value", "Staged"),
            ReportColumn.amountNoTotal("target_value", "BIBS"),
            ReportColumn.amountNoTotal("difference", "Difference"),
            ReportColumn.text(STATUS, STATUS_LABEL),
            ReportColumn.text("break_reason", "Reason"),
            ReportColumn.text("explanation", "Explanation"),
            ReportColumn.text("explained_by", "Explained by"),
            ReportColumn.text("approved_by", "Approved by"))
        .groupBy(BATCH, BATCH_LABEL)
        .row(MigReport.relabel(STATUS, "break_reason"))
        .build();
  }

  /**
   * {@code MIG-CLIENT-MATCH}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migClientMatchReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-CLIENT-MATCH",
            "Client Matching",
            "Clusters of matching client records with scores, keys, decisions and survivors")
        .byBatch()
        .sql(
            "select b.batch_no, m.cluster_no, m.left_key,"
                + " coalesce(m.right_key, m.right_client_code) as matched_with,"
                + " case when m.right_client_code is null then 'Legacy' else 'BIBS' end as matched_origin,"
                + " m.score, m.matched_keys, m.decision, m.survivor_key, m.decided_by"
                + " from mig_client_match m join mig_batch b on b.id = m.batch_id"
                + " where b.company_id = :company"
                + BATCH_FILTER
                + " order by b.batch_no, m.cluster_no, m.id")
        .columns(
            ReportColumn.count("cluster_no", "Cluster"),
            ReportColumn.text("left_key", "Legacy client"),
            ReportColumn.text("matched_with", "Matched with"),
            ReportColumn.text("matched_origin", "Origin"),
            ReportColumn.count("score", "Score"),
            ReportColumn.text("matched_keys", "Keys matched"),
            ReportColumn.text(DECISION, "Decision"),
            ReportColumn.text("survivor_key", "Survivor"),
            ReportColumn.text("decided_by", "Decided by"))
        .groupBy(BATCH, BATCH_LABEL)
        .row(MigReport.relabel(DECISION))
        .build();
  }

  /**
   * {@code MIG-SIGNOFF-STATUS}.
   *
   * @param jdbc report SQL
   * @return report
   */
  @Bean
  public ReportDefinition migSignoffStatusReport(NbReportJdbc jdbc) {
    return MigReport.of(
            jdbc,
            "MIG-SIGNOFF-STATUS",
            "Sign-off Status",
            "Gates signed per object and batch with the signer, role, decision and evidence")
        .byObject()
        .sql(
            "select x.object_code, coalesce(b.batch_no, '') as batch_no, x.gate, x.role_code,"
                + " x.username, x.decision, to_char(x.signed_at"
                + PHT
                + " as signed_at, x.comment, x.evidence_name from mig_signoff x"
                + " left join mig_batch b on b.id = x.batch_id where x.company_id = :company"
                + OBJECT_FILTER
                + " order by x.object_code, x.gate, x.signed_at")
        .columns(
            ReportColumn.text(BATCH, BATCH_LABEL),
            ReportColumn.text("gate", "Gate"),
            ReportColumn.text("role_code", "Role"),
            ReportColumn.text("username", "Signed by"),
            ReportColumn.text(DECISION, "Decision"),
            ReportColumn.text("signed_at", "Signed"),
            ReportColumn.text("comment", "Comment"),
            ReportColumn.text("evidence_name", "Evidence"))
        .groupBy(OBJECT, OBJECT_LABEL)
        .row(MigReport.relabel(DECISION))
        .build();
  }
}
