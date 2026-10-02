package com.iortatechnxt.brokerverse.placement.domain;

/** Outcome of matching one payment report line to the accounts awaiting payment. */
public enum MatchStatus {
  /** One account awaiting payment, reported paid. */
  MATCHED,
  /** One account found but reported unpaid. */
  UNPAID,
  /** No account awaiting payment has the reference. */
  UNMATCHED,
  /** Several accounts have the reference: the user chooses. */
  AMBIGUOUS
}
