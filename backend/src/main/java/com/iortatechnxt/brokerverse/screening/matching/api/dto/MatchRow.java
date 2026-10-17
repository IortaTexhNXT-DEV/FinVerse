package com.iortatechnxt.brokerverse.screening.matching.api.dto;

import com.iortatechnxt.brokerverse.screening.config.domain.MatchAlgorithm;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.matching.domain.MatchStatus;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatch;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A match on the Matches screen and the client Screening tab (SNSRP-301; FR-SS-031 fields).
 *
 * @param id match id
 * @param runId run that recorded it
 * @param clientId client
 * @param clientCode client code
 * @param clientName client name
 * @param entryId watchlist entry
 * @param entryVersion entry version matched
 * @param entryName entry primary name
 * @param sourceCode source
 * @param listType list type
 * @param subjectType individual or entity
 * @param score score, 0 to 1
 * @param algorithm algorithm of the score
 * @param matchedFields fields matched
 * @param caseThreshold whether the score reaches the case threshold
 * @param status status
 * @param caseId case, {@code null} when not cased
 * @param decidedBy who decided
 * @param decidedAt when decided
 * @param decisionRemarks justification or remarks of the decision
 * @param createdAt when recorded
 */
public record MatchRow(
    Long id,
    Long runId,
    Long clientId,
    String clientCode,
    String clientName,
    Long entryId,
    int entryVersion,
    String entryName,
    String sourceCode,
    String listType,
    SubjectType subjectType,
    BigDecimal score,
    MatchAlgorithm algorithm,
    List<String> matchedFields,
    boolean caseThreshold,
    MatchStatus status,
    Long caseId,
    String decidedBy,
    Instant decidedAt,
    String decisionRemarks,
    Instant createdAt) {

  /** Defensive copy. */
  public MatchRow {
    matchedFields = List.copyOf(matchedFields);
  }

  /**
   * Maps a match.
   *
   * @param m match
   * @return row
   */
  public static MatchRow from(ScreeningMatch m) {
    return new MatchRow(
        m.getId(),
        m.getRunId(),
        m.getClientId(),
        m.getClientCode(),
        m.getClientName(),
        m.getEntryId(),
        m.getEntryVersion(),
        m.getEntryName(),
        m.getSourceCode(),
        m.getListType(),
        m.getSubjectType(),
        m.getScore(),
        m.getAlgorithm(),
        m.fields().stream().map(Enum::name).toList(),
        m.isCaseThreshold(),
        m.getStatus(),
        m.getCaseId(),
        m.getDecidedBy(),
        m.getDecidedAt(),
        m.getDecisionRemarks(),
        m.getCreatedAt());
  }
}
