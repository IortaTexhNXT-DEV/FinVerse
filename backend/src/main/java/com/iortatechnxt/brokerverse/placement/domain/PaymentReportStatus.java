package com.iortatechnxt.brokerverse.placement.domain;

/** Life of an uploaded payment report. */
public enum PaymentReportStatus {
  /** Matched; the user reviews the matches. */
  REVIEW,
  /** Confirmed: the payment gate opened for the matched accounts. */
  CONFIRMED,
  /** Discarded without effect. */
  DISCARDED
}
