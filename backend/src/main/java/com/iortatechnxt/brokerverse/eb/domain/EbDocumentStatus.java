package com.iortatechnxt.brokerverse.eb.domain;

/** Status of a document in the EB register (versions are never changed, BRID-024). */
public enum EbDocumentStatus {
  /** Current version. */
  ACTIVE,
  /** Replaced by a later version. */
  SUPERSEDED,
  /** Rejected on validation. */
  REJECTED
}
