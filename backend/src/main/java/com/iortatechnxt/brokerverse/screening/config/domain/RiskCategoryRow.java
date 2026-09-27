package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A risk-profile category of a RISK_RULES version (SNSRP-102). Rows are replaced as a whole while
 * the version is a draft.
 */
@Entity
@Table(name = "scr_risk_category")
public class RiskCategoryRow extends BaseEntity {

  @Column(name = "version_id", nullable = false, updatable = false)
  private Long versionId;

  @Column(name = "code", nullable = false, length = 30, updatable = false)
  private String code;

  @Column(name = "name", nullable = false, length = 100, updatable = false)
  private String name;

  @Column(name = "tier", nullable = false, updatable = false)
  private int tier;

  @Column(name = "kyc_risk_rating", nullable = false, length = 30, updatable = false)
  private String kycRiskRating;

  @Column(name = "tags", nullable = false, length = 200, updatable = false)
  private String tags;

  @Column(name = "case_type", length = 30, updatable = false)
  private String caseType;

  @Column(name = "requires_edd", nullable = false, updatable = false)
  private boolean requiresEdd;

  /** For JPA. */
  protected RiskCategoryRow() {}

  /**
   * Creates a row.
   *
   * @param versionId the owning version id
   * @param code code
   * @param name name
   * @param tier tier, 1 = highest
   * @param kycRiskRating KYC_RISK_RATING code
   * @param tags CLIENT_TAG codes, comma separated
   * @param caseType SCR_CASE_TYPE to open, null = none
   * @param requiresEdd whether EDD is required
   */
  @SuppressWarnings("java:S107")
  public RiskCategoryRow(
      Long versionId,
      String code,
      String name,
      int tier,
      String kycRiskRating,
      String tags,
      String caseType,
      boolean requiresEdd) {
    this.versionId = versionId;
    this.code = code;
    this.name = name;
    this.tier = tier;
    this.kycRiskRating = kycRiskRating;
    this.tags = tags;
    this.caseType = caseType;
    this.requiresEdd = requiresEdd;
  }

  public Long getVersionId() {
    return versionId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public int getTier() {
    return tier;
  }

  public String getKycRiskRating() {
    return kycRiskRating;
  }

  public String getTags() {
    return tags;
  }

  public String getCaseType() {
    return caseType;
  }

  public boolean isRequiresEdd() {
    return requiresEdd;
  }
}
