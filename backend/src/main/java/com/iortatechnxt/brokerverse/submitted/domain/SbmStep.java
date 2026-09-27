package com.iortatechnxt.brokerverse.submitted.domain;

/** Steps of a processing run, in their order (BRIDSP-08, 09, 16). */
public enum SbmStep {
  /** Normalised fields, duplicates and incomplete records. */
  SANITATION,
  /** PN matched against the LAMD loan snapshot. */
  MATCHING,
  /** Inforced or Submitted. */
  CLASSIFICATION,
  /** Bucket, renewal tag, RA template, exclusion. */
  DISPOSITION,
  /** Insurer acceptance and coverage limits. */
  LIMITS
}
