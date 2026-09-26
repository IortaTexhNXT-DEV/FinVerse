package com.iortatechnxt.brokerverse.storage.domain;

/** Malware scan state of a stored file (DOCUMENT_STORAGE_DECISION, decision 2). */
public enum ScanStatus {
  /** A presigned upload was issued; the object has not been confirmed yet. */
  AWAITING_UPLOAD,
  /** Stored; the scan result tag has not been read yet. Not downloadable. */
  PENDING,
  /** The scan found no threats ({@code NO_THREATS_FOUND}). Downloadable. */
  CLEAN,
  /** The scan found a threat or could not complete; moved to the quarantine prefix. */
  QUARANTINED
}
