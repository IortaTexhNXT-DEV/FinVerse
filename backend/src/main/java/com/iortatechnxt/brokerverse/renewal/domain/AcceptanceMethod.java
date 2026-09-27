package com.iortatechnxt.brokerverse.renewal.domain;

/** How the client accepted the renewal (BRRN.040). */
public enum AcceptanceMethod {
  /** E-mail (attachment RA_ACCEPTANCE). */
  EMAIL,
  /** Signed Renewal Advice (attachment SIGNED_RA). */
  SIGNED_RA,
  /** Payment of the renewal premium. */
  PAYMENT;
}
