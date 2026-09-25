package com.iortatechnxt.brokerverse.events.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Daily clean-up of the event tables: delivered outbox rows older than {@code
 * brokerverse.kafka.outbox-retention}, archived events and resolved dead letters older than {@code
 * brokerverse.kafka.archive-retention}. Pending and FAILED rows and open dead letters are kept.
 */
@Component
public class EventHousekeepingJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "EVENT_HOUSEKEEPING";

  private final OutboxStore outbox;
  private final JdbcTemplate jdbc;
  private final EventsProperties properties;
  private final Clock clock;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param outbox outbox store
   * @param jdbc JDBC template
   * @param properties retention settings
   * @param clock clock
   * @param cron schedule ({@code brokerverse.jobs.event-housekeeping-cron})
   */
  public EventHousekeepingJob(
      OutboxStore outbox,
      JdbcTemplate jdbc,
      EventsProperties properties,
      Clock clock,
      @Value("${brokerverse.jobs.event-housekeeping-cron:0 50 0 * * *}") String cron) {
    this.outbox = outbox;
    this.jdbc = jdbc;
    this.properties = properties;
    this.clock = clock;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Deletes delivered outbox rows, old archived events and resolved dead letters";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    Instant now = clock.instant();
    int delivered = outbox.purgeDelivered(now.minus(properties.outboxRetention()));
    Timestamp archiveCutOff = Timestamp.from(now.minus(properties.archiveRetention()));
    int archived = jdbc.update("delete from evt_archive where archived_at < ?", archiveCutOff);
    int deadLetters =
        jdbc.update(
            "delete from evt_dead_letter where status <> 'NEW' and resolved_at < ?", archiveCutOff);
    return new JobOutcome(
        delivered + archived + deadLetters,
        delivered
            + " outbox row(s), "
            + archived
            + " archived event(s) and "
            + deadLetters
            + " resolved dead letter(s) deleted");
  }
}
