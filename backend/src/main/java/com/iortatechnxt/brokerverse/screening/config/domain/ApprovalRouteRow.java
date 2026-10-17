package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A route of an APPROVAL_MATRIX version (SNSRP-103, 703). Rows are replaced as a whole while the
 * version is a draft.
 */
@Entity
@Table(name = "scr_approval_route")
public class ApprovalRouteRow extends BaseEntity {

  @Column(name = "version_id", nullable = false, updatable = false)
  private Long versionId;

  @Column(name = "sort_order", nullable = false, updatable = false)
  private int sortOrder;

  @Column(name = "from_stage", nullable = false, length = 30, updatable = false)
  private String fromStage;

  @Column(name = "case_type", length = 30, updatable = false)
  private String caseType;

  @Column(name = "risk_category", length = 30, updatable = false)
  private String riskCategory;

  @Column(name = "marketing_unit", length = 30, updatable = false)
  private String marketingUnit;

  @Column(name = "disposition", length = 40, updatable = false)
  private String disposition;

  @Column(name = "to_stage", nullable = false, length = 30, updatable = false)
  private String toStage;

  @Enumerated(EnumType.STRING)
  @Column(name = "approver_kind", length = 20, updatable = false)
  private ApproverKind approverKind;

  @Column(name = "approver_value", length = 50, updatable = false)
  private String approverValue;

  /** For JPA. */
  protected ApprovalRouteRow() {}

  /**
   * Creates a row.
   *
   * @param versionId the owning version id
   * @param sortOrder evaluation order
   * @param fromStage stage left
   * @param caseType case type condition
   * @param riskCategory risk category condition
   * @param marketingUnit marketing unit condition
   * @param disposition disposition condition
   * @param toStage next stage
   * @param approverKind how the approver is named
   * @param approverValue role or user
   */
  @SuppressWarnings("java:S107")
  public ApprovalRouteRow(
      Long versionId,
      int sortOrder,
      String fromStage,
      String caseType,
      String riskCategory,
      String marketingUnit,
      String disposition,
      String toStage,
      ApproverKind approverKind,
      String approverValue) {
    this.versionId = versionId;
    this.sortOrder = sortOrder;
    this.fromStage = fromStage;
    this.caseType = caseType;
    this.riskCategory = riskCategory;
    this.marketingUnit = marketingUnit;
    this.disposition = disposition;
    this.toStage = toStage;
    this.approverKind = approverKind;
    this.approverValue = approverValue;
  }

  public Long getVersionId() {
    return versionId;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getFromStage() {
    return fromStage;
  }

  public String getCaseType() {
    return caseType;
  }

  public String getRiskCategory() {
    return riskCategory;
  }

  public String getMarketingUnit() {
    return marketingUnit;
  }

  public String getDisposition() {
    return disposition;
  }

  public String getToStage() {
    return toStage;
  }

  public ApproverKind getApproverKind() {
    return approverKind;
  }

  public String getApproverValue() {
    return approverValue;
  }
}
