package com.iortatechnxt.finverse.reserves.domain;

/** Claim amounts accumulated in a development triangle. */
public enum TriangleBasis {
  /** Payments less recoveries. */
  PAID,
  /** Payments less recoveries plus the change in outstanding reserves. */
  INCURRED
}
