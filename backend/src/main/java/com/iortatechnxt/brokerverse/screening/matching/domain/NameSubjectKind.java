package com.iortatechnxt.brokerverse.screening.matching.domain;

/**
 * Whose name a blocking key belongs to (SNSRP-301). For ENTRY and ALIAS keys the subject id is the
 * watchlist entry id (an alias key points to the entry it is an alias of); for CLIENT keys it is
 * the client id.
 */
public enum NameSubjectKind {
  /** A client's names. */
  CLIENT,
  /** A watchlist entry's own names. */
  ENTRY,
  /** A watchlist entry's aliases. */
  ALIAS
}
