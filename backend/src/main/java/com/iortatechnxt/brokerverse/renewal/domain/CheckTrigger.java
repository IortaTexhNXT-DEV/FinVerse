package com.iortatechnxt.brokerverse.renewal.domain;

/** What ran the checks of a candidate (BRRN.020/023). */
public enum CheckTrigger {
  /** Extraction. */
  EXTRACTION,
  /** Initiation. */
  INITIATION,
  /** An upload. */
  UPLOAD,
  /** A ledger, account or booking event. */
  EVENT,
  /** The nightly re-evaluation. */
  NIGHTLY,
  /** A user. */
  MANUAL,
  /** An override. */
  OVERRIDE;
}
