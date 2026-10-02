package com.iortatechnxt.brokerverse.issuance.domain;

/** Life of a bulk e-policy upload. */
public enum UploadStatus {
  /** Matches proposed; the user reviews them. */
  REVIEW,
  /** Confirmed: the included files were stored on their accounts. */
  CONFIRMED,
  /** Discarded without effect. */
  DISCARDED
}
