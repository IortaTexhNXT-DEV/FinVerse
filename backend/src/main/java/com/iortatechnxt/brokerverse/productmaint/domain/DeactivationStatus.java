package com.iortatechnxt.brokerverse.productmaint.domain;

/** Approval status of a package deactivation request (BDOI FRS FRPM.003.06). */
public enum DeactivationStatus {
  /** Waiting for the selected approver. */
  PENDING("Pending Approval"),
  /** Approved: the package expiry date was set. */
  APPROVED("Approved"),
  /** Rejected with the approval remarks: the package stays active. */
  REJECTED("Rejected"),
  /** Withdrawn by the requestor before the decision. */
  CANCELLED("Cancelled");

  private final String label;

  DeactivationStatus(String label) {
    this.label = label;
  }

  /**
   * Business label shown on screens.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
