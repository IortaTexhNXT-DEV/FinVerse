package com.iortatechnxt.brokerverse.renewal.domain;

/** Status of a transfer request for a New Business opportunity (FRRN.011.04). */
public enum ReferralStatus {
  /** Saved, not submitted. */
  DRAFT("Draft"),
  /** Submitted to the receiving unit. */
  PENDING_ACCEPTANCE("Pending Acceptance"),
  /** Accepted by the receiving unit. */
  ACCEPTED("Accepted"),
  /** Rejected by the receiving unit, with remarks. */
  REJECTED("Rejected"),
  /** Returned to the requester for clarification, with remarks. */
  RETURNED("Returned for Clarification"),
  /** Withdrawn by the requester. */
  CANCELLED("Cancelled");

  private final String label;

  ReferralStatus(String label) {
    this.label = label;
  }

  /**
   * The status as users read it.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
