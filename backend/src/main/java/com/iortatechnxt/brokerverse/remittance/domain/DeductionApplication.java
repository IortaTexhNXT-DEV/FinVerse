package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * The part of a remittance deduction consumed by one send cycle of a batch (ACSL 2.9.2), with the
 * journal of its {@code OPS_REMIT_DEDUCTION} posting; reversed when the batch's DV is cancelled
 * (DIS 2.20.0).
 */
@Entity
@Table(name = "rem_deduction_application")
public class DeductionApplication extends BaseEntity {

  @Column(name = "deduction_id", nullable = false, updatable = false)
  private Long deductionId;

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "batch_no", nullable = false, length = 40, updatable = false)
  private String batchNo;

  @Column(name = "send_cycle", nullable = false, updatable = false)
  private int sendCycle;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(nullable = false)
  private boolean reversed;

  @Column(name = "reversed_at")
  private Instant reversedAt;

  protected DeductionApplication() {}

  /**
   * A consumption.
   *
   * @param deductionId deduction
   * @param batch batch
   * @param amount amount consumed
   */
  public DeductionApplication(Long deductionId, RemittanceBatch batch, BigDecimal amount) {
    this.deductionId = deductionId;
    this.batchId = batch.getId();
    this.batchNo = batch.getBatchNo();
    this.sendCycle = batch.getSettlement().getSendCycle();
    this.amount = amount;
  }

  /**
   * Records the posting.
   *
   * @param journal journal batch number
   */
  public void posted(String journal) {
    this.journalBatchNo = journal;
  }

  /**
   * Records the reversal.
   *
   * @param at time
   */
  public void reverse(Instant at) {
    this.reversed = true;
    this.reversedAt = at;
  }

  public Long getDeductionId() {
    return deductionId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public int getSendCycle() {
    return sendCycle;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public boolean isReversed() {
    return reversed;
  }

  public Instant getReversedAt() {
    return reversedAt;
  }
}
