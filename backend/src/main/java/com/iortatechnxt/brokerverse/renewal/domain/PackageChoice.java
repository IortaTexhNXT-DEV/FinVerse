package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * The BIBS package version chosen for a migrated policy whose legacy package the map does not
 * resolve (DMQ36; FR-RN-028): chosen with a reason by a member of the Renewal processing team
 * (maker, created by) and approved or rejected by a second member.
 */
@Entity
@Table(name = "rnw_package_choice")
public class PackageChoice extends BaseEntity {

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(name = "legacy_package_code", length = 40, updatable = false)
  private String legacyPackageCode;

  @Column(name = "legacy_package_version", length = 20, updatable = false)
  private String legacyPackageVersion;

  @Column(name = "product_code", nullable = false, length = 20, updatable = false)
  private String productCode;

  @Column(name = "product_version_no", nullable = false, updatable = false)
  private int productVersionNo;

  @Column(nullable = false, length = 200, updatable = false)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ApprovalStatus status = ApprovalStatus.PENDING;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_remarks", length = 200)
  private String decisionRemarks;

  protected PackageChoice() {}

  /**
   * Records a choice.
   *
   * @param candidate migrated candidate
   * @param productCode chosen package (risk code)
   * @param productVersionNo chosen version
   * @param reason reason
   */
  public PackageChoice(
      RenewalCandidate candidate, String productCode, int productVersionNo, String reason) {
    this.candidateId = candidate.getId();
    this.legacyPackageCode = candidate.getSnapshot().legacyPackageCode();
    this.legacyPackageVersion = candidate.getSnapshot().legacyPackageVersion();
    this.productCode = productCode;
    this.productVersionNo = productVersionNo;
    this.reason = reason;
  }

  /**
   * Decides the choice (checker, never the maker).
   *
   * @param checker checker
   * @param approve approve or reject
   * @param remarks remarks (required to reject)
   * @param at time
   */
  public void decide(String checker, boolean approve, String remarks, Instant at) {
    if (status != ApprovalStatus.PENDING) {
      throw new BusinessRuleException(
          "RNW_PACKAGE_CHOICE_DECIDED", "The package choice is already " + status);
    }
    if (Objects.equals(checker, getCreatedBy())) {
      throw new BusinessRuleException(
          "RNW_PACKAGE_CHOICE_MAKER", "The package choice must be approved by another user");
    }
    this.status = approve ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
    this.decidedBy = checker;
    this.decidedAt = at;
    this.decisionRemarks = remarks;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getLegacyPackageCode() {
    return legacyPackageCode;
  }

  public String getLegacyPackageVersion() {
    return legacyPackageVersion;
  }

  public String getProductCode() {
    return productCode;
  }

  public int getProductVersionNo() {
    return productVersionNo;
  }

  public String getReason() {
    return reason;
  }

  public ApprovalStatus getStatus() {
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
}
