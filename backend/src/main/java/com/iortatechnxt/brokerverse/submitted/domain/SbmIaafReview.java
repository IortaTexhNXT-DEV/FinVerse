package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** A policy review (BRIDSP-06): number, date, reviewer, adequacy, findings and remarks. */
@Entity
@Table(name = "sbm_iaaf_review")
public class SbmIaafReview extends BaseEntity {

  /** Adequate. */
  public static final String ADEQUATE = "ADEQUATE";

  /** With findings. */
  public static final String WITH_FINDINGS = "WITH_FINDINGS";

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(name = "iaaf_id")
  private Long iaafId;

  @Column(name = "review_no", nullable = false, updatable = false)
  private int reviewNo;

  @Column(name = "review_date", nullable = false, updatable = false)
  private LocalDate reviewDate;

  @Column(nullable = false, updatable = false, length = 50)
  private String reviewer;

  @Column(nullable = false, updatable = false, length = 20)
  private String adequacy;

  @Column(updatable = false, length = 500)
  private String findings;

  @Column(updatable = false, length = 1000)
  private String remarks;

  @Column(name = "sent_to", length = 300)
  private String sentTo;

  @Column(name = "sent_at")
  private Instant sentAt;

  protected SbmIaafReview() {}

  /**
   * A review.
   *
   * @param policyId policy
   * @param reviewNo number
   * @param reviewer reviewer
   * @param content date, adequacy, findings and remarks
   */
  public SbmIaafReview(Long policyId, int reviewNo, String reviewer, Content content) {
    this.policyId = policyId;
    this.reviewNo = reviewNo;
    this.reviewer = reviewer;
    this.reviewDate = content.reviewDate();
    this.adequacy = content.adequacy();
    this.findings =
        content.findings() == null || content.findings().isEmpty()
            ? null
            : String.join(",", content.findings());
    this.remarks = content.remarks();
  }

  /**
   * Links the review to the IAAF generated after it.
   *
   * @param iaaf IAAF
   */
  public void iaaf(Long iaaf) {
    this.iaafId = iaaf;
  }

  /**
   * The findings were sent to the bank counterpart.
   *
   * @param to recipients
   * @param at time
   */
  public void sent(String to, Instant at) {
    this.sentTo = to;
    this.sentAt = at;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public Long getIaafId() {
    return iaafId;
  }

  public int getReviewNo() {
    return reviewNo;
  }

  public LocalDate getReviewDate() {
    return reviewDate;
  }

  public String getReviewer() {
    return reviewer;
  }

  public String getAdequacy() {
    return adequacy;
  }

  /**
   * The findings.
   *
   * @return finding codes
   */
  public List<String> findingList() {
    return findings == null ? List.of() : Arrays.asList(findings.split(","));
  }

  public String getRemarks() {
    return remarks;
  }

  public String getSentTo() {
    return sentTo;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  /**
   * The content of a review.
   *
   * @param reviewDate date
   * @param adequacy ADEQUATE or WITH_FINDINGS
   * @param findings finding codes (LOV SBM_IAAF_FINDING)
   * @param remarks remarks
   */
  public record Content(
      LocalDate reviewDate, String adequacy, List<String> findings, String remarks) {

    /** Defensive copy. */
    public Content {
      findings = findings == null ? List.of() : List.copyOf(findings);
    }
  }
}
