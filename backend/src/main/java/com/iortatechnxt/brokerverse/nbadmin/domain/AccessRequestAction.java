package com.iortatechnxt.brokerverse.nbadmin.domain;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;

/** An event in the history of an access request (BRD 1.008, 2.002; "add / save remarks"). */
public enum AccessRequestAction {
  /** Saved as a draft. */
  SAVE("Saved as draft", AuditAction.CREATE),
  /** Draft or returned request edited. */
  EDIT("Edited", AuditAction.UPDATE),
  /** Submitted. */
  SUBMIT("Submitted", AuditAction.SUBMIT),
  /** Resubmitted after a return. */
  RESUBMIT("Resubmitted", AuditAction.SUBMIT),
  /** Returned to the requester. */
  RETURN("Returned", AuditAction.REJECT),
  /** Cancelled. */
  CANCEL("Cancelled", AuditAction.DEACTIVATE),
  /** Approved by an approver. */
  APPROVE("Approved", AuditAction.AUTHORIZE),
  /** Second approval given. */
  SECOND_APPROVE("Second approval", AuditAction.AUTHORIZE),
  /** Rejected. */
  REJECT("Rejected", AuditAction.REJECT),
  /** Approved with a future effective date. */
  SCHEDULE("Approved, scheduled", AuditAction.UPDATE),
  /** Change applied. */
  APPLY("Approved and applied", AuditAction.AUTHORIZE),
  /** Scheduled change could not be applied. */
  APPLY_FAILED("Could not be applied", AuditAction.UPDATE),
  /** Approved group-profile request handed to the System Administrator. */
  FOR_IMPLEMENTATION("Approved, for implementation", AuditAction.UPDATE),
  /** Group-profile request implemented. */
  IMPLEMENT("Implemented", AuditAction.AUTHORIZE);

  private final String label;
  private final AuditAction auditAction;

  AccessRequestAction(String label, AuditAction auditAction) {
    this.label = label;
    this.auditAction = auditAction;
  }

  /**
   * Action of the event in the audit trail.
   *
   * @return audit action
   */
  public AuditAction auditAction() {
    return auditAction;
  }

  /**
   * Text of the event in the audit trail.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
