package com.iortatechnxt.brokerverse.csf.domain;

/** Result of the verification of a caller (FR-CSF-020). */
public enum VerificationResult {
  /** Enough checks matched: the contact details may be changed while the verification is valid. */
  PASSED,
  /** Too few checks matched: no change is allowed. */
  FAILED
}
