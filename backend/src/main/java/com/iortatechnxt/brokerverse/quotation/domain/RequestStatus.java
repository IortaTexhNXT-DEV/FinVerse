package com.iortatechnxt.brokerverse.quotation.domain;

/** Status of a quotation request in the request inbox (BRNB.041). */
public enum RequestStatus {
  /** Received, not yet quoted. */
  NEW,
  /** A quotation was created for it. */
  QUOTED,
  /** Closed without a quotation (duplicate, withdrawn...). */
  CLOSED
}
