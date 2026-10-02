package com.iortatechnxt.brokerverse.sharedstate.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Deletes expired rows of the database fallback of the session state ({@code sec_revoked_token},
 * {@code sys_shared_counter}). With Redis the entries expire by themselves and the tables stay
 * empty.
 */
@Component
public class SharedStateCleanupJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "SHARED_STATE_CLEANUP";

  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param jdbc JDBC template
   * @param clock clock
   * @param cron schedule ({@code brokerverse.jobs.shared-state-cleanup-cron})
   */
  public SharedStateCleanupJob(
      JdbcTemplate jdbc,
      Clock clock,
      @Value("${brokerverse.jobs.shared-state-cleanup-cron:0 40 0 * * *}") String cron) {
    this.jdbc = jdbc;
    this.clock = clock;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Deletes expired revoked tokens and shared counters (database fallback of Redis)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    Timestamp now = Timestamp.from(clock.instant());
    int tokens = jdbc.update("delete from sec_revoked_token where expires_at <= ?", now);
    int counters = jdbc.update("delete from sys_shared_counter where expires_at <= ?", now);
    return new JobOutcome(
        tokens + counters,
        tokens + " expired revoked token(s) and " + counters + " expired counter(s) deleted");
  }
}
