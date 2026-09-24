package com.iortatechnxt.brokerverse.placement.domain;

/** Kind of evidence behind a payment gate decision. */
public enum EvidenceKind {
  /** A payment matched from a payment report or supplied by a confirmation source. */
  PAYMENT,
  /** The client's confirmation recorded by a user. */
  CLIENT_CONFIRMATION,
  /** The account is paid directly to the insurer (BRNB.114). */
  DIRECT_PAYMENT
}
