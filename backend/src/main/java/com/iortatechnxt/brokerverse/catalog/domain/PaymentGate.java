package com.iortatechnxt.brokerverse.catalog.domain;

/** What releases an account for placement (BRD 2.3.1). */
public enum PaymentGate {
  /** CBG Fire and Motor: the premium must be paid. */
  PAID,
  /** Other Lines: the client's confirmation is enough. */
  CLIENT_CONFIRMATION
}
