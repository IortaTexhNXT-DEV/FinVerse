package com.iortatechnxt.brokerverse.screening.risk.api.dto;

import com.iortatechnxt.brokerverse.screening.risk.domain.RiskProfileEntry;
import com.iortatechnxt.brokerverse.screening.risk.domain.RiskSource;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A row of the client risk-profile history (SNSRP-302, 304; FR-SS-033 "category, rating, tags,
 * source RULE, the rule, the match and the time").
 *
 * @param id id
 * @param clientId client
 * @param clientCode client code
 * @param categoryCode risk category
 * @param previousRating rating before
 * @param riskRating rating after
 * @param tagsAdded tags added (comma separated)
 * @param tagsRemoved tags ended (comma separated)
 * @param activeTags active tags after the change (comma separated)
 * @param source RULE or MANUAL
 * @param riskVersionId risk rules version (RULE)
 * @param ruleId rule (RULE)
 * @param matchId match
 * @param runId run (RULE)
 * @param justification justification (MANUAL) or category name (RULE)
 * @param evidence evidence attachment ids (MANUAL)
 * @param evidenceCaseId case holding the evidence (MANUAL)
 * @param kycReviewDue next KYC review after the change
 * @param effectiveAt when
 * @param by who (SYSTEM for a rule run by a job)
 */
public record RiskProfileRow(
    Long id,
    Long clientId,
    String clientCode,
    String categoryCode,
    String previousRating,
    String riskRating,
    String tagsAdded,
    String tagsRemoved,
    String activeTags,
    RiskSource source,
    Long riskVersionId,
    Long ruleId,
    Long matchId,
    Long runId,
    String justification,
    String evidence,
    Long evidenceCaseId,
    LocalDate kycReviewDue,
    Instant effectiveAt,
    String by) {

  /**
   * Maps a history row.
   *
   * @param e entry
   * @return row
   */
  public static RiskProfileRow from(RiskProfileEntry e) {
    return new RiskProfileRow(
        e.getId(),
        e.getClientId(),
        e.getClientCode(),
        e.getCategoryCode(),
        e.getPreviousRating(),
        e.getKycRiskRating(),
        e.getTagsAdded(),
        e.getTagsRemoved(),
        e.getActiveTags(),
        e.getSource(),
        e.getRiskVersionId(),
        e.getRuleId(),
        e.getMatchId(),
        e.getRunId(),
        e.getJustification(),
        e.getEvidence(),
        e.getEvidenceCaseId(),
        e.getKycReviewDue(),
        e.getEffectiveAt(),
        e.getCreatedBy());
  }
}
