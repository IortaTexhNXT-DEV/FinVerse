package com.iortatechnxt.brokerverse.screening.cases.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The review of a case on its template (SNSRP-501; FR-SS-050): the template version fixed when the
 * review started (SNSRP-104 "applies to new reviews only") and its status. Answers are {@link
 * CaseAnswer} rows.
 */
@Entity
@Table(name = "scr_case_review")
public class CaseReview extends BaseEntity {

  @Column(name = "case_id", nullable = false, updatable = false)
  private Long caseId;

  @Column(name = "template_version_id", nullable = false, updatable = false)
  private Long templateVersionId;

  @Column(name = "template_type", nullable = false, length = 30, updatable = false)
  private String templateType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ReviewStatus status;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  /** For JPA. */
  protected CaseReview() {}

  /**
   * Starts a review.
   *
   * @param caseId the case
   * @param templateVersionId the TEMPLATE version
   * @param templateType the template type
   */
  public CaseReview(Long caseId, Long templateVersionId, String templateType) {
    this.caseId = caseId;
    this.templateVersionId = templateVersionId;
    this.templateType = templateType;
    this.status = ReviewStatus.DRAFT;
  }

  /**
   * Submits the review with the case.
   *
   * @param by investigator
   * @param at when
   */
  public void submit(String by, Instant at) {
    this.status = ReviewStatus.SUBMITTED;
    this.submittedBy = by;
    this.submittedAt = at;
  }

  /** Re-opens the review when the case is returned or re-opened. */
  public void reopen() {
    this.status = ReviewStatus.DRAFT;
  }

  public Long getCaseId() {
    return caseId;
  }

  public Long getTemplateVersionId() {
    return templateVersionId;
  }

  public String getTemplateType() {
    return templateType;
  }

  public ReviewStatus getStatus() {
    return status;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }
}
