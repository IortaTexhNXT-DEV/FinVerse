package com.iortatechnxt.brokerverse.claims.domain;

/** Status of a local purchase order. */
public enum LpoStatus {
  /** Issued to the garage. */
  ISSUED,
  /** Cancelled with a reason; excluded from reports. */
  CANCELLED
}
