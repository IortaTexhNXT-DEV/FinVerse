package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * A document approved level by level by the approval matrix (IAAF and Terms of Reference,
 * BRIDSP-07, 18): submitted with the number of levels of its matrix band, approved level by level
 * until the last, or returned to its preparer with a reason.
 */
@MappedSuperclass
public abstract class SbmApprovable extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SbmDocStatus status = SbmDocStatus.DRAFT;

  @Column(name = "current_level", nullable = false)
  private int currentLevel;

  @Column(name = "total_levels", nullable = false)
  private int totalLevels;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(name = "return_reason", length = 500)
  private String returnReason;

  protected SbmApprovable() {}

  /**
   * A new draft.
   *
   * @param companyId company
   */
  protected SbmApprovable(Long companyId) {
    this.companyId = companyId;
  }

  /**
   * The number of the document (IAAF or TOR number).
   *
   * @return number
   */
  public abstract String number();

  /**
   * Submits the draft (or the returned document) to its first level.
   *
   * @param levels levels of its matrix band
   * @param at time
   */
  public void submit(int levels, Instant at) {
    require(SbmDocStatus.DRAFT, SbmDocStatus.RETURNED);
    this.status = SbmDocStatus.FOR_APPROVAL;
    this.currentLevel = 1;
    this.totalLevels = levels;
    this.submittedAt = at;
    this.returnReason = null;
  }

  /**
   * Approves the current level.
   *
   * @return true when it was the last level (the document is APPROVED)
   */
  public boolean approveLevel() {
    require(SbmDocStatus.FOR_APPROVAL);
    if (currentLevel >= totalLevels) {
      this.status = SbmDocStatus.APPROVED;
      return true;
    }
    currentLevel++;
    return false;
  }

  /**
   * Returns the document to its preparer.
   *
   * @param reason reason
   */
  public void returned(String reason) {
    require(SbmDocStatus.FOR_APPROVAL);
    this.status = SbmDocStatus.RETURNED;
    this.currentLevel = 0;
    this.returnReason = reason;
  }

  /** Cancels a draft or returned document. */
  public void cancel() {
    require(SbmDocStatus.DRAFT, SbmDocStatus.RETURNED);
    this.status = SbmDocStatus.CANCELLED;
  }

  /**
   * Ends the document: ISSUED (IAAF) or RELEASED (TOR).
   *
   * @param end ISSUED or RELEASED
   */
  protected void end(SbmDocStatus end) {
    require(SbmDocStatus.APPROVED);
    this.status = end;
  }

  /**
   * Keeps the signed PDF.
   *
   * @param attachment attachment of the PDF
   */
  public void signedPdf(Long attachment) {
    this.attachmentId = attachment;
  }

  /**
   * Throws unless the document is in one of the statuses.
   *
   * @param allowed statuses
   */
  public void require(SbmDocStatus... allowed) {
    for (SbmDocStatus s : allowed) {
      if (s == status) {
        return;
      }
    }
    throw new BusinessRuleException(
        "SBM_DOCUMENT_STATUS", number() + " is " + DisplayFormat.words(status));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public SbmDocStatus getStatus() {
    return status;
  }

  public int getCurrentLevel() {
    return currentLevel;
  }

  public int getTotalLevels() {
    return totalLevels;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getReturnReason() {
    return returnReason;
  }
}
