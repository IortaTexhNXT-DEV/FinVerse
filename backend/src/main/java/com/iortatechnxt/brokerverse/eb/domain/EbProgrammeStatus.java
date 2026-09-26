package com.iortatechnxt.brokerverse.eb.domain;

/** Status of an EB programme (design 4.2). */
public enum EbProgrammeStatus {
  /** Prospect: no placed cycle yet. */
  PROSPECT,
  /** Placed and in force. */
  ACTIVE,
  /** Expired without renewal. */
  LAPSED,
  /** Lost to another broker or insurer arrangement. */
  LOST,
  /** Closed by the AO. */
  INACTIVE
}
