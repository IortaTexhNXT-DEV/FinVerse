package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * An account of an {@link InsurerBatch} with the snapshot of the 28 columns sent (BRD 3.009.1.4),
 * kept as JSON.
 */
@Entity
@Table(name = "rnw_insurer_batch_line")
public class InsurerBatchLine extends BaseEntity {

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(nullable = false, length = 4000, updatable = false)
  private String snapshot;

  @Column(nullable = false)
  private boolean responded;

  protected InsurerBatchLine() {}

  /**
   * Adds an account to a batch.
   *
   * @param batchId batch
   * @param candidateId candidate
   * @param snapshot the columns sent, as JSON
   */
  public InsurerBatchLine(Long batchId, Long candidateId, String snapshot) {
    this.batchId = batchId;
    this.candidateId = candidateId;
    this.snapshot = snapshot;
  }

  /** Marks the account answered. */
  public void respond() {
    this.responded = true;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getSnapshot() {
    return snapshot;
  }

  public boolean isResponded() {
    return responded;
  }
}
