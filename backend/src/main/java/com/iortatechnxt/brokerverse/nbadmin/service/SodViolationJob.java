package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.nbadmin.service.SodViolations.Violation;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Job {@value #JOB_NAME}: checks every active user against the active separation-of-duties rules
 * ({@link SodViolations}), records each breach the first time it is found in {@code
 * nba_sod_violation} and closes the breaches that no longer hold. When new breaches are found,
 * Information Security (the authorisers of the rules, {@value SodRuleService#AUTHORIZE}) is told in
 * the system and by e-mail as each prefers. Daily at 01:50 PHT ({@code
 * brokerverse.jobs.uam-sod-violations-cron}).
 */
@Component
public class SodViolationJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "UAM_SOD_VIOLATIONS";

  /** Notification event of new breaches. */
  public static final String CODE = "UAM_SOD_VIOLATION";

  private final SodViolations violations;
  private final NoticeDelivery notices;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param violations current breaches
   * @param notices notices
   * @param jdbc database
   * @param transactions transaction manager
   * @param clock clock
   * @param cron schedule
   */
  public SodViolationJob(
      SodViolations violations,
      NoticeDelivery notices,
      JdbcTemplate jdbc,
      PlatformTransactionManager transactions,
      Clock clock,
      @Value("${brokerverse.jobs.uam-sod-violations-cron:0 50 17 * * *}") String cron) {
    this.violations = violations;
    this.notices = notices;
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(transactions);
    this.clock = clock;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Finds the users whose group profiles breach an active separation-of-duties rule and"
        + " tells Information Security of the new ones";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    Integer added = tx.execute(s -> record(violations.current()));
    int fresh = added == null ? 0 : added;
    if (fresh > 0) {
      notices.toPermission(
          SodRuleService.AUTHORIZE,
          new Notice(
              "Separation of duties: " + fresh + " new breach(es)",
              fresh
                  + " user(s) hold group profiles or permissions that an active"
                  + " separation-of-duties rule keeps apart. Review them on the Violations tab"
                  + " and ask UIDM-ISC to correct the group profiles.",
              SodRuleService.SCREEN + "?tab=violations",
              SodRuleService.ENTITY,
              JOB_NAME),
          CODE,
          true);
    }
    return new JobOutcome(fresh, fresh + " new breach(es) of separation of duties");
  }

  /** Records the new breaches and closes the ones gone; returns the number of new ones. */
  private int record(List<Violation> current) {
    Timestamp now = Timestamp.from(clock.instant());
    Set<String> seen = new HashSet<>();
    int fresh = 0;
    for (Violation v : current) {
      seen.add(v.ruleCode() + '|' + v.username());
      fresh +=
          jdbc.update(
              "insert into nba_sod_violation (rule_code, username, found_at) select ?, ?, ?"
                  + " where not exists (select 1 from nba_sod_violation where rule_code = ?"
                  + " and username = ? and cleared_at is null)",
              v.ruleCode(),
              v.username(),
              now,
              v.ruleCode(),
              v.username());
    }
    for (var open :
        jdbc.queryForList(
            "select id, rule_code, username from nba_sod_violation where cleared_at is null")) {
      if (!seen.contains(open.get("rule_code") + "|" + open.get("username"))) {
        jdbc.update(
            "update nba_sod_violation set cleared_at = ? where id = ?", now, open.get("id"));
      }
    }
    return fresh;
  }
}
