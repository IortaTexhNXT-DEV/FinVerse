package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.screening.config.domain.MatchAlgorithm;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.matching.domain.MatchStatus;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatch;
import java.math.BigDecimal;
import java.util.Set;

/**
 * One match of a client against a watchlist entry as the {@link ScreeningEngine} recorded it
 * (SNSRP-301, FR-SS-031): the pair, the entry version, the best score and its algorithm, the fields
 * that matched, and whether the score reaches the case threshold of the matching rule (FR-SS-034: a
 * NAME_MATCH case opens or the match joins the open case). Stable contract for the case wave.
 *
 * @param matchId the match ({@code scr_match.id})
 * @param runId the run that recorded it
 * @param clientId the client
 * @param clientCode the client or prospect code
 * @param clientName the client's display name
 * @param entryId the watchlist entry
 * @param entryVersion the entry version matched (suppressions and idempotence key on it)
 * @param entryName the entry's primary name
 * @param sourceCode the entry's source (AML_ADVISORY, NLDS_PEP, INTERNAL ...)
 * @param listType the entry's list type (SANCTION, PEP, INTERNAL, ADVERSE_MEDIA)
 * @param subjectType individual or entity
 * @param score the best score, 0 to 1 (scale 4)
 * @param algorithm the algorithm of the best score
 * @param matchedFields the fields that matched (NAME or ALIAS, BIRTH_DATE, NATIONALITY, ID)
 * @param matchRuleId the matching rule applied ({@code scr_match_rule.id})
 * @param reachesCaseThreshold whether the score is at or above the rule's case threshold
 * @param status the match status
 * @param caseId the screening case the match belongs to, {@code null} when not cased
 */
public record ScreenedMatch(
    Long matchId,
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
    Set<MatchField> matchedFields,
    Long matchRuleId,
    boolean reachesCaseThreshold,
    MatchStatus status,
    Long caseId) {

  /** Defensive copy. */
  public ScreenedMatch {
    matchedFields = matchedFields == null ? Set.of() : Set.copyOf(matchedFields);
  }

  /**
   * Maps a stored match.
   *
   * @param m the match
   * @return the record
   */
  public static ScreenedMatch from(ScreeningMatch m) {
    return new ScreenedMatch(
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
        m.fields(),
        m.getMatchRuleId(),
        m.isCaseThreshold(),
        m.getStatus(),
        m.getCaseId());
  }
}
