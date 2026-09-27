package com.iortatechnxt.brokerverse.migration.cutover.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The go / no-go criteria measured by the system (DATA_MIGRATION_DESIGN 17.5): day-1 objects
 * accepted, count and amount reconciliation of the latest batches, financial rejects, Migration
 * Clearing, legacy control accounts and the client review queue.
 */
@Component
@Transactional(readOnly = true)
public class GonogoMeasures {

  private static final String LATEST_BATCHES =
      "select distinct on (object_code) id, object_code from mig_batch"
          + " where company_id = ? and status <> 'ROLLED_BACK' order by object_code, id desc";

  private static final String DAY1 =
      "select count(*) as total, count(*) filter (where exists (select 1 from mig_batch b"
          + " where b.company_id = ? and b.object_code = o.code and b.status = 'SIGNED_OFF')) as done"
          + " from mig_data_object o where o.day1_need"
          + " and coalesce(o.decided_class, o.proposed_class) in ('MIGRATE', 'CARRY_FORWARD')";

  private static final String BREAKS =
      "select count(*) from mig_recon_line l join mig_recon_run r on r.id = l.run_id"
          + " where l.level = ? and l.status = 'BREAK' and r.id in (select max(x.id) from mig_recon_run x"
          + " where x.batch_id in (select b.id from ("
          + LATEST_BATCHES
          + ") b"
          + " where cast(? as varchar) is null or b.object_code = any(string_to_array(?, ',')))"
          + " group by x.batch_id)";

  private static final String REJECTS =
      "select count(*) from mig_stage_row s where s.status = 'REJECTED' and s.batch_id in"
          + " (select b.id from ("
          + LATEST_BATCHES
          + ") b where b.object_code in ('F01', 'F02', 'G01'))";

  private static final String CLEARING =
      "select count(*) from (select 1 from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " where e.company_id = ? and a.code = 'LGC-CLR' group by e.branch_id, e.currency"
          + " having sum(e.debit_fc - e.credit_fc) <> 0) t";

  private static final String QUEUE =
      "select count(*) from mig_client_match m join mig_batch b on b.id = m.batch_id"
          + " where b.company_id = ? and m.decision = 'REVIEW'";

  private static final String FINANCIAL = "F01,F02,G01";

  private final JdbcTemplate jdbc;

  /**
   * Creates the measures.
   *
   * @param jdbc JDBC
   */
  public GonogoMeasures(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Measures a criterion.
   *
   * @param companyId company
   * @param measure measure code
   * @return the value and whether the threshold is met
   */
  public Result measure(Long companyId, String measure) {
    return switch (measure) {
      case "DAY1_ACCEPTED" -> day1(companyId);
      case "COUNTS" -> zero(breaks(companyId, "L1", null), "count break(s)");
      case "FINANCIAL_REJECTS" ->
          zero(count(REJECTS, companyId), "rejected row(s) in the financial batches");
      case "AMOUNTS" -> zero(breaks(companyId, "L2", FINANCIAL), "amount break(s)");
      case "CLEARING" -> zero(count(CLEARING, companyId), "branch and currency(ies) not at zero");
      case "LEGACY_CONTROL" -> zero(breaks(companyId, "L5", "F01,F02"), "control account break(s)");
      case "CLIENT_QUEUE" -> zero(count(QUEUE, companyId), "client pair(s) to review");
      default -> new Result("Not measured", false);
    };
  }

  private Result day1(Long companyId) {
    return jdbc.queryForObject(
        DAY1,
        (rs, i) -> {
          int total = rs.getInt("total");
          int done = rs.getInt("done");
          return new Result(done + " of " + total + " accepted", total > 0 && done == total);
        },
        companyId);
  }

  private int breaks(Long companyId, String level, String objects) {
    Integer n = jdbc.queryForObject(BREAKS, Integer.class, level, companyId, objects, objects);
    return n == null ? 0 : n;
  }

  private int count(String sql, Long companyId) {
    Integer n = jdbc.queryForObject(sql, Integer.class, companyId);
    return n == null ? 0 : n;
  }

  private static Result zero(int n, String what) {
    return new Result(n + " " + what, n == 0);
  }

  /**
   * A measurement.
   *
   * @param value measured value
   * @param met threshold met
   */
  public record Result(String value, boolean met) {}
}
