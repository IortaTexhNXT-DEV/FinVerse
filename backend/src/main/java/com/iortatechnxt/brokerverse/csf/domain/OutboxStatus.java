package com.iortatechnxt.brokerverse.csf.domain;

/** State of one row of the legacy contact sync outbox (FR-CSF-022). */
public enum OutboxStatus {
  /** Legacy sync disabled when the change was made; kept for a replay. */
  NOT_CONFIGURED,
  /** Waiting for the legacy sync job. */
  QUEUED,
  /** Accepted by the legacy system. */
  SENT,
  /** The last attempt failed; sent again by the next run. */
  FAILED
}
