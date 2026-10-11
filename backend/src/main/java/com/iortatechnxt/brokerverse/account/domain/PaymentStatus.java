package com.iortatechnxt.brokerverse.account.domain;

/** Payment gate position of an account (BRD 2.3.1, BRNB.067/068/114). */
public enum PaymentStatus {
  /** Premium not yet paid or confirmed. */
  UNPAID,
  /** Premium paid (CBG Fire and Motor). */
  PAID,
  /** Client confirmation received (Other Lines). */
  CLIENT_CONFIRMED,
  /** Paid directly to the insurer: the payment gate does not apply (BRNB.114). */
  DIRECT
}
