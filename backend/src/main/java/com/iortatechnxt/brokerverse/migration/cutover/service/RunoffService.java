package com.iortatechnxt.brokerverse.migration.cutover.service;

import com.iortatechnxt.brokerverse.migration.cutover.domain.RunoffCohort;
import com.iortatechnxt.brokerverse.migration.cutover.domain.RunoffCohortRepository;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The run-off of the migrated policies (DATA_MIGRATION_DESIGN 17.2 and 17.6; report MIG-RUNOFF):
 * per expiry month and source system, the in-force legacy headers (policy headers of origin
 * MIGRATED), their premium and the outcome at the snapshot date - renewed (a renewal account names
 * the header as the account it renews), not renewed (the header was cancelled), lapsed (expired
 * without a renewal) or still open. A snapshot of the same day replaces the earlier one.
 */
@Service
@Transactional
public class RunoffService {

  private static final String COHORTS =
      "select date_trunc('month', a.period_to)::date as expiry_month, l.source_system,"
          + " count(*) as headers, coalesce(sum(a.gross_premium), 0) as premium,"
          + " count(*) filter (where r.arn is not null) as renewed,"
          + " count(*) filter (where r.arn is null and a.status = 'CANCELLED') as not_renewed,"
          + " count(*) filter (where r.arn is null and a.status <> 'CANCELLED'"
          + " and a.period_to < ?) as lapsed,"
          + " count(*) filter (where r.arn is null and a.status <> 'CANCELLED'"
          + " and a.period_to >= ?) as still_open"
          + " from acc_account a join acc_account_legacy l on l.account_id = a.id"
          + " left join lateral (select x.arn from acc_account x where x.renewal_of_ref = a.arn"
          + " and x.status <> 'CANCELLED' limit 1) r on true"
          + " where a.company_id = ? and a.origin = 'MIGRATED' and l.rolled_back_at is null"
          + " and a.period_to is not null"
          + " group by 1, 2 order by 1, 2";

  private final RunoffCohortRepository cohorts;
  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cohorts cohorts
   * @param jdbc JDBC
   * @param clock clock
   */
  public RunoffService(RunoffCohortRepository cohorts, JdbcTemplate jdbc, Clock clock) {
    this.cohorts = cohorts;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * Takes the run-off snapshot of a company.
   *
   * @param companyId company
   * @param snapshotDate snapshot date
   * @return cohorts
   */
  public List<RunoffCohort> snapshot(Long companyId, LocalDate snapshotDate) {
    cohorts.deleteByCompanyIdAndSnapshotDate(companyId, snapshotDate);
    cohorts.flush();
    List<RunoffCohort> out = new ArrayList<>();
    jdbc.query(
        COHORTS,
        rs -> {
          out.add(
              cohorts.save(
                  new RunoffCohort(
                      companyId,
                      snapshotDate,
                      new RunoffCohort.Key(
                          rs.getDate("expiry_month").toLocalDate(), rs.getString("source_system")),
                      new RunoffCohort.Outcomes(
                          rs.getInt("headers"),
                          nz(rs.getBigDecimal("premium")),
                          rs.getInt("renewed"),
                          rs.getInt("not_renewed"),
                          rs.getInt("lapsed"),
                          rs.getInt("still_open")),
                      clock.instant())));
        },
        Date.valueOf(snapshotDate),
        Date.valueOf(snapshotDate),
        companyId);
    return out;
  }

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  /**
   * The latest snapshot of a company.
   *
   * @param companyId company
   * @return cohorts, empty before the first snapshot
   */
  @Transactional(readOnly = true)
  public List<RunoffCohort> latest(Long companyId) {
    return cohorts
        .latestSnapshot(companyId)
        .map(
            d ->
                cohorts.findByCompanyIdAndSnapshotDateOrderByExpiryMonthAscSourceSystemAsc(
                    companyId, d))
        .orElse(List.of());
  }

  /**
   * The companies with migrated policy headers (job scope).
   *
   * @return company ids
   */
  @Transactional(readOnly = true)
  public List<Long> companies() {
    return jdbc.queryForList(
        "select distinct company_id from acc_account where origin = 'MIGRATED'", Long.class);
  }
}
