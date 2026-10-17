package com.iortatechnxt.brokerverse.booking.domain;

/** Incentive indicator of a booked transaction (BRNB.107, BRRN.041; FR-NB-118, FR-RN-087). */
public enum IncentiveStatus {
  /** Booked, not yet fully paid (or a renewal whose acceptance is not confirmed). */
  PENDING("Pending (not fully paid)"),
  /** At least one active incentive criterion matches. */
  ELIGIBLE("Eligible"),
  /** No criterion matches, or the booking was cancelled. */
  NOT_ELIGIBLE("Not eligible");

  private final String label;

  IncentiveStatus(String label) {
    this.label = label;
  }

  /**
   * The name shown on the screens and reports.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
