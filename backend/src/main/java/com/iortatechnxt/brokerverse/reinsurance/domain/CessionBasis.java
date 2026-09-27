package com.iortatechnxt.brokerverse.reinsurance.domain;

/** How the premium of a transaction was allocated. */
public enum CessionBasis {
  /** Allocated risk by risk on the treaty programme (original issue, renewal). */
  FULL,
  /** Endorsement, refund or cancellation ceded in the proportions of the in-force allocation. */
  PRO_RATA,
  /** Nothing to cede (no premium, e.g. a NIL endorsement). */
  NONE
}
