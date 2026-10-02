package com.iortatechnxt.brokerverse.csf.domain;

/** Legacy write-back state of a contact change (FR-CSF-022). */
public enum SyncStatus {
  /** Nothing to send (refused or referred changes). */
  NOT_REQUIRED,
  /** Waiting for the next run of the legacy sync job. */
  QUEUED,
  /** Legacy sync disabled: the change stays in the outbox until the interface exists. */
  NOT_CONFIGURED,
  /** Sent to every legacy system. */
  SENT,
  /** A sending failed; it is tried again by the next run. */
  FAILED
}
