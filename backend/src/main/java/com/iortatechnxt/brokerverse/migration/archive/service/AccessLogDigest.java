package com.iortatechnxt.brokerverse.migration.archive.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationJobs;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

/**
 * The monthly digest of the legacy archive access log (DATA_MIGRATION_DESIGN section 16): the
 * searches, views, downloads and exports of the previous month per user, sent to the holders of
 * {@code LEGACY_ACCESS_LOG_VIEW} (Compliance), who open the access log for the detail.
 */
@Component
public class AccessLogDigest implements ManagedJob {

  private static final String SQL =
      "select company_id, username, action, count(*) as n, sum(result_count) as records"
          + " from mig_access_log where accessed_at >= ? and accessed_at < ?"
          + " group by company_id, username, action order by company_id, username, action";

  private final JdbcTemplate jdbc;
  private final NotificationService notifications;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param jdbc JDBC
   * @param notifications notifications
   * @param cron schedule
   */
  public AccessLogDigest(
      JdbcTemplate jdbc,
      NotificationService notifications,
      @Value("${brokerverse.jobs.mig-access-log-digest-cron:-}") String cron) {
    this.jdbc = jdbc;
    this.notifications = notifications;
    this.cron = cron;
  }

  @Override
  public String name() {
    return MigrationJobs.ACCESS_LOG_DIGEST;
  }

  @Override
  public String description() {
    return "Sends Compliance the monthly digest of the legacy archive access log";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    LocalDate to = businessDate.withDayOfMonth(1);
    LocalDate from = to.minusMonths(1);
    Map<Long, StringBuilder> byCompany = new LinkedHashMap<>();
    RowCallbackHandler row =
        rs ->
            byCompany
                .computeIfAbsent(rs.getLong("company_id"), k -> new StringBuilder())
                .append(rs.getString("username"))
                .append(' ')
                .append(rs.getString("action").toLowerCase(Locale.ROOT))
                .append(": ")
                .append(rs.getInt("n"))
                .append(" (")
                .append(rs.getLong("records"))
                .append(" records)\n");
    jdbc.query(
        SQL,
        row,
        Timestamp.from(from.atStartOfDay(BusinessClock.zone()).toInstant()),
        Timestamp.from(to.atStartOfDay(BusinessClock.zone()).toInstant()));
    int sent = 0;
    for (Map.Entry<Long, StringBuilder> e : byCompany.entrySet()) {
      sent +=
          notifications.notifyPermission(
              "LEGACY_ACCESS_LOG_VIEW",
              new Notice(
                  "Legacy archive access in " + from.getMonth() + " " + from.getYear(),
                  e.getValue().toString(),
                  "/legacy-inquiry/access-log",
                  "MigAccessLog",
                  String.valueOf(e.getKey())));
    }
    return new JobOutcome(byCompany.size(), byCompany.size() + " companies, " + sent + " notices");
  }
}
