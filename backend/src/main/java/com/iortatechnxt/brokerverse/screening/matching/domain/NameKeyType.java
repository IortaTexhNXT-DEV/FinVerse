package com.iortatechnxt.brokerverse.screening.matching.domain;

/** Kind of blocking key in {@code scr_name_key} (SNSRP-301; design 4.2). */
public enum NameKeyType {
  /** A normalised name token. */
  TOKEN,
  /** A Double Metaphone code of a token. */
  PHONETIC,
  /** The sorted token string of a whole name. */
  EXACT
}
