package com.iortatechnxt.brokerverse.nbadmin.domain;

/**
 * The party an external (portal) user belongs to (cross-BRD decision D7; USER_ACCESS_DESIGN section
 * 4.4).
 */
public enum ExternalPartyKind {
  /** A user of an insurer. */
  INSURER,
  /** An HR user of a client. */
  CLIENT
}
