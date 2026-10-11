package com.iortatechnxt.brokerverse.acsl.domain;

/** Stage of an ACSL case, mirrored from its work case (workflow ACSL_CASE of V890). */
public enum CaseStage {
  /** Received, waiting for the team leader's assignment. */
  RECEIVED,
  /** Assigned to a processor. */
  ASSIGNED,
  /** Being investigated. */
  INVESTIGATING,
  /** The result was given to the requester (ACSL 2.5.4). */
  RESULT_PROVIDED,
  /** A correction entry was raised (ACSL 2.9.0). */
  CORRECTION
}
