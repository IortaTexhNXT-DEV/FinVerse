package com.iortatechnxt.brokerverse.renewal.domain;

/** Who produced a renewal letter. */
public enum LetterSource {
  /** A job. */
  SYSTEM,
  /** A user. */
  USER,
  /** Sent by hand before go-live (tracker). */
  LEGACY_MANUAL;
}
