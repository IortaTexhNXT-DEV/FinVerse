package com.iortatechnxt.brokerverse.placement.domain;

/** What opens the payment gate of an account (BRD 2.3.1). */
public enum GateRule {
  /** CBG Fire and Motor: the premium must be matched as paid. */
  PAYMENT_MATCHED,
  /** Other Lines: the client's confirmation is recorded. */
  CLIENT_CONFIRMATION,
  /** Paid directly to the insurer: the gate does not apply (BRNB.114). */
  DIRECT_PAYMENT
}
