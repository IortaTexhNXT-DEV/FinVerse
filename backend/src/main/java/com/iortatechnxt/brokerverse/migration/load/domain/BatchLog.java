package com.iortatechnxt.brokerverse.migration.load.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A step of the run log of a batch (DATA_MIGRATION_DESIGN section 11): plan, validation, matching,
 * approval, load, reconciliation, sign-off, rollback and purge, with counts and time.
 */
@Entity
@Table(name = "mig_batch_log")
public class BatchLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(nullable = false, length = 12, updatable = false)
  private String step;

  @Column(nullable = false, length = 8, updatable = false)
  private String level;

  @Column(nullable = false, length = 2000, updatable = false)
  private String message;

  @Column(length = 500, updatable = false)
  private String counts;

  @Column(name = "logged_by", nullable = false, length = 50, updatable = false)
  private String loggedBy;

  @Column(name = "logged_at", nullable = false, updatable = false)
  private Instant loggedAt;

  protected BatchLog() {}

  /**
   * A log line.
   *
   * @param batchId batch
   * @param step step
   * @param level INFO, WARN or ERROR
   * @param message message
   * @param counts counts summary
   * @param user user
   * @param when time
   */
  public BatchLog(
      Long batchId,
      String step,
      String level,
      String message,
      String counts,
      String user,
      Instant when) {
    this.batchId = batchId;
    this.step = step;
    this.level = level;
    this.message = message.length() > 2000 ? message.substring(0, 2000) : message;
    this.counts = counts;
    this.loggedBy = user;
    this.loggedAt = when;
  }

  public Long getId() {
    return id;
  }

  public Long getBatchId() {
    return batchId;
  }

  public String getStep() {
    return step;
  }

  public String getLevel() {
    return level;
  }

  public String getMessage() {
    return message;
  }

  public String getCounts() {
    return counts;
  }

  public String getLoggedBy() {
    return loggedBy;
  }

  public Instant getLoggedAt() {
    return loggedAt;
  }
}
