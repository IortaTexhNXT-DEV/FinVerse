package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Job {@value #JOB_NAME}: compares every user holding a Windows ID with the account of the
 * Enterprise SSO / UIDM-ISC directory ({@link EnterpriseDirectory}) and records each mismatch of
 * the day in {@code idn_reconciliation_mismatch}: an account the directory does not know, a status
 * that differs (active in one, not in the other), or group profiles that differ when the directory
 * names them. Nothing is changed here: UIDM-ISC corrects the user with an event. The holders of the
 * user-management permission are told of the mismatches found (in the system and by e-mail as each
 * prefers). Daily at 01:40 PHT ({@code brokerverse.jobs.identity-reconciliation-cron}); a scheduled
 * run compares every {@value #INTERVAL} days and skips the other days, a run started by a user
 * always compares.
 */
@Component
public class IdentityReconciliationJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "UAM_IDENTITY_RECONCILIATION";

  /** Notification event. */
  public static final String CODE = "UAM_RECONCILIATION_MISMATCH";

  /** Mismatch: the directory does not know the account. */
  public static final String NOT_IN_DIRECTORY = "NOT_IN_DIRECTORY";

  /** Mismatch: active in one, not in the other. */
  public static final String STATUS = "STATUS";

  /** Mismatch: different group profiles. */
  public static final String GROUP_PROFILES = "GROUP_PROFILES";

  private static final Logger LOG = LoggerFactory.getLogger(IdentityReconciliationJob.class);
  private static final int DETAIL = 500;

  /** Days between two reconciliations (1 = daily, 0 = never). */
  public static final String INTERVAL = "UAM_RECONCILIATION_INTERVAL_DAYS";

  private final AppUserRepository users;
  private final SystemParameterService parameters;
  private final EnterpriseDirectory directory;
  private final NoticeDelivery notices;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param users users
   * @param directory the directory of the Enterprise SSO platform
   * @param notices notices
   * @param jdbc database
   * @param transactions transaction manager
   * @param clock clock
   * @param cron schedule
   * @param parameters the reconciliation interval
   */
  public IdentityReconciliationJob(
      AppUserRepository users,
      EnterpriseDirectory directory,
      NoticeDelivery notices,
      JdbcTemplate jdbc,
      PlatformTransactionManager transactions,
      Clock clock,
      @Value("${brokerverse.jobs.identity-reconciliation-cron:0 40 17 * * *}") String cron,
      SystemParameterService parameters) {
    this.parameters = parameters;
    this.users = users;
    this.directory = directory;
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
    return "Compares the users and their group profiles with the UIDM-ISC directory and reports"
        + " the mismatches";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    String notDue = startedByUser() ? null : notDue(businessDate);
    if (notDue != null) {
      return new JobOutcome(0, notDue);
    }
    List<Snapshot> snapshots = tx.execute(s -> snapshots());
    List<Mismatch> mismatches = new ArrayList<>();
    int compared = 0;
    int unavailable = 0;
    for (Snapshot user : snapshots == null ? List.<Snapshot>of() : snapshots) {
      try {
        compare(user, directory.find(user.windowsId())).ifPresent(mismatches::add);
        compared++;
      } catch (IdentityRefused ex) {
        unavailable++;
        LOG.warn(
            "Reconciliation of user {} skipped: the directory did not answer", user.username());
      }
    }
    tx.executeWithoutResult(s -> record(businessDate, mismatches));
    if (!mismatches.isEmpty()) {
      notices.toPermission(
          "USER_MANAGE",
          new Notice(
              "Identity reconciliation: " + mismatches.size() + " mismatch(es)",
              mismatches.size()
                  + " user(s) differ from the "
                  + directory.name()
                  + " directory on "
                  + BusinessClock.dateOf(clock.instant())
                  + ". Review them on the Identity Sync screen and ask UIDM-ISC to correct them.",
              IdentityAlerts.SCREEN,
              IdentitySyncService.ENTITY,
              JOB_NAME),
          CODE,
          true);
    }
    return new JobOutcome(
        compared,
        compared
            + " user(s) compared, "
            + mismatches.size()
            + " mismatch(es)"
            + (unavailable > 0
                ? ", " + unavailable + " not compared (directory unavailable)"
                : ""));
  }

  /** Why a scheduled run does not compare today, or null when it is due. */
  private String notDue(LocalDate businessDate) {
    int interval = parameters.intValue(INTERVAL, 1);
    if (interval <= 0) {
      return "Reconciliation is switched off (" + INTERVAL + " = 0)";
    }
    LocalDate last = lastComparedOn();
    return last != null && last.plusDays(interval).isAfter(businessDate)
        ? "Not due: last reconciliation on " + last + ", every " + interval + " day(s)"
        : null;
  }

  /** Whether this run was started by a user (Synchronise Now / Run Now): it always compares. */
  private boolean startedByUser() {
    List<String> triggers =
        jdbc.queryForList(
            "select trigger_type from sys_job_run where job_name = ? and status = 'RUNNING'"
                + " order by started_at desc limit 1",
            String.class,
            JOB_NAME);
    return !triggers.isEmpty() && "MANUAL".equals(triggers.get(0));
  }

  /** The business date of the last run that compared users. */
  private LocalDate lastComparedOn() {
    java.sql.Timestamp last =
        jdbc.queryForObject(
            "select max(started_at) from sys_job_run where job_name = ? and status = 'SUCCEEDED'"
                + " and items_processed > 0",
            java.sql.Timestamp.class,
            JOB_NAME);
    return last == null ? null : BusinessClock.dateOf(last.toInstant());
  }

  private List<Snapshot> snapshots() {
    return users.findAll().stream()
        .filter(u -> u.getWindowsId() != null && !u.getWindowsId().isBlank())
        .map(
            u ->
                new Snapshot(
                    u.getUsername(),
                    u.getWindowsId(),
                    u.isEnabled(),
                    u.getRoles().stream()
                        .map(Role::getCode)
                        .collect(Collectors.toCollection(TreeSet::new))))
        .toList();
  }

  /**
   * The mismatch of a user with its directory account.
   *
   * @param user the user in the system
   * @param account the directory account, empty when unknown there
   * @return the mismatch, empty when they agree
   */
  public static Optional<Mismatch> compare(Snapshot user, Optional<DirectoryAccount> account) {
    if (account.isEmpty()) {
      return user.enabled()
          ? Optional.of(
              new Mismatch(
                  user, NOT_IN_DIRECTORY, "Active in the system but unknown to the directory"))
          : Optional.empty();
    }
    boolean active = account.get().status().grantsAccess();
    if (active != user.enabled()) {
      return Optional.of(
          new Mismatch(
              user,
              STATUS,
              (user.enabled() ? "Active" : "Inactive")
                  + " in the system, "
                  + (active ? "active" : "not active")
                  + " in the directory"));
    }
    Set<String> held = new TreeSet<>(account.get().groupProfiles());
    if (!held.isEmpty() && !held.equals(user.groupProfiles())) {
      return Optional.of(
          new Mismatch(
              user,
              GROUP_PROFILES,
              "Group profiles "
                  + user.groupProfiles()
                  + " in the system, "
                  + held
                  + " in the directory"));
    }
    return Optional.empty();
  }

  private void record(LocalDate runDate, List<Mismatch> mismatches) {
    for (Mismatch m : mismatches) {
      String detail = m.detail().length() <= DETAIL ? m.detail() : m.detail().substring(0, DETAIL);
      jdbc.update(
          "insert into idn_reconciliation_mismatch (run_date, username, windows_id, kind, detail,"
              + " found_at) values (?, ?, ?, ?, ?, ?)",
          runDate,
          m.user().username(),
          m.user().windowsId(),
          m.kind(),
          detail,
          java.sql.Timestamp.from(clock.instant()));
    }
  }

  /**
   * A user as the system holds it.
   *
   * @param username user ID
   * @param windowsId Windows ID
   * @param enabled whether active
   * @param groupProfiles codes of the group profiles held
   */
  public record Snapshot(
      String username, String windowsId, boolean enabled, Set<String> groupProfiles) {}

  /**
   * A difference found.
   *
   * @param user the user
   * @param kind NOT_IN_DIRECTORY, STATUS or GROUP_PROFILES
   * @param detail what differs
   */
  public record Mismatch(Snapshot user, String kind, String detail) {}
}
