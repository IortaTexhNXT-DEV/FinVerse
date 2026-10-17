package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A rule of a VALIDATION_RULES version (SNSRP-701, 802). Rows are replaced as a whole while the
 * version is a draft.
 */
@Entity
@Table(name = "scr_validation_rule")
public class ValidationRuleRow extends BaseEntity {

  @Column(name = "version_id", nullable = false, updatable = false)
  private Long versionId;

  @Column(name = "stage", length = 30, updatable = false)
  private String stage;

  @Column(name = "case_type", length = 30, updatable = false)
  private String caseType;

  @Enumerated(EnumType.STRING)
  @Column(name = "rule_kind", nullable = false, length = 40, updatable = false)
  private ValidationKind ruleKind;

  @Column(name = "parameters", length = 500, updatable = false)
  private String parameters;

  @Column(name = "blocking", nullable = false, updatable = false)
  private boolean blocking;

  /** For JPA. */
  protected ValidationRuleRow() {}

  /**
   * Creates a row.
   *
   * @param versionId the owning version id
   * @param stage stage condition
   * @param caseType case type condition
   * @param ruleKind check
   * @param parameters check parameters
   * @param blocking whether a failure blocks
   */
  @SuppressWarnings("java:S107")
  public ValidationRuleRow(
      Long versionId,
      String stage,
      String caseType,
      ValidationKind ruleKind,
      String parameters,
      boolean blocking) {
    this.versionId = versionId;
    this.stage = stage;
    this.caseType = caseType;
    this.ruleKind = ruleKind;
    this.parameters = parameters;
    this.blocking = blocking;
  }

  public Long getVersionId() {
    return versionId;
  }

  public String getStage() {
    return stage;
  }

  public String getCaseType() {
    return caseType;
  }

  public ValidationKind getRuleKind() {
    return ruleKind;
  }

  public String getParameters() {
    return parameters;
  }

  public boolean isBlocking() {
    return blocking;
  }
}
