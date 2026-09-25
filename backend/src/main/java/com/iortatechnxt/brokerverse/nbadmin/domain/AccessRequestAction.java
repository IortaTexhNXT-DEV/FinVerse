package com.iortatechnxt.brokerverse.nbadmin.domain;

/** An event in the history of an access request (BRD 1.008, 2.002; "add / save remarks"). */
public enum AccessRequestAction {
  /** Saved as a draft. */
  SAVE("Saved as draft"),
  /** Draft or returned request edited. */
  EDIT("Edited"),
  /** Submitted. */
  SUBMIT("Submitted"),
  /** Resubmitted after a return. */
  RESUBMIT("Resubmitted"),
  /** Returned to the requester. */
  RETURN("Returned"),
  /** Cancelled. */
  CANCEL("Cancelled"),
  /** Approved by an approver. */
  APPROVE("Approved"),
  /** Second approval given. */
  SECOND_APPROVE("Second approval"),
  /** Rejected. */
  REJECT("Rejected"),
  /** Approved with a future effective date. */
  SCHEDULE("Approved, scheduled"),
  /** Change applied. */
  APPLY("Approved and applied"),
  /** Scheduled change could not be applied. */
  APPLY_FAILED("Could not be applied"),
  /** Approved group-profile request handed to the System Administrator. */
  FOR_IMPLEMENTATION("Approved, for implementation"),
  /** Group-profile request implemented. */
  IMPLEMENT("Implemented");

  private final String label;

  AccessRequestAction(String label) {
    this.label = label;
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
