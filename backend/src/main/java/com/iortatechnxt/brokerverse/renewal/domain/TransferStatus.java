package com.iortatechnxt.brokerverse.renewal.domain;

/** Status of a transfer between Marketing units (BRD 1.006/1.007). */
public enum TransferStatus {
  /** Waiting for the receiving unit. */
  REQUESTED,
  /** Accepted. */
  ACCEPTED,
  /** Declined with remarks. */
  DECLINED,
  /** Cancelled by the sender. */
  CANCELLED;
}
