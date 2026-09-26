package com.iortatechnxt.brokerverse.system.domain;

/** Outcome of a background job run. */
public enum JobRunStatus {
  RUNNING,
  SUCCEEDED,
  FAILED,
  /** Not run: another instance held the job lock (the job was already running). */
  SKIPPED_LOCKED
}
