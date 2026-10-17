package com.iortatechnxt.brokerverse.placement.domain;

/** Life of a CLPC billing batch (BRNB.067). */
public enum BillingBatchStatus {
  /** File generated; waiting for the CLPC payment report. */
  GENERATED,
  /** A payment report was uploaded and is being reviewed. */
  REPORT_RECEIVED,
  /** The payment report was confirmed. */
  CLOSED
}
