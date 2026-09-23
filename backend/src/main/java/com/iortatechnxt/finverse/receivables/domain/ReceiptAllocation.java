package com.iortatechnxt.finverse.receivables.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Part of a receipt applied to one debit open item (debit note). The sub-ledger match is recorded
 * when the receipt is approved (or when unapplied money is applied later).
 */
@Entity
@Table(name = "rcv_receipt_allocation")
public class ReceiptAllocation extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "receipt_id", nullable = false)
  private Receipt receipt;

  @Column(name = "debit_item_id", nullable = false)
  private Long debitItemId;

  @Column(name = "document_no", nullable = false, length = 40)
  private String documentNo;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "match_id")
  private Long matchId;

  @Column(name = "applied_on")
  private LocalDate appliedOn;

  protected ReceiptAllocation() {}

  ReceiptAllocation(Receipt receipt, Long debitItemId, String documentNo, BigDecimal amount) {
    this.receipt = receipt;
    this.debitItemId = debitItemId;
    this.documentNo = documentNo;
    this.amount = amount;
  }

  /**
   * Records the sub-ledger match that applied this allocation.
   *
   * @param id match id
   * @param date application date
   */
  public void matched(Long id, LocalDate date) {
    this.matchId = id;
    this.appliedOn = date;
  }

  /** Clears the match after it was undone (cancelled or bounced receipt). */
  public void unmatched() {
    this.matchId = null;
  }

  public Receipt getReceipt() {
    return receipt;
  }

  public Long getDebitItemId() {
    return debitItemId;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Long getMatchId() {
    return matchId;
  }

  public LocalDate getAppliedOn() {
    return appliedOn;
  }
}
