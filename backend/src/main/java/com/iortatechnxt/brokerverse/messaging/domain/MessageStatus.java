package com.iortatechnxt.brokerverse.messaging.domain;

/** Delivery status of an outbound message. */
public enum MessageStatus {
  /** Waiting for (another) delivery attempt. */
  QUEUED,
  /** Delivered to the mail server (or simulated). */
  SENT,
  /** Every attempt failed; {@code lastError} holds the reason. */
  FAILED,
  /** Withdrawn before delivery. */
  CANCELLED
}
