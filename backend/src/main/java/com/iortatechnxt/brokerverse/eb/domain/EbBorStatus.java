package com.iortatechnxt.brokerverse.eb.domain;

/**
 * Status of a Broker on Record version (BRID-008; FR-EB-031). A cycle without any version shows
 * "Pending". A later validated version supersedes the earlier validated one.
 */
public enum EbBorStatus {
  /** Uploaded, waiting for the validator. */
  UPLOADED,
  /** Checklist confirmed; the active BOR while it is the latest validated version. */
  VALIDATED,
  /** Rejected by the validator; the AO uploads a corrected version. */
  REJECTED,
  /** Replaced by a later validated version. */
  SUPERSEDED
}
