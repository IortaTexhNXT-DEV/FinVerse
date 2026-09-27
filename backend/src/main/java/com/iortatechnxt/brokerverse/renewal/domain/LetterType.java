package com.iortatechnxt.brokerverse.renewal.domain;

/** Type of a renewal letter (RENEWAL_DESIGN section 4.5). */
public enum LetterType {
  /** Renewal Advice. */
  RA,
  /** No Advice Letter. */
  NAL,
  /** Not for Renewal Letter. */
  NFR,
  /** Reminder to a renewal not yet submitted. */
  NRNS_REMINDER,
  /** Non-acceptance letter after expiry. */
  NON_ACCEPTANCE;
}
