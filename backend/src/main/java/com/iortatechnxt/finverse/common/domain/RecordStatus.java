package com.iortatechnxt.finverse.common.domain;

/** Lifecycle of master data records governed by maker-checker control. */
public enum RecordStatus {
  /** Created or modified by a maker; not usable until authorized by a checker. */
  PENDING_AUTHORIZATION,
  /** Authorized and usable. */
  ACTIVE,
  /** Deactivated. Records are never physically deleted. */
  INACTIVE
}
