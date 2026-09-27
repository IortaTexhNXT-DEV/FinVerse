package com.iortatechnxt.brokerverse.renewal.domain;

/** Path a renewal takes (RENEWAL_DESIGN section 4.1). */
public enum RenewalPath {
  /** Standard path through Marketing and Processing. */
  STANDARD,
  /** New Business path (quotation or PRF). */
  NB_PATH,
  /** Straight-through processing (matrix AUTO or loan-driven). */
  STP;
}
