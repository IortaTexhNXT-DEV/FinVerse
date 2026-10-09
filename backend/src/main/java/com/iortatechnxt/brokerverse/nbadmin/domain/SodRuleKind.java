package com.iortatechnxt.brokerverse.nbadmin.domain;

/** What a separation-of-duties rule pairs (BDOI FRS FRUM.006.03 and FRUM.012.01). */
public enum SodRuleKind {
  /** Two group profiles one user may not hold. */
  PROFILES,
  /** Two permissions that may not be held together (conflicting permission combination). */
  PERMISSIONS
}
