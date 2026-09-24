package com.iortatechnxt.brokerverse.payables.domain;

/** Layout of the payment notification file expected by a bank. */
public enum NotificationFormat {
  /** Fixed-width records 01 (detail) and 02 (trailer), no delimiters. */
  FIXED_WIDTH,
  /** Comma separated values with a header line. */
  CSV
}
