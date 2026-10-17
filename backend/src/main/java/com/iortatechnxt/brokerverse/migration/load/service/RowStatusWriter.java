package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Writes the outcome of a unit on its staged rows in the chunk transaction (staging is a table of
 * this module; the rows of a unit are updated together).
 */
@Component
public class RowStatusWriter {

  private static final int MAX_MESSAGE = 1000;

  private final JdbcTemplate jdbc;

  /**
   * Creates the writer.
   *
   * @param jdbc JDBC
   */
  public RowStatusWriter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The rows were loaded into a record.
   *
   * @param rows rows
   * @param target record
   * @param when time
   */
  public void loaded(List<StageRow> rows, KeyXref.Target target, Instant when) {
    for (StageRow r : rows) {
      jdbc.update(
          "update mig_stage_row set status = 'LOADED', target_entity = ?, target_id = ?, target_code = ?,"
              + " loaded_at = ?, message = null, version = version + 1 where id = ?",
          target.entity(),
          target.id(),
          target.code(),
          Timestamp.from(when),
          r.getId());
    }
  }

  /**
   * The rows were skipped or rejected.
   *
   * @param rows rows
   * @param status SKIPPED, REJECTED or ROLLED_BACK
   * @param message reason
   */
  public void finish(List<StageRow> rows, RowStatus status, String message) {
    String text =
        message == null || message.length() <= MAX_MESSAGE
            ? message
            : message.substring(0, MAX_MESSAGE);
    for (StageRow r : rows) {
      jdbc.update(
          "update mig_stage_row set status = ?, message = ?, version = version + 1 where id = ?",
          status.name(),
          text,
          r.getId());
    }
  }
}
