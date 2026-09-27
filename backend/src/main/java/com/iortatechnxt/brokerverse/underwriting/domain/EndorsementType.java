package com.iortatechnxt.brokerverse.underwriting.domain;

import java.util.Optional;

/** Kinds of policy endorsement and the accounting event each one raises. */
public enum EndorsementType {
  /** Additional premium (extension of cover, increased sum insured). */
  ADDITIONAL("POLICY_ENDORSEMENT"),
  /** Return premium (reduced cover). */
  REFUND("POLICY_ENDORSEMENT"),
  /** Renewal for a new period, accounted for like a new issue. */
  RENEWAL("POLICY_ISSUE"),
  /** Cancellation with pro-rata (1/365) return premium. */
  CANCELLATION("POLICY_CANCELLATION"),
  /** Non-financial change (address, name, description). */
  NIL(null);

  private final String eventType;

  EndorsementType(String eventType) {
    this.eventType = eventType;
  }

  /**
   * Accounting event type raised on approval.
   *
   * @return event type code, empty for non-financial endorsements
   */
  public Optional<String> eventType() {
    return Optional.ofNullable(eventType);
  }
}
