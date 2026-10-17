package com.iortatechnxt.brokerverse.screening.cases.domain;

/** Status of a case review (SNSRP-501): saved as a draft or submitted with the case. */
public enum ReviewStatus {
  /** Being completed. */
  DRAFT,
  /** Submitted with the case (read only until the case returns). */
  SUBMITTED
}
