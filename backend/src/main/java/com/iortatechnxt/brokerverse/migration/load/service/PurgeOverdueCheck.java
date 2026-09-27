package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * {@code MIG_STAGING_PURGE_OVERDUE} (DATA_MIGRATION_DESIGN sections 6 and 20): staged payloads of a
 * batch still kept after its purge date, for example when the purge job did not run. Personal data
 * must not stay in staging beyond the retention days.
 */
@Component
public class PurgeOverdueCheck implements AlertCheck {

  private final JdbcTemplate jdbc;

  /**
   * Creates the check.
   *
   * @param jdbc JDBC
   */
  public PurgeOverdueCheck(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<AlertSignal> evaluate(LocalDate asOf) {
    return jdbc.query(
        "select company_id, batch_no, purge_due_on from mig_batch"
            + " where purged_at is null and purge_due_on < ? order by id",
        (rs, i) -> {
          String batchNo = rs.getString(2);
          return new AlertSignal(
              MigrationCodes.ALERT_PURGE_OVERDUE,
              new AlertFacts(
                  rs.getLong(1),
                  null,
                  MigrationCodes.ENTITY_BATCH,
                  batchNo,
                  "Staging data of batch "
                      + batchNo
                      + " is kept past its purge date "
                      + rs.getObject(3, LocalDate.class),
                  null,
                  MigrationCodes.ALERT_PURGE_OVERDUE + ":" + batchNo));
        },
        asOf);
  }
}
