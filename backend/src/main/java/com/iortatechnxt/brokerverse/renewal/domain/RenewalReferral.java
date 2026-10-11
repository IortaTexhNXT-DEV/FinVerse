package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A transfer request for a New Business opportunity (BDOI Renewal FRS FRRN.011): a referral of a
 * renewal account to another Marketing unit, which accepts it and opens a New Business account from
 * the copied data; the renewal account keeps its unit and Account Officer.
 */
@Entity
@Table(name = "rnw_referral")
public class RenewalReferral extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "referral_no", nullable = false, length = 30, updatable = false)
  private String referralNo;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(name = "from_unit", length = 20, updatable = false)
  private String fromUnit;

  @Column(name = "to_unit", nullable = false, length = 20, updatable = false)
  private String toUnit;

  @Column(nullable = false, length = 1000)
  private String justification;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ReferralStatus status;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_remarks", length = 1000)
  private String decisionRemarks;

  @Column(name = "nb_arn", length = 30)
  private String nbArn;

  @Column(name = "nb_created_by", length = 50)
  private String nbCreatedBy;

  @Column(name = "nb_created_at")
  private Instant nbCreatedAt;

  protected RenewalReferral() {}

  /**
   * A new request.
   *
   * @param candidate renewal account referred
   * @param referralNo Transfer Request Number
   * @param toUnit receiving Marketing unit
   * @param justification business justification
   * @param submit true to submit (Pending Acceptance), false to keep as Draft
   */
  public RenewalReferral(
      RenewalCandidate candidate,
      String referralNo,
      String toUnit,
      String justification,
      boolean submit) {
    this.companyId = candidate.getCompanyId();
    this.candidateId = candidate.getId();
    this.fromUnit = candidate.getOwnerUnit();
    this.referralNo = referralNo;
    this.toUnit = toUnit;
    this.justification = justification;
    this.status = submit ? ReferralStatus.PENDING_ACCEPTANCE : ReferralStatus.DRAFT;
  }

  /**
   * Submits a draft or a request returned for clarification.
   *
   * @param newJustification justification, possibly clarified
   */
  public void submit(String newJustification) {
    require(ReferralStatus.DRAFT, ReferralStatus.RETURNED);
    this.justification = newJustification;
    this.status = ReferralStatus.PENDING_ACCEPTANCE;
  }

  /**
   * Records the decision of the receiving unit.
   *
   * @param outcome ACCEPTED, REJECTED or RETURNED
   * @param user deciding user
   * @param remarks remarks (required to reject or return)
   * @param at time
   */
  public void decide(ReferralStatus outcome, String user, String remarks, Instant at) {
    require(ReferralStatus.PENDING_ACCEPTANCE);
    this.status = outcome;
    this.decidedBy = user;
    this.decisionRemarks = remarks;
    this.decidedAt = at;
  }

  /** Withdraws the request (the requester). */
  public void cancel() {
    require(ReferralStatus.DRAFT, ReferralStatus.PENDING_ACCEPTANCE, ReferralStatus.RETURNED);
    this.status = ReferralStatus.CANCELLED;
  }

  /**
   * Links the New Business account created from the request.
   *
   * @param arn account reference
   * @param user creating user
   * @param at time
   */
  public void linkNewBusiness(String arn, String user, Instant at) {
    require(ReferralStatus.ACCEPTED);
    if (nbArn != null) {
      throw new BusinessRuleException(
          "RNW_REFERRAL_NB_EXISTS", "New Business account " + nbArn + " is already created");
    }
    this.nbArn = arn;
    this.nbCreatedBy = user;
    this.nbCreatedAt = at;
  }

  private void require(ReferralStatus... allowed) {
    for (ReferralStatus s : allowed) {
      if (status == s) {
        return;
      }
    }
    throw new BusinessRuleException(
        "RNW_REFERRAL_STATUS", "Transfer request " + referralNo + " is " + status.label());
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getReferralNo() {
    return referralNo;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getFromUnit() {
    return fromUnit;
  }

  public String getToUnit() {
    return toUnit;
  }

  public String getJustification() {
    return justification;
  }

  public ReferralStatus getStatus() {
    return status;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionRemarks() {
    return decisionRemarks;
  }

  public String getNbArn() {
    return nbArn;
  }

  public String getNbCreatedBy() {
    return nbCreatedBy;
  }

  public Instant getNbCreatedAt() {
    return nbCreatedAt;
  }
}
