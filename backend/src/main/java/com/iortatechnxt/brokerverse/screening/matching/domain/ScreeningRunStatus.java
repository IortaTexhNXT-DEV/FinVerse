package com.iortatechnxt.brokerverse.screening.matching.domain;

/** Status of a screening run (SNSRP-602 "every activity is logged"). */
public enum ScreeningRunStatus {
  /** Started, not yet ended. */
  RUNNING,
  /** Ended; the counts are final. */
  SUCCESS,
  /** Ended with an error; the reason is kept. */
  FAILED
}
