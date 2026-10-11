package com.iortatechnxt.brokerverse.booking.domain;

/** Who receives a service invoice type (BRNB.100). */
public enum SiRecipient {
  /** The insurer (commission invoice), e-mailed to its billing address. */
  INSURER,
  /** An internal unit; kept in the register, not e-mailed. */
  INTERNAL
}
