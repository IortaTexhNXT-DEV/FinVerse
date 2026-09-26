package com.iortatechnxt.brokerverse.storage.domain;

/** Archive state of a stored file in the ECM (DOCUMENT_STORAGE_DECISION, decision 5). */
public enum EcmStatus {
  /** The record class is not archived to ECM. */
  NOT_REQUIRED,
  /** The record class is archived to ECM; the record has not reached its final state. */
  AWAITING_FINAL,
  /** Final: waits for the archive publisher. */
  PENDING,
  /** The archive request was published through the integration outbox. */
  REQUESTED,
  /** The ECM acknowledged the record; its reference is stored. */
  ARCHIVED
}
