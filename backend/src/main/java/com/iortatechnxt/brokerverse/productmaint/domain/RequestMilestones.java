package com.iortatechnxt.brokerverse.productmaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * Who moved a package request through its business steps and when (BRPM.021/024): submission,
 * Marketing approval, TSU recommendation and approval, terms final, requirements and MBS set-up.
 * The four-eyes checks of the services read these (approver never the maker or submitter, TSU Head
 * never the recommender).
 */
@Embeddable
public class RequestMilestones {

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "recommended_by", length = 50)
  private String recommendedBy;

  @Column(name = "recommended_at")
  private Instant recommendedAt;

  @Column(name = "tsu_approved_by", length = 50)
  private String tsuApprovedBy;

  @Column(name = "tsu_approved_at")
  private Instant tsuApprovedAt;

  @Column(name = "terms_final_by", length = 50)
  private String termsFinalBy;

  @Column(name = "terms_final_at")
  private Instant termsFinalAt;

  @Column(name = "requirements_by", length = 50)
  private String requirementsBy;

  @Column(name = "requirements_at")
  private Instant requirementsAt;

  @Column(name = "setup_by", length = 50)
  private String setupBy;

  @Column(name = "setup_at")
  private Instant setupAt;

  /**
   * Marks the submission for Marketing approval.
   *
   * @param user submitter
   * @param when time
   */
  public void submitted(String user, Instant when) {
    this.submittedBy = user;
    this.submittedAt = when;
  }

  /**
   * Marks the Marketing approval (BRPM.008).
   *
   * @param user approver
   * @param when time
   */
  public void approved(String user, Instant when) {
    this.approvedBy = user;
    this.approvedAt = when;
  }

  /**
   * Marks the TSU Team Lead recommendation (BRPM.009).
   *
   * @param user recommender
   * @param when time
   */
  public void recommended(String user, Instant when) {
    this.recommendedBy = user;
    this.recommendedAt = when;
  }

  /**
   * Marks the TSU Head approval (BRPM.009).
   *
   * @param user approver
   * @param when time
   */
  public void tsuApproved(String user, Instant when) {
    this.tsuApprovedBy = user;
    this.tsuApprovedAt = when;
  }

  /**
   * Marks the negotiated terms final (BRPM.010).
   *
   * @param user user
   * @param when time
   */
  public void termsFinal(String user, Instant when) {
    this.termsFinalBy = user;
    this.termsFinalAt = when;
  }

  /**
   * Marks the requirements pack submitted to ManCom (BRPM.015).
   *
   * @param user user
   * @param when time
   */
  public void requirementsSubmitted(String user, Instant when) {
    this.requirementsBy = user;
    this.requirementsAt = when;
  }

  /**
   * Marks the MBS set-up (BRPM.015).
   *
   * @param user MBS user
   * @param when time
   */
  public void setUp(String user, Instant when) {
    this.setupBy = user;
    this.setupAt = when;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public String getRecommendedBy() {
    return recommendedBy;
  }

  public Instant getRecommendedAt() {
    return recommendedAt;
  }

  public String getTsuApprovedBy() {
    return tsuApprovedBy;
  }

  public Instant getTsuApprovedAt() {
    return tsuApprovedAt;
  }

  public String getTermsFinalBy() {
    return termsFinalBy;
  }

  public Instant getTermsFinalAt() {
    return termsFinalAt;
  }

  public String getRequirementsBy() {
    return requirementsBy;
  }

  public Instant getRequirementsAt() {
    return requirementsAt;
  }

  public String getSetupBy() {
    return setupBy;
  }

  public Instant getSetupAt() {
    return setupAt;
  }
}
