package com.iortatechnxt.brokerverse.screening.config.domain;

/**
 * Name-matching algorithm of a matching rule (SNSRP-101): exact normalised name, phonetic (Double
 * Metaphone keys) or fuzzy (Jaro-Winkler similarity).
 */
public enum MatchAlgorithm {
  EXACT,
  PHONETIC,
  FUZZY
}
