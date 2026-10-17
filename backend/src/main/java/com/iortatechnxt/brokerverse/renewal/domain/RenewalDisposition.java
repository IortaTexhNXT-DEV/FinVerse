package com.iortatechnxt.brokerverse.renewal.domain;

/**
 * Marketing's decision on an expiring account (BRD 2.004.3); the codes are fixed, their labels are
 * the list of values {@code RNW_DISPOSITION}.
 */
public enum RenewalDisposition {
  /** Renew: Processing, insurer, Renewal Advice, acceptance, placement and booking. */
  FOR_RENEWAL("For Renewal"),
  /** Do not renew: letter step, then closed. */
  NOT_FOR_RENEWAL("Not for Renewal"),
  /** New Business path: package quotation. */
  FOR_QUOTATION("For Quotation"),
  /** New Business path: proposal request (PRF). */
  FOR_PROPOSAL("For Proposal"),
  /** Lost business: closed. */
  LOST_BUSINESS("Lost Business");

  private final String label;

  RenewalDisposition(String label) {
    this.label = label;
  }

  /**
   * The name shown to users.
   *
   * @return label
   */
  public String label() {
    return label;
  }

  /**
   * Whether the disposition takes the New Business path.
   *
   * @return true for For Quotation and For Proposal
   */
  public boolean isNewBusinessPath() {
    return this == FOR_QUOTATION || this == FOR_PROPOSAL;
  }
}
