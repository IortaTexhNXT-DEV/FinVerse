package com.iortatechnxt.brokerverse.migration.home.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The tiles of the Migration Home screen (DATA_MIGRATION_DESIGN section 22): objects by status,
 * batches by status, open reconciliation breaks, unmapped codes, the next cut-over tasks and the
 * latest run-off snapshot. Read-only aggregates over the migration tables.
 */
@Service
@Transactional(readOnly = true)
public class MigrationHomeService {

  private static final int NEXT_TASKS = 8;

  private final JdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param jdbc JDBC
   */
  public MigrationHomeService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The tiles of a company.
   *
   * @param companyId company
   * @return tiles
   */
  public Tiles tiles(Long companyId) {
    return new Tiles(
        counts("select status, count(*) from mig_data_object group by status"),
        counts(
            "select status, count(*) from mig_batch where company_id = ? group by status",
            companyId),
        count(
            "select count(*) from mig_recon_line l join mig_recon_run r on r.id = l.run_id"
                + " where r.company_id = ? and l.status = 'BREAK'",
            companyId),
        count(
            "select count(distinct i.value) from mig_issue i join mig_batch b on b.id = i.batch_id"
                + " where b.company_id = ? and i.rule_code = 'DQ-003' and i.resolution = 'OPEN'",
            companyId),
        count(
            "select count(*) from mig_issue i join mig_batch b on b.id = i.batch_id"
                + " where b.company_id = ? and i.resolution = 'OPEN' and i.severity = 'ERROR'",
            companyId),
        count(
            "select count(*) from mig_client_match m join mig_batch b on b.id = m.batch_id"
                + " where b.company_id = ? and m.decision = 'REVIEW'",
            companyId),
        nextTasks(companyId),
        runoff(companyId));
  }

  private Map<String, Integer> counts(String sql, Object... args) {
    Map<String, Integer> out = new LinkedHashMap<>();
    jdbc.query(
        sql,
        rs -> {
          out.put(rs.getString(1), rs.getInt(2));
        },
        args);
    return out;
  }

  private int count(String sql, Object... args) {
    Integer n = jdbc.queryForObject(sql, Integer.class, args);
    return n == null ? 0 : n;
  }

  private List<Task> nextTasks(Long companyId) {
    return jdbc.query(
        "select p.plan_no, t.seq, t.phase, t.task, t.owner_role, t.planned_start, t.status"
            + " from mig_cutover_task t join mig_cutover_plan p on p.id = t.plan_id"
            + " where p.company_id = ? and p.status in ('PLANNED', 'IN_PROGRESS')"
            + " and t.status in ('NOT_STARTED', 'IN_PROGRESS', 'BLOCKED')"
            + " order by t.planned_start nulls last, p.plan_no, t.seq limit "
            + NEXT_TASKS,
        (rs, i) ->
            new Task(
                rs.getString("plan_no"),
                rs.getInt("seq"),
                rs.getString("phase"),
                rs.getString("task"),
                rs.getString("owner_role"),
                rs.getObject("planned_start", LocalDateTime.class),
                rs.getString("status")),
        companyId);
  }

  private List<Runoff> runoff(Long companyId) {
    return jdbc.query(
        "select expiry_month, sum(headers_in_force) as in_force, sum(renewed) as renewed,"
            + " sum(not_renewed + lapsed) as not_renewed, sum(still_open) as still_open"
            + " from mig_runoff_cohort where company_id = ? and snapshot_date ="
            + " (select max(snapshot_date) from mig_runoff_cohort where company_id = ?)"
            + " group by expiry_month order by expiry_month",
        (rs, i) ->
            new Runoff(
                rs.getObject("expiry_month", LocalDate.class),
                rs.getInt("in_force"),
                rs.getInt("renewed"),
                rs.getInt("not_renewed"),
                rs.getInt("still_open")),
        companyId,
        companyId);
  }

  /**
   * The tiles.
   *
   * @param objectsByStatus objects of the register by status
   * @param batchesByStatus batches by status
   * @param openBreaks open reconciliation breaks
   * @param unmappedCodes distinct unmapped legacy codes
   * @param openErrors open validation errors
   * @param pairsToReview client pairs waiting for review
   * @param nextTasks next cut-over tasks
   * @param runoff latest run-off snapshot by expiry month
   */
  public record Tiles(
      Map<String, Integer> objectsByStatus,
      Map<String, Integer> batchesByStatus,
      int openBreaks,
      int unmappedCodes,
      int openErrors,
      int pairsToReview,
      List<Task> nextTasks,
      List<Runoff> runoff) {}

  /**
   * A cut-over task.
   *
   * @param planNo plan
   * @param seq sequence
   * @param phase phase
   * @param task task
   * @param ownerRole owner
   * @param plannedStart planned start
   * @param status status
   */
  public record Task(
      String planNo,
      int seq,
      String phase,
      String task,
      String ownerRole,
      LocalDateTime plannedStart,
      String status) {}

  /**
   * A run-off month.
   *
   * @param expiryMonth month of expiry
   * @param inForce headers in force at go-live
   * @param renewed renewed in BIBS
   * @param notRenewed not renewed or lapsed
   * @param stillOpen still open
   */
  public record Runoff(
      LocalDate expiryMonth, int inForce, int renewed, int notRenewed, int stillOpen) {}
}
