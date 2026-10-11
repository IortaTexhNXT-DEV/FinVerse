package com.iortatechnxt.brokerverse.placement.domain;

/** Life of a placement slip version (BRNB.069/071). */
public enum SlipStatus {
  /** Generated, not yet sent. */
  GENERATED,
  /** Sent to the insurer (may be resent). */
  SENT,
  /** Replaced by a regenerated version (kept for the record). */
  SUPERSEDED
}
