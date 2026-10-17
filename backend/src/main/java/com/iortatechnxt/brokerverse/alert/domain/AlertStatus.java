package com.iortatechnxt.brokerverse.alert.domain;

/** Alert lifecycle: OPEN, then ACKNOWLEDGED (being handled), then RESOLVED. */
public enum AlertStatus {
  OPEN,
  ACKNOWLEDGED,
  RESOLVED
}
