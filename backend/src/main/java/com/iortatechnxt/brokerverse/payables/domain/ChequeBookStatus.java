package com.iortatechnxt.brokerverse.payables.domain;

/** Status of a cheque book. */
public enum ChequeBookStatus {
  /** In use: leaves are allocated from it. */
  ACTIVE,
  /** Every leaf has been used. */
  EXHAUSTED,
  /** Withdrawn (lost, damaged); no further leaves are issued. */
  CANCELLED
}
