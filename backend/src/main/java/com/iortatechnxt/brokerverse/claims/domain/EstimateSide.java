package com.iortatechnxt.brokerverse.claims.domain;

/** The two sides of a claim estimate: amounts the company pays, and amounts it recovers. */
public enum EstimateSide {
  /** Indemnity and expenses payable (the outstanding claim reserve). */
  PAYMENT,
  /** Salvage and subrogation receivable (memorandum estimate, not reserved in the ledger). */
  RECOVERY
}
