package com.iortatechnxt.brokerverse.eb.domain;

/** Status of a tracked item (BRID-030; FR-EB-057). */
public enum EbItemStatus {
  /** Expected from the responsible party; followed up when past due. */
  PENDING,
  /** Received by BDOI. */
  RECEIVED,
  /** Released to the client or member. */
  RELEASED,
  /** Closed (delivered or no longer needed). */
  CLOSED
}
