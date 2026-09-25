package com.iortatechnxt.brokerverse.productmaint.domain;

/**
 * Scope of a package (BRPM.013, PQ14): a generic programme for a segment, or a client-specific
 * package whose negotiated terms Marketing reviews before the client presentation.
 */
public enum RequestScope {
  /** Generic programme (the Marketing review of the terms is skipped). */
  GENERIC,
  /** Client-specific package (a client is mandatory; Marketing reviews the terms). */
  CLIENT_SPECIFIC
}
