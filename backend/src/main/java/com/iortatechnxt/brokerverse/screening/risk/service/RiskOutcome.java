package com.iortatechnxt.brokerverse.screening.risk.service;

import java.util.Set;

/**
 * The risk category a client qualified for after a screening run or a match decision (SNSRP-302,
 * FR-SS-033), and what was applied to the client master. Returned for every client whose rules
 * qualified, also when nothing changed (the client already had the rating and tags), so that the
 * case wave can open or join the category's case (FR-SS-034). Stable contract for the case wave.
 *
 * @param clientId the client
 * @param categoryCode the qualifying risk category ({@code scr_risk_category.code})
 * @param categoryName the category's name
 * @param tier the category tier (1 = highest risk)
 * @param kycRiskRating the rating the category sets ({@code KYC_RISK_RATING})
 * @param riskRating the client's rating after the evaluation (a rule never lowers it)
 * @param tagsAdded the tags this evaluation added (empty when unchanged)
 * @param caseType the case type the category opens ({@code SCR_CASE_TYPE}), {@code null} = none
 * @param requiresEdd whether the category requires EDD for a client with an active policy
 * @param riskVersionId the RISK_RULES configuration version evaluated
 * @param ruleId the rule that qualified ({@code scr_risk_rule.id})
 * @param matchId the match the rule held on, {@code null} for a rule on client attributes only
 * @param profileEntryId the risk-profile history row written, {@code null} when nothing changed
 * @param changed whether the rating or tags of the client changed
 */
public record RiskOutcome(
    Long clientId,
    String categoryCode,
    String categoryName,
    int tier,
    String kycRiskRating,
    String riskRating,
    Set<String> tagsAdded,
    String caseType,
    boolean requiresEdd,
    Long riskVersionId,
    Long ruleId,
    Long matchId,
    Long profileEntryId,
    boolean changed) {

  /** Defensive copy. */
  public RiskOutcome {
    tagsAdded = tagsAdded == null ? Set.of() : Set.copyOf(tagsAdded);
  }
}
