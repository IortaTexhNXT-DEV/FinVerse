package com.iortatechnxt.brokerverse.identity.domain;

/** Outcome of a provisioning event. */
public enum IdentityEventStatus {
  /** Applied to the user. */
  APPLIED,
  /** Refused by a rule (for example a Windows ID held by another user); reprocessable. */
  REFUSED,
  /** Failed for a technical reason; reprocessable. */
  FAILED,
  /** Nothing to change (the user already matches the event). */
  NO_CHANGE
}
