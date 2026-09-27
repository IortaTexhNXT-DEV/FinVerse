package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.screening.risk.service.RiskOutcome;

/**
 * The result of a decision on a match (SNSRP-302, 304): the match after the decision, the risk
 * outcome of the rules evaluated on it (confirmation) and the manual history row (false positive
 * with a profile change).
 *
 * @param match the match
 * @param riskOutcome the category the client qualified for after a confirmation, {@code null} when
 *     none
 * @param manualEntryId the manual risk-profile history row, {@code null} when none
 */
public record MatchDecision(ScreenedMatch match, RiskOutcome riskOutcome, Long manualEntryId) {}
