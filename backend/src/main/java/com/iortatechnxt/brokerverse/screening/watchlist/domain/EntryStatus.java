package com.iortatechnxt.brokerverse.screening.watchlist.domain;

/**
 * Status of a watchlist entry (SNSRP-203): screening reads ACTIVE entries only; a new manual entry
 * is PENDING until approved, DRAFT after a rejection, INACTIVE when delisted.
 */
public enum EntryStatus {
  DRAFT,
  PENDING,
  ACTIVE,
  INACTIVE
}
