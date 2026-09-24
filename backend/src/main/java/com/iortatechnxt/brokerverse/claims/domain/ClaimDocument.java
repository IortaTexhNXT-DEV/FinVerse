package com.iortatechnxt.brokerverse.claims.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Base of the claim documents that change money (reserve change, settlement, recovery): the claim,
 * the maker-checker state and the journal that accounted for the approval.
 */
@MappedSuperclass
public abstract class ClaimDocument extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claim_id")
  private Claim claim;

  @Embedded private ClaimApproval approval;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  protected ClaimDocument() {}

  /**
   * Creates a document.
   *
   * @param claim claim
   * @param approval initial maker-checker state
   */
  protected ClaimDocument(Claim claim, ClaimApproval approval) {
    this.claim = claim;
    this.approval = approval;
  }

  /**
   * Label used in messages and the audit trail.
   *
   * @return e.g. "Settlement CS-HO-2026-000001"
   */
  public abstract String label();

  /**
   * Checker approves the document (accounting is performed by the service in the same transaction).
   *
   * @param checker approving user
   * @param when time
   * @param date accounting date
   */
  public void approve(String checker, Instant when, LocalDate date) {
    approval.approve(label(), checker, when, date);
  }

  /**
   * Checker rejects the document.
   *
   * @param checker rejecting user
   * @param when time
   * @param reason reason
   */
  public void reject(String checker, Instant when, String reason) {
    approval.reject(label(), checker, when, reason);
  }

  /**
   * Records the journal that accounted for the approval.
   *
   * @param batchNo journal batch number
   */
  public void recordJournal(String batchNo) {
    this.journalBatchNo = batchNo;
  }

  public Claim getClaim() {
    return claim;
  }

  public ClaimApproval getApproval() {
    return approval;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }
}
