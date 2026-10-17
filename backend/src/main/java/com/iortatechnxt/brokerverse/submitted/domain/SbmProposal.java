package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A renewal proposal of a record (FR-SP-066, 067): one per record and version in a batch, with the
 * default and the chosen insurer, the nominated and the applied rate (with the reason of a change)
 * and the premium. For Review until the Team Lead releases it or returns it with a reason; a new
 * version supersedes it.
 */
@Entity
@Table(name = "sbm_proposal")
public class SbmProposal extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Column(name = "default_insurer", length = 30, updatable = false)
  private String defaultInsurer;

  @Column(name = "insurer_code", nullable = false, length = 30)
  private String insurerCode;

  @Column(name = "nominated_rate", precision = 9, scale = 6)
  private BigDecimal nominatedRate;

  @Column(name = "applied_rate", nullable = false, precision = 9, scale = 6)
  private BigDecimal appliedRate;

  @Column(name = "rate_reason", length = 250)
  private String rateReason;

  @Column(name = "sum_insured", precision = 19, scale = 2, updatable = false)
  private BigDecimal sumInsured;

  @Column(precision = 19, scale = 2)
  private BigDecimal premium;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.FOR_REVIEW;

  @Column(name = "return_reason", length = 250)
  private String returnReason;

  @Column(name = "released_by", length = 50)
  private String releasedBy;

  @Column(name = "released_at")
  private Instant releasedAt;

  protected SbmProposal() {}

  /**
   * A proposal For Review.
   *
   * @param batch batch
   * @param policy record
   * @param versionNo version of the record's proposal
   * @param terms insurer, rates and premium
   */
  public SbmProposal(SbmProposalBatch batch, SbmPolicy policy, int versionNo, Terms terms) {
    this.companyId = batch.getCompanyId();
    this.batchId = batch.getId();
    this.policyId = policy.getId();
    this.versionNo = versionNo;
    this.defaultInsurer = terms.defaultInsurer();
    this.sumInsured = policy.getTerms().sumInsured();
    price(terms);
  }

  /**
   * Assigns another insurer with its nominated rate (FR-SP-067).
   *
   * @param terms chosen insurer and its rates
   */
  public void assign(Terms terms) {
    requireForReview();
    price(terms);
  }

  private void price(Terms terms) {
    this.insurerCode = terms.insurerCode();
    this.nominatedRate = terms.nominatedRate();
    this.appliedRate = terms.appliedRate();
    this.rateReason = terms.reason();
    this.premium = terms.premium();
  }

  /**
   * Releases the proposal (someone other than its maker).
   *
   * @param by Team Lead
   * @param at time
   */
  public void release(String by, Instant at) {
    requireForReview();
    if (by.equals(getCreatedBy())) {
      throw new BusinessRuleException(
          "SBM_PROPOSAL_MAKER", "A proposal is released by someone other than its maker");
    }
    this.status = Status.RELEASED;
    this.releasedBy = by;
    this.releasedAt = at;
  }

  /**
   * Returns the proposal to its maker with a reason.
   *
   * @param reason reason
   */
  public void returned(String reason) {
    if (status != Status.FOR_REVIEW && status != Status.RELEASED) {
      throw new BusinessRuleException("SBM_PROPOSAL_STATUS", "The proposal cannot be returned");
    }
    this.status = Status.RETURNED;
    this.returnReason = reason;
    this.releasedBy = null;
    this.releasedAt = null;
  }

  /** Marks the proposal as replaced by a newer version. */
  public void supersede() {
    this.status = Status.SUPERSEDED;
  }

  private void requireForReview() {
    if (status != Status.FOR_REVIEW) {
      throw new BusinessRuleException("SBM_PROPOSAL_STATUS", "The proposal is not for review");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public String getDefaultInsurer() {
    return defaultInsurer;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public BigDecimal getNominatedRate() {
    return nominatedRate;
  }

  public BigDecimal getAppliedRate() {
    return appliedRate;
  }

  public String getRateReason() {
    return rateReason;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public Status getStatus() {
    return status;
  }

  public String getReturnReason() {
    return returnReason;
  }

  public String getReleasedBy() {
    return releasedBy;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  /** Status of a proposal. */
  public enum Status {
    /** Waiting for the Team Lead. */
    FOR_REVIEW,
    /** Released to the hand-off and the letters. */
    RELEASED,
    /** Returned to the maker with a reason. */
    RETURNED,
    /** Replaced by a newer version. */
    SUPERSEDED
  }

  /**
   * Insurer and price of a proposal.
   *
   * @param defaultInsurer insurer the rules give, may be null
   * @param insurerCode insurer chosen
   * @param nominatedRate nominated rate of the insurer, null when none
   * @param appliedRate rate applied
   * @param reason reason of a rate other than the nominated one, may be null
   * @param premium premium at the applied rate
   */
  public record Terms(
      String defaultInsurer,
      String insurerCode,
      BigDecimal nominatedRate,
      BigDecimal appliedRate,
      String reason,
      BigDecimal premium) {}
}
