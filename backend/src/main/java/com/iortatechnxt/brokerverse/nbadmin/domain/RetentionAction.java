package com.iortatechnxt.brokerverse.nbadmin.domain;

/** What happens to records eligible under a retention rule (BRNB.106). */
public enum RetentionAction {
  /** Listed for a business review only. */
  REVIEW,
  /**
   * To be moved to the archive once the archive store exists (parked: storage and backup decision
   * of the DBA). Today the records are only counted and listed; nothing is moved or deleted.
   */
  ARCHIVE
}
