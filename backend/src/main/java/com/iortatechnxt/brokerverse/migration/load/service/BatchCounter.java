package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRowRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Counts the rows of a batch by status and computes its error rate (DATA_MIGRATION_DESIGN section
 * 8): (INVALID rows not waived + waived rows) / (staged rows - EXCLUDED rows), in percent. Rows the
 * data owner excluded are neither errors nor part of the base.
 */
@Component
public class BatchCounter {

  private static final int RATE_SCALE = 4;
  private static final BigDecimal HUNDRED = new BigDecimal("100");

  private final StageRowRepository rows;
  private final JdbcTemplate jdbc;
  private final EntityManager em;

  /**
   * Creates the counter.
   *
   * @param rows staged rows
   * @param jdbc JDBC (waived rows)
   * @param em entity manager (flush before the count)
   */
  public BatchCounter(StageRowRepository rows, JdbcTemplate jdbc, EntityManager em) {
    this.rows = rows;
    this.jdbc = jdbc;
    this.em = em;
  }

  /**
   * Recounts a batch.
   *
   * @param batch batch
   */
  public void recount(MigBatch batch) {
    MigBatch.Counts c = count(batch.getId());
    batch.recount(c, rate(c));
  }

  /**
   * The counts of a batch.
   *
   * @param batchId batch
   * @return counts
   */
  public MigBatch.Counts count(Long batchId) {
    em.flush();
    Map<RowStatus, Integer> by = new EnumMap<>(RowStatus.class);
    for (Object[] row : rows.countByStatus(batchId)) {
      by.put((RowStatus) row[0], ((Number) row[1]).intValue());
    }
    int staged = by.values().stream().mapToInt(Integer::intValue).sum();
    Integer waived =
        jdbc.queryForObject(
            "select count(distinct i.stage_row_id) from mig_issue i join mig_stage_row r on r.id = i.stage_row_id"
                + " where r.batch_id = ? and i.resolution = 'WAIVED' and i.severity = 'ERROR'"
                + " and r.status <> 'EXCLUDED'",
            Integer.class,
            batchId);
    return new MigBatch.Counts(
        staged,
        get(by, RowStatus.VALID),
        get(by, RowStatus.WARNING),
        get(by, RowStatus.INVALID),
        get(by, RowStatus.LOADED),
        get(by, RowStatus.SKIPPED),
        get(by, RowStatus.REJECTED),
        get(by, RowStatus.EXCLUDED),
        waived == null ? 0 : waived);
  }

  /**
   * The error rate of the counts, in percent.
   *
   * @param c counts
   * @return rate
   */
  public static BigDecimal rate(MigBatch.Counts c) {
    int base = c.staged() - c.excluded();
    if (base <= 0) {
      return BigDecimal.ZERO.setScale(RATE_SCALE);
    }
    return BigDecimal.valueOf((long) c.invalid() + c.waived())
        .multiply(HUNDRED)
        .divide(BigDecimal.valueOf(base), RATE_SCALE, RoundingMode.HALF_UP);
  }

  private static int get(Map<RowStatus, Integer> by, RowStatus s) {
    return by.getOrDefault(s, 0);
  }
}
