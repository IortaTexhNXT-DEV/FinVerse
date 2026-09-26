package com.iortatechnxt.brokerverse.adjustment.domain;

/** Outcome of one request in a posting batch (ADJID.006). */
public enum BatchOutcome {
  /** Posted and complete. */
  POSTED,
  /** Posted; payments wait for re-application by cashiering. */
  AWAITING_REAPPLICATION,
  /** Not posted; the message says why (the request stays for posting). */
  FAILED
}
