package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchLog;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchLogRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import java.time.Clock;
import org.springframework.stereotype.Component;

/** Writes the run log of a batch (DATA_MIGRATION_DESIGN section 11). */
@Component
public class BatchLogger {

  private final BatchLogRepository logs;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the logger.
   *
   * @param logs run log
   * @param currentUser current user
   * @param clock clock
   */
  public BatchLogger(BatchLogRepository logs, CurrentUser currentUser, Clock clock) {
    this.logs = logs;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Logs an information line.
   *
   * @param batch batch
   * @param step step
   * @param message message
   */
  public void info(MigBatch batch, String step, String message) {
    write(batch, step, "INFO", message);
  }

  /**
   * Logs a warning line.
   *
   * @param batch batch
   * @param step step
   * @param message message
   */
  public void warn(MigBatch batch, String step, String message) {
    write(batch, step, "WARN", message);
  }

  /**
   * Logs an error line.
   *
   * @param batch batch
   * @param step step
   * @param message message
   */
  public void error(MigBatch batch, String step, String message) {
    write(batch, step, "ERROR", message);
  }

  private void write(MigBatch batch, String step, String level, String message) {
    MigBatch.Counts c = batch.counts();
    String counts =
        "staged "
            + c.staged()
            + ", valid "
            + c.valid()
            + ", warning "
            + c.warning()
            + ", invalid "
            + c.invalid()
            + ", loaded "
            + c.loaded()
            + ", skipped "
            + c.skipped()
            + ", rejected "
            + c.rejected()
            + ", excluded "
            + c.excluded();
    logs.save(
        new BatchLog(
            batch.getId(), step, level, message, counts, currentUser.username(), clock.instant()));
  }
}
