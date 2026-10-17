package com.iortatechnxt.brokerverse.submitted.domain;

/** What changed a field of a masterlist record (BRIDSP-29). */
public enum SbmHistorySource {
  /** A source file. */
  INTAKE,
  /** A confirmed document extraction. */
  EXTRACTION,
  /** A user. */
  MANUAL,
  /** A processing run. */
  RUN,
  /** The renewal hand-off and its progress. */
  RENEWAL,
  /** The booking of the renewal account. */
  BOOKING,
  /** The migration of the Excel masterlists. */
  MIGRATION
}
