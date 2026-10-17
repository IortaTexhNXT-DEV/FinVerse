package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * An SLA row of an SLA_MATRIX version (SNSRP-108). Rows are replaced as a whole while the version
 * is a draft.
 */
@Entity
@Table(name = "scr_sla_rule")
public class SlaRuleRow extends BaseEntity {

  @Column(name = "version_id", nullable = false, updatable = false)
  private Long versionId;

  @Column(name = "stage", nullable = false, length = 30, updatable = false)
  private String stage;

  @Column(name = "case_type", length = 30, updatable = false)
  private String caseType;

  @Column(name = "risk_category", length = 30, updatable = false)
  private String riskCategory;

  @Column(name = "sla_hours", nullable = false, updatable = false)
  private int slaHours;

  @Column(name = "reminder_lead_hours", nullable = false, updatable = false)
  private int reminderLeadHours;

  @Column(name = "escalate_to_role", nullable = false, length = 50, updatable = false)
  private String escalateToRole;

  @Enumerated(EnumType.STRING)
  @Column(name = "calendar", nullable = false, length = 20, updatable = false)
  private SlaCalendar calendar;

  /** For JPA. */
  protected SlaRuleRow() {}

  /**
   * Creates a row.
   *
   * @param versionId the owning version id
   * @param stage stage
   * @param caseType case type condition
   * @param riskCategory risk category condition
   * @param slaHours SLA hours
   * @param reminderLeadHours reminder lead hours
   * @param escalateToRole escalation role
   * @param calendar calendar or working hours
   */
  @SuppressWarnings("java:S107")
  public SlaRuleRow(
      Long versionId,
      String stage,
      String caseType,
      String riskCategory,
      int slaHours,
      int reminderLeadHours,
      String escalateToRole,
      SlaCalendar calendar) {
    this.versionId = versionId;
    this.stage = stage;
    this.caseType = caseType;
    this.riskCategory = riskCategory;
    this.slaHours = slaHours;
    this.reminderLeadHours = reminderLeadHours;
    this.escalateToRole = escalateToRole;
    this.calendar = calendar;
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

  public String getRiskCategory() {
    return riskCategory;
  }

  public int getSlaHours() {
    return slaHours;
  }

  public int getReminderLeadHours() {
    return reminderLeadHours;
  }

  public String getEscalateToRole() {
    return escalateToRole;
  }

  public SlaCalendar getCalendar() {
    return calendar;
  }
}
