package com.iortatechnxt.brokerverse.screening.watchlist.domain;

/** What started an ingestion run (SNSRP-201). */
public enum IngestionTrigger {
  SCHEDULED,
  MANUAL_UPLOAD,
  API
}
