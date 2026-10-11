package com.iortatechnxt.brokerverse.issuance.domain;

/** Status of an Insurance Advice. */
public enum AdviceStatus {
  /** Generated, not yet sent. */
  GENERATED,
  /** Sent at least once. */
  SENT
}
