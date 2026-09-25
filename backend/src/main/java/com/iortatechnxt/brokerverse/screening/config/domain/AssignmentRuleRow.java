package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A scenario of an ASSIGNMENT_MATRIX version (SNSRP-106). Rows are replaced as a whole while the
 * version is a draft.
 */
@Entity
@Table(name = "scr_assignment_rule")
public class AssignmentRuleRow extends BaseEntity {

  @Column(name = "version_id", nullable = false, updatable = false)
  private Long versionId;

  @Column(name = "sort_order", nullable = false, updatable = false)
  private int sortOrder;

  @Column(name = "case_type", length = 30, updatable = false)
  private String caseType;

  @Column(name = "trigger_code", length = 30, updatable = false)
  private String triggerCode;

  @Column(name = "risk_category", length = 30, updatable = false)
  private String riskCategory;

  @Column(name = "marketing_unit", length = 30, updatable = false)
  private String marketingUnit;

  @Column(name = "client_type", length = 20, updatable = false)
  private String clientType;

  @Column(name = "team_role", length = 50, updatable = false)
  private String teamRole;

  @Column(name = "user_name", length = 50, updatable = false)
  private String assigneeUser;

  @Enumerated(EnumType.STRING)
  @Column(name = "balancing", nullable = false, length = 20, updatable = false)
  private Balancing balancing;

  /** For JPA. */
  protected AssignmentRuleRow() {}

  /**
   * Creates a row.
   *
   * @param versionId the owning version id
   * @param sortOrder evaluation order
   * @param caseType case type condition
   * @param triggerCode trigger condition
   * @param riskCategory risk category condition
   * @param marketingUnit marketing unit condition
   * @param clientType client type condition
   * @param teamRole team role target
   * @param assigneeUser user target
   * @param balancing balancing
   */
  @SuppressWarnings("java:S107")
  public AssignmentRuleRow(
      Long versionId,
      int sortOrder,
      String caseType,
      String triggerCode,
      String riskCategory,
      String marketingUnit,
      String clientType,
      String teamRole,
      String assigneeUser,
      Balancing balancing) {
    this.versionId = versionId;
    this.sortOrder = sortOrder;
    this.caseType = caseType;
    this.triggerCode = triggerCode;
    this.riskCategory = riskCategory;
    this.marketingUnit = marketingUnit;
    this.clientType = clientType;
    this.teamRole = teamRole;
    this.assigneeUser = assigneeUser;
    this.balancing = balancing;
  }

  public Long getVersionId() {
    return versionId;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getCaseType() {
    return caseType;
  }

  public String getTriggerCode() {
    return triggerCode;
  }

  public String getRiskCategory() {
    return riskCategory;
  }

  public String getMarketingUnit() {
    return marketingUnit;
  }

  public String getClientType() {
    return clientType;
  }

  public String getTeamRole() {
    return teamRole;
  }

  public String getAssigneeUser() {
    return assigneeUser;
  }

  public Balancing getBalancing() {
    return balancing;
  }
}
