package com.iortatechnxt.brokerverse.screening.cases.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A screening case (SNSRP-303, 401-405, 501, 502, 701-706; design 4.4): the client, what opened it,
 * its type and risk category, the active-policy flag, the marketing unit and unit head, the stage
 * of workflow {@code SCR_CASE} (mirrored), the assignee, the disposition and recommendation, the
 * SLA state of the current stage entry and the configuration versions in force when it was opened
 * (FR-SS-034 R4). One open case per client and case type.
 */
@Entity
@Table(name = "scr_case")
public class ScreeningCase extends BaseEntity {

  private static final int MAX_TEXT = 4000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "case_no", nullable = false, length = 30, updatable = false)
  private String caseNo;

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "client_code", nullable = false, length = 30)
  private String clientCode;

  @Column(name = "client_name", nullable = false, length = 300)
  private String clientName;

  @Column(name = "client_type", nullable = false, length = 20, updatable = false)
  private String clientType;

  @Column(name = "trigger_code", nullable = false, length = 30, updatable = false)
  private String triggerCode;

  @Column(name = "trigger_reference", length = 100, updatable = false)
  private String triggerReference;

  @Column(name = "case_type", nullable = false, length = 30, updatable = false)
  private String caseType;

  @Column(name = "risk_category", length = 30)
  private String riskCategory;

  @Column(name = "template_type", nullable = false, length = 30, updatable = false)
  private String templateType;

  @Column(name = "active_policy", nullable = false)
  private boolean activePolicy;

  @Column(name = "marketing_unit", length = 20)
  private String marketingUnit;

  @Column(name = "unit_head", length = 50)
  private String unitHead;

  @Column(name = "account_officer", length = 50)
  private String accountOfficer;

  @Column(name = "team_code", length = 20)
  private String teamCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "stage", nullable = false, length = 30)
  private CaseStage stage;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private CaseStatus status;

  @Column(name = "stage_entered_at", nullable = false)
  private Instant stageEnteredAt;

  @Column(name = "assignee", length = 50)
  private String assignee;

  @Column(name = "investigator", length = 50)
  private String investigator;

  @Enumerated(EnumType.STRING)
  @Column(name = "returned_from", length = 30)
  private CaseStage returnedFrom;

  @Column(name = "returned_by", length = 50)
  private String returnedBy;

  @Column(name = "round_no", nullable = false)
  private int roundNo = 1;

  @Column(name = "committee_round", nullable = false)
  private int committeeRound;

  @Column(name = "disposition", length = 40)
  private String disposition;

  @Column(name = "investigator_disposition", length = 40)
  private String investigatorDisposition;

  @Column(name = "recommendation", length = MAX_TEXT)
  private String recommendation;

  @Column(name = "str_required", nullable = false)
  private boolean strRequired;

  @Column(name = "committee_decision", length = 40)
  private String committeeDecision;

  @Column(name = "committee_decided_at")
  private Instant committeeDecidedAt;

  @Column(name = "due_at")
  private Instant dueAt;

  @Column(name = "reminder_lead_hours")
  private Integer reminderLeadHours;

  @Column(name = "remind_at")
  private Instant remindAt;

  @Column(name = "escalate_to_role", length = 40)
  private String escalateToRole;

  @Column(name = "reminded_at")
  private Instant remindedAt;

  @Column(name = "breached", nullable = false)
  private boolean breached;

  @Column(name = "breached_at")
  private Instant breachedAt;

  @Column(name = "escalated_to", length = 200)
  private String escalatedTo;

  @Column(name = "document_reminded_on")
  private LocalDate documentRemindedOn;

  @Column(name = "match_version_id", updatable = false)
  private Long matchVersionId;

  @Column(name = "risk_version_id", updatable = false)
  private Long riskVersionId;

  @Column(name = "approval_version_id", updatable = false)
  private Long approvalVersionId;

  @Column(name = "assignment_version_id", updatable = false)
  private Long assignmentVersionId;

  @Column(name = "sla_version_id", updatable = false)
  private Long slaVersionId;

  @Column(name = "validation_version_id", updatable = false)
  private Long validationVersionId;

  @Column(name = "work_case_id")
  private Long workCaseId;

  @Column(name = "closed_at")
  private Instant closedAt;

  /** For JPA. */
  protected ScreeningCase() {}

  /**
   * Opens a case in stage NEW.
   *
   * @param caseNo the case number (SCR-yyyy-nnnnnn)
   * @param client the client and its sales facts
   * @param kind trigger, type, category, template and active-policy flag
   * @param versions the configuration versions in force
   * @param now the creation time
   */
  public ScreeningCase(
      String caseNo, CaseClient client, CaseKind kind, CaseVersions versions, Instant now) {
    this.caseNo = caseNo;
    this.companyId = client.companyId();
    this.clientId = client.clientId();
    this.clientCode = client.clientCode();
    this.clientName = client.clientName();
    this.clientType = client.clientType();
    this.marketingUnit = client.marketingUnit();
    this.unitHead = client.unitHead();
    this.accountOfficer = client.accountOfficer();
    this.triggerCode = kind.trigger();
    this.triggerReference = kind.reference();
    this.caseType = kind.caseType();
    this.riskCategory = kind.riskCategory();
    this.templateType = kind.templateType();
    this.activePolicy = kind.activePolicy();
    this.matchVersionId = versions.matchVersionId();
    this.riskVersionId = versions.riskVersionId();
    this.approvalVersionId = versions.approvalVersionId();
    this.assignmentVersionId = versions.assignmentVersionId();
    this.slaVersionId = versions.slaVersionId();
    this.validationVersionId = versions.validationVersionId();
    this.stage = CaseStage.NEW;
    this.status = CaseStatus.OPEN;
    this.stageEnteredAt = now;
  }

  /**
   * Links the workflow case.
   *
   * @param id {@code wf_case.id}
   */
  public void linkWorkCase(Long id) {
    this.workCaseId = id;
  }

  /**
   * Mirrors a stage entry (from {@code WorkCaseTransitioned}): a new stage entry has its own SLA,
   * reminder and escalation (FR-SS-044 R1).
   *
   * @param newStage the stage entered
   * @param at when
   */
  public void enter(CaseStage newStage, Instant at) {
    this.stage = newStage;
    this.stageEnteredAt = at;
    this.status = newStage == CaseStage.CLOSED ? CaseStatus.CLOSED : CaseStatus.OPEN;
    this.closedAt = newStage == CaseStage.CLOSED ? at : null;
    this.dueAt = null;
    this.reminderLeadHours = null;
    this.remindAt = null;
    this.escalateToRole = null;
    this.remindedAt = null;
    this.breached = false;
    this.breachedAt = null;
    this.escalatedTo = null;
    this.documentRemindedOn = null;
  }

  /**
   * Sets the SLA of the current stage entry.
   *
   * @param due the due time, null for none
   * @param leadHours reminder lead hours, null for none
   * @param escalateRole the role notified on a breach, null for none
   */
  public void applySla(Instant due, Integer leadHours, String escalateRole) {
    this.dueAt = due;
    this.reminderLeadHours = leadHours;
    this.remindAt = due == null || leadHours == null ? due : due.minus(Duration.ofHours(leadHours));
    this.escalateToRole = escalateRole;
  }

  /**
   * Records the SLA reminder of the stage entry.
   *
   * @param at when
   */
  public void reminded(Instant at) {
    this.remindedAt = at;
  }

  /**
   * Flags the breach of the stage entry and who was notified.
   *
   * @param at when
   * @param notified the users or role notified
   */
  public void breach(Instant at, String notified) {
    this.breached = true;
    this.breachedAt = at;
    this.escalatedTo = notified;
  }

  /**
   * Records the day of the last missing-document reminder (FR-SS-081 R1).
   *
   * @param day the day
   */
  public void documentReminded(LocalDate day) {
    this.documentRemindedOn = day;
  }

  /**
   * Sets the assignee.
   *
   * @param user the user, null for the stage queue
   */
  public void assignTo(String user) {
    this.assignee = user;
  }

  /**
   * Records the investigator's disposition and recommendation (SNSRP-502).
   *
   * @param newDisposition the disposition
   * @param text the recommendation
   * @param str whether an STR is proposed
   * @param by the investigator
   */
  public void dispose(String newDisposition, String text, boolean str, String by) {
    this.disposition = newDisposition;
    this.investigatorDisposition = newDisposition;
    this.recommendation = text;
    this.strRequired = str;
    this.investigator = by;
  }

  /**
   * Records a later disposition (unit head, Compliance) without changing the investigator.
   *
   * @param newDisposition the disposition
   */
  public void decide(String newDisposition) {
    this.disposition = newDisposition;
  }

  /**
   * Marks the case as needing an STR (Compliance FOR_STR or the committee's APPROVE_STR).
   *
   * @param required whether an STR is required
   */
  public void requireStr(boolean required) {
    this.strRequired = required;
  }

  /**
   * Records a return: the stage that returned the case, by whom, and a new round (FR-SS-062 R1).
   *
   * @param from the stage that returned the case
   * @param by the user
   */
  public void returned(CaseStage from, String by) {
    this.returnedFrom = from;
    this.returnedBy = by;
    this.roundNo++;
  }

  /** Opens a new AML Committee round (a new escalation). */
  public void newCommitteeRound() {
    this.committeeRound++;
  }

  /**
   * Records the final committee decision.
   *
   * @param decision APPROVE_STR, NO_STR or COMMITTEE_RETURN
   * @param at when
   */
  public void committeeDecided(String decision, Instant at) {
    this.committeeDecision = decision;
    this.committeeDecidedAt = at;
  }

  /**
   * Refreshes the risk category (a later outcome of the same case type).
   *
   * @param category the category code
   */
  public void categorise(String category) {
    this.riskCategory = category;
  }

  /**
   * Sets the investigator's sales team (team scope of the case list, FR-SS-041 R1).
   *
   * @param team the team code, may be null
   */
  public void team(String team) {
    this.teamCode = team;
  }

  /**
   * Whether the case is open.
   *
   * @return true when not closed
   */
  public boolean isOpen() {
    return status == CaseStatus.OPEN;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCaseNo() {
    return caseNo;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getClientName() {
    return clientName;
  }

  public String getClientType() {
    return clientType;
  }

  public String getTriggerCode() {
    return triggerCode;
  }

  public String getTriggerReference() {
    return triggerReference;
  }

  public String getCaseType() {
    return caseType;
  }

  public String getRiskCategory() {
    return riskCategory;
  }

  public String getTemplateType() {
    return templateType;
  }

  public boolean isActivePolicy() {
    return activePolicy;
  }

  public String getMarketingUnit() {
    return marketingUnit;
  }

  public String getUnitHead() {
    return unitHead;
  }

  public String getAccountOfficer() {
    return accountOfficer;
  }

  public String getTeamCode() {
    return teamCode;
  }

  public CaseStage getStage() {
    return stage;
  }

  public CaseStatus getStatus() {
    return status;
  }

  public Instant getStageEnteredAt() {
    return stageEnteredAt;
  }

  public String getAssignee() {
    return assignee;
  }

  public String getInvestigator() {
    return investigator;
  }

  public CaseStage getReturnedFrom() {
    return returnedFrom;
  }

  public String getReturnedBy() {
    return returnedBy;
  }

  public int getRoundNo() {
    return roundNo;
  }

  public int getCommitteeRound() {
    return committeeRound;
  }

  public String getDisposition() {
    return disposition;
  }

  public String getInvestigatorDisposition() {
    return investigatorDisposition;
  }

  public String getRecommendation() {
    return recommendation;
  }

  public boolean isStrRequired() {
    return strRequired;
  }

  public String getCommitteeDecision() {
    return committeeDecision;
  }

  public Instant getCommitteeDecidedAt() {
    return committeeDecidedAt;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public Integer getReminderLeadHours() {
    return reminderLeadHours;
  }

  public Instant getRemindAt() {
    return remindAt;
  }

  public String getEscalateToRole() {
    return escalateToRole;
  }

  public Instant getRemindedAt() {
    return remindedAt;
  }

  public boolean isBreached() {
    return breached;
  }

  public Instant getBreachedAt() {
    return breachedAt;
  }

  public String getEscalatedTo() {
    return escalatedTo;
  }

  public LocalDate getDocumentRemindedOn() {
    return documentRemindedOn;
  }

  public Long getMatchVersionId() {
    return matchVersionId;
  }

  public Long getRiskVersionId() {
    return riskVersionId;
  }

  public Long getApprovalVersionId() {
    return approvalVersionId;
  }

  public Long getAssignmentVersionId() {
    return assignmentVersionId;
  }

  public Long getSlaVersionId() {
    return slaVersionId;
  }

  public Long getValidationVersionId() {
    return validationVersionId;
  }

  public Long getWorkCaseId() {
    return workCaseId;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  /**
   * The client of a case and its sales facts.
   *
   * @param companyId company
   * @param clientId client id
   * @param clientCode client or prospect code
   * @param clientName display name
   * @param clientType INDIVIDUAL or CORPORATE
   * @param marketingUnit marketing unit (sales department) code, may be null
   * @param unitHead unit head user, may be null
   * @param accountOfficer the client's account officer, may be null
   */
  public record CaseClient(
      Long companyId,
      Long clientId,
      String clientCode,
      String clientName,
      String clientType,
      String marketingUnit,
      String unitHead,
      String accountOfficer) {}

  /**
   * What a case is.
   *
   * @param trigger the screening trigger (or MANUAL)
   * @param reference the trigger's reference, may be null
   * @param caseType the case type
   * @param riskCategory the risk category, may be null
   * @param templateType the review template type
   * @param activePolicy whether the client has an active policy
   */
  public record CaseKind(
      String trigger,
      String reference,
      String caseType,
      String riskCategory,
      String templateType,
      boolean activePolicy) {}

  /**
   * The configuration versions in force when the case was opened (FR-SS-034 R4).
   *
   * @param matchVersionId MATCH_CRITERIA
   * @param riskVersionId RISK_RULES
   * @param approvalVersionId APPROVAL_MATRIX
   * @param assignmentVersionId ASSIGNMENT_MATRIX
   * @param slaVersionId SLA_MATRIX
   * @param validationVersionId VALIDATION_RULES
   */
  public record CaseVersions(
      Long matchVersionId,
      Long riskVersionId,
      Long approvalVersionId,
      Long assignmentVersionId,
      Long slaVersionId,
      Long validationVersionId) {}
}
