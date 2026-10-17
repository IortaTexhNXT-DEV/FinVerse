package com.iortatechnxt.brokerverse.booking.domain;

/** E-mail dispatch of a service invoice (BRNB.100b). */
public enum DispatchStatus {
  /** Not e-mailed (internal type, or no billing address). */
  NOT_SENT,
  /** Queued in the outbox. */
  QUEUED,
  /** Delivered (or simulated) by the mail transport. */
  SENT,
  /** Delivery failed; the reason is kept and the owner notified. */
  FAILED
}
