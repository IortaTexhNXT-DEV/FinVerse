package com.iortatechnxt.brokerverse.system.domain;

/** How a background job run was started. */
public enum JobTrigger {
  /** Started by the scheduler (cron). */
  SCHEDULED,
  /** Started by a user ("run now"). */
  MANUAL
}
