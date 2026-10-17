package com.iortatechnxt.brokerverse.screening.watchlist.domain;

/** Result of an ingestion run (SNSRP-201): PARTIAL when some records failed. */
public enum RunStatus {
  RUNNING,
  SUCCESS,
  PARTIAL,
  FAILED
}
