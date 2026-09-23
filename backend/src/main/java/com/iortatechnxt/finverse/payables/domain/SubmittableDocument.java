package com.iortatechnxt.finverse.payables.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Approvable document with an explicit draft stage: the maker saves drafts, then submits them to
 * the approval queue.
 */
@MappedSuperclass
public abstract class SubmittableDocument extends ApprovableDocument {

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  /**
   * Records the submission.
   *
   * @param user submitting user
   * @param when timestamp
   */
  protected void recordSubmission(String user, Instant when) {
    this.submittedBy = user;
    this.submittedAt = when;
  }

  /** Clears the submission when the document returns to draft. */
  protected void clearSubmission() {
    this.submittedBy = null;
    this.submittedAt = null;
  }

  @Override
  public String maker() {
    return submittedBy != null ? submittedBy : getCreatedBy();
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }
}
