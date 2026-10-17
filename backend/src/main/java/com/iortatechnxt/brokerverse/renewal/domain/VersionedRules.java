package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A versioned rule set with maker-checker (bucket rule sets and decision matrices, BRRN.023/034;
 * RENEWAL_DESIGN section 4.2): drafted and submitted by the maker, activated from its effective
 * date by a checker who is not the maker, retired when a later version is activated.
 */
@MappedSuperclass
public abstract class VersionedRules extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RuleSetStatus status = RuleSetStatus.DRAFT;

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

  protected VersionedRules() {}

  /**
   * Starts a draft version.
   *
   * @param companyId company
   * @param versionNo version number
   * @param effectiveFrom effective date
   * @param description description
   */
  protected VersionedRules(
      Long companyId, int versionNo, LocalDate effectiveFrom, String description) {
    this.companyId = companyId;
    this.versionNo = versionNo;
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
   * Submits the draft to a checker.
   *
   * @param user maker
   * @param at time
   */
  public void submit(String user, Instant at) {
    requireDraft();
    this.status = RuleSetStatus.SUBMITTED;
    this.submittedBy = user;
    this.submittedAt = at;
  }

  /**
   * Activates the submitted version (checker, never the maker).
   *
   * @param checker checker
   * @param at time
   */
  public void activate(String checker, Instant at) {
    requireSubmitted();
    if (Objects.equals(checker, submittedBy)) {
      throw new BusinessRuleException(
          "RNW_RULES_MAKER_CHECKER", "A version is activated by someone other than its maker");
    }
    this.status = RuleSetStatus.ACTIVE;
    this.approvedBy = checker;
    this.approvedAt = at;
  }

  /**
   * Rejects the submitted version back to the maker.
   *
   * @param checker checker
   * @param remarks reason
   * @param at time
   */
  public void reject(String checker, String remarks, Instant at) {
    requireSubmitted();
    this.status = RuleSetStatus.REJECTED;
    this.approvedBy = checker;
    this.approvedAt = at;
    this.decisionRemarks = remarks;
  }

  /** Retires the active version when a later one is activated. */
  public void retire() {
    this.status = RuleSetStatus.RETIRED;
  }

  /** Refuses a change unless the version is a draft. */
  protected void requireDraft() {
    if (status != RuleSetStatus.DRAFT) {
      throw new BusinessRuleException(
          "RNW_RULES_NOT_DRAFT", "Version " + versionNo + " is " + status + " and cannot change");
    }
  }

  private void requireSubmitted() {
    if (status != RuleSetStatus.SUBMITTED) {
      throw new BusinessRuleException(
          "RNW_RULES_NOT_SUBMITTED", "Version " + versionNo + " is not waiting for a checker");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public RuleSetStatus getStatus() {
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
}
