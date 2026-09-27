package com.iortatechnxt.brokerverse.migration.object.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A decision of the class of a data object (gate G1; FR-DM-002): submitted by the Data Migration
 * Lead with the criteria and the rationale, approved or returned by the business owner of the
 * object, never by the submitter. The history of decisions of an object is kept.
 */
@Entity
@Table(name = "mig_object_decision")
public class MigObjectDecision extends BaseEntity {

  /** Status of a decision. */
  public enum Status {
    /** Waiting for the business owner. */
    FOR_DECISION,
    /** Approved. */
    DECIDED,
    /** Returned to the Data Migration Lead. */
    RETURNED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "decision_no", nullable = false, length = 30, updatable = false)
  private String decisionNo;

  @Column(name = "object_code", nullable = false, length = 10, updatable = false)
  private String objectCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "proposed_class", nullable = false, length = 20, updatable = false)
  private MigrationClass proposedClass;

  @Column(name = "condition_text", length = 500, updatable = false)
  private String conditionText;

  @Column(name = "condition_met", nullable = false, updatable = false)
  private boolean conditionMet;

  @Column(nullable = false, length = 2000, updatable = false)
  private String criteria;

  @Column(nullable = false, length = 2000, updatable = false)
  private String rationale;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.FOR_DECISION;

  @Column(name = "submitted_by", nullable = false, length = 50, updatable = false)
  private String submittedBy;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "return_reason", length = 1000)
  private String returnReason;

  protected MigObjectDecision() {}

  /**
   * A submitted decision.
   *
   * @param companyId company
   * @param decisionNo number
   * @param object the object with its proposal
   * @param conditionMet the condition of a conditional class is stated as met
   * @param criteria criteria summary
   * @param submitter Data Migration Lead
   * @param when time
   */
  public MigObjectDecision(
      Long companyId,
      String decisionNo,
      MigDataObject object,
      boolean conditionMet,
      String criteria,
      String submitter,
      Instant when) {
    this.companyId = companyId;
    this.decisionNo = decisionNo;
    this.objectCode = object.getCode();
    this.proposedClass = object.getProposedClass();
    this.conditionText = object.getConditionText();
    this.conditionMet = conditionMet;
    this.criteria = criteria;
    this.rationale = object.getRationale();
    this.submittedBy = submitter;
    this.submittedAt = when;
  }

  /**
   * Approves the decision.
   *
   * @param approver business owner
   * @param when time
   */
  public void approve(String approver, Instant when) {
    requirePending();
    requireOtherThanSubmitter(approver);
    this.status = Status.DECIDED;
    this.decidedBy = approver;
    this.decidedAt = when;
  }

  /**
   * Returns the decision.
   *
   * @param approver business owner
   * @param reason reason
   * @param when time
   */
  public void returnWith(String approver, String reason, Instant when) {
    requirePending();
    requireOtherThanSubmitter(approver);
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Enter the reason for the return");
    }
    this.status = Status.RETURNED;
    this.decidedBy = approver;
    this.decidedAt = when;
    this.returnReason = reason.strip();
  }

  private void requirePending() {
    if (status != Status.FOR_DECISION) {
      throw new BusinessRuleException(
          "MIG_DECISION_NOT_PENDING", "Decision " + decisionNo + " is not waiting for approval");
    }
  }

  private void requireOtherThanSubmitter(String approver) {
    if (CurrentUser.sameUser(approver, submittedBy)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getDecisionNo() {
    return decisionNo;
  }

  public String getObjectCode() {
    return objectCode;
  }

  public MigrationClass getProposedClass() {
    return proposedClass;
  }

  public String getConditionText() {
    return conditionText;
  }

  public boolean isConditionMet() {
    return conditionMet;
  }

  public String getCriteria() {
    return criteria;
  }

  public String getRationale() {
    return rationale;
  }

  public Status getStatus() {
    return status;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getReturnReason() {
    return returnReason;
  }
}
