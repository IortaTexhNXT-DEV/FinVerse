package com.iortatechnxt.brokerverse.csf.domain;

/** State of a contact change record (FRS section 5.2). */
public enum ChangeStatus {
  /** The contact change is saved in the client master (FR-CSF-021). */
  APPLIED,
  /** The request asked for a field the contact centre cannot change; nothing was saved. */
  REFUSED,
  /** A change the contact centre cannot make was referred to the fulfilment unit. */
  REFERRED
}
