package com.iortatechnxt.brokerverse.screening.matching.domain;

import java.math.BigDecimal;

/**
 * Criteria of the match search (Matches screen, FR-SS-032); {@code null} criteria are ignored.
 *
 * @param companyId company
 * @param status status
 * @param listType list type
 * @param minScore lowest score
 * @param maxScore highest score
 * @param like lower-case pattern on the client name or code or the entry name, {@code %} for all
 * @param uncased true for matches not yet in a case only
 */
public record MatchSearch(
    Long companyId,
    MatchStatus status,
    String listType,
    BigDecimal minScore,
    BigDecimal maxScore,
    String like,
    boolean uncased) {}
