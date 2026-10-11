package com.iortatechnxt.brokerverse.screening.matching.domain;

/**
 * Status of a screening match (SNSRP-301, 304; FR-SS-031 R1): recorded POTENTIAL by the engine,
 * then confirmed TRUE_MATCH by the investigation or cleared as FALSE_POSITIVE with justification
 * and evidence.
 */
public enum MatchStatus {
  /** Recorded by the engine; not yet decided. */
  POTENTIAL,
  /** Confirmed by the investigation. */
  TRUE_MATCH,
  /** Cleared with justification and evidence; the pair is suppressed for the entry version. */
  FALSE_POSITIVE
}
