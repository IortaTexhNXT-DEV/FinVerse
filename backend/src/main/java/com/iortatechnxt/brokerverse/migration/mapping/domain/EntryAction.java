package com.iortatechnxt.brokerverse.migration.mapping.domain;

/** Action of a code map entry (FR-DM-011 R3). */
public enum EntryAction {
  /** Map to the target code. */
  MAP,
  /** Map to the default target of the set. */
  DEFAULT,
  /** The row fails validation. */
  REJECT,
  /** A new BIBS value is created by the reference-data load. */
  CREATE
}
