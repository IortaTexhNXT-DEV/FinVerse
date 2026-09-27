package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A version of a rule set of one processing step, segment and business type (BRIDSP-08; design
 * section 4.2): drafted and submitted by the maker, activated from its effective date by a checker
 * who is not the maker, retired when a later version of the same code is activated.
 */
@Entity
@Table(name = "sbm_rule_set")
public class SbmRuleSet extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, updatable = false, length = 40)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private SbmStep step;

  @Column(length = 30)
  private String segment;

  @Column(name = "business_type", length = 2)
  private String businessType;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SbmRuleSetStatus status = SbmRuleSetStatus.DRAFT;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(length = 200)
  private String description;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "decision_remarks", length = 200)
  private String decisionRemarks;

  protected SbmRuleSet() {}

  /**
   * A draft version.
   *
   * @param companyId company
   * @param code rule set code
   * @param versionNo version
   * @param scope step, segment and business type
   * @param effectiveFrom effective date
   * @param description description
   */
  public SbmRuleSet(
      Long companyId,
      String code,
      int versionNo,
      Scope scope,
      LocalDate effectiveFrom,
      String description) {
    this.companyId = companyId;
    this.code = code;
    this.versionNo = versionNo;
    this.step = scope.step();
    this.segment = scope.segment();
    this.businessType = scope.businessType();
    this.effectiveFrom = effectiveFrom;
    this.description = description;
  }

  /**
   * Changes the header of a draft.
   *
   * @param newEffectiveFrom effective date
   * @param newDescription description
   */
  public void describe(LocalDate newEffectiveFrom, String newDescription) {
    requireDraft();
    this.effectiveFrom = newEffectiveFrom;
    this.description = newDescription;
  }

  /**
   * Submits the draft for approval.
   *
   * @param by maker
   * @param at time
   */
  public void submit(String by, Instant at) {
    requireDraft();
    this.status = SbmRuleSetStatus.SUBMITTED;
    this.submittedBy = by;
    this.submittedAt = at;
  }

  /**
   * Approves the version (not by its maker).
   *
   * @param by checker
   * @param at time
   * @param remarks remarks, may be null
   */
  public void approve(String by, Instant at, String remarks) {
    requireSubmitted();
    if (Objects.equals(by, submittedBy)) {
      throw new BusinessRuleException(
          "SBM_RULE_SET_MAKER", "A rule set is approved by someone other than its maker");
    }
    this.status = SbmRuleSetStatus.ACTIVE;
    this.approvedBy = by;
    this.approvedAt = at;
    this.decisionRemarks = remarks;
  }

  /**
   * Returns the version to its maker as a draft with the reason.
   *
   * @param by checker
   * @param at time
   * @param reason reason
   */
  public void reject(String by, Instant at, String reason) {
    requireSubmitted();
    if (Objects.equals(by, submittedBy)) {
      throw new BusinessRuleException(
          "SBM_RULE_SET_MAKER", "A rule set is approved by someone other than its maker");
    }
    this.status = SbmRuleSetStatus.DRAFT;
    this.approvedBy = by;
    this.approvedAt = at;
    this.decisionRemarks = reason;
  }

  /** Retires the version when a later one is activated. */
  public void retire() {
    this.status = SbmRuleSetStatus.RETIRED;
  }

  /** Throws unless the version is a draft. */
  public void requireDraft() {
    if (status != SbmRuleSetStatus.DRAFT) {
      throw new BusinessRuleException(
          "SBM_RULE_SET_NOT_DRAFT", "Only a draft rule set can be changed");
    }
  }

  private void requireSubmitted() {
    if (status != SbmRuleSetStatus.SUBMITTED) {
      throw new BusinessRuleException(
          "SBM_RULE_SET_NOT_SUBMITTED", "The rule set is not waiting for approval");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCode() {
    return code;
  }

  public SbmStep getStep() {
    return step;
  }

  public String getSegment() {
    return segment;
  }

  public String getBusinessType() {
    return businessType;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public SbmRuleSetStatus getStatus() {
    return status;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public String getDescription() {
    return description;
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

  public String getDecisionRemarks() {
    return decisionRemarks;
  }

  /**
   * What a rule set applies to.
   *
   * @param step processing step
   * @param segment segment, null for every segment
   * @param businessType NB or RB, null for both
   */
  public record Scope(SbmStep step, String segment, String businessType) {}
}
