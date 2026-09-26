package com.iortatechnxt.brokerverse.screening.matching.service;

/**
 * Port for "Open Case" on a potential match (FR-SS-032; SNSRP-303): opens a screening case for the
 * match, or adds the match to the client's open case of the same type (FR-SS-032 alternate flow,
 * FR-SS-034 R1). Implemented by the case wave; until a bean exists the Matches screen refuses the
 * action with {@code SCR_CASES_NOT_AVAILABLE}.
 */
public interface MatchCaseOpener {

  /**
   * Opens or joins the case of a match. The implementation stamps the case on the match through
   * {@code MatchDecisionService.linkToCase}.
   *
   * @param match the match (status POTENTIAL, not yet cased)
   * @return the case the match now belongs to
   */
  OpenedCase open(ScreenedMatch match);

  /**
   * The case a match was opened in or added to.
   *
   * @param caseId the case id
   * @param caseNo the case number (SCR-yyyy-nnnnnn)
   * @param joined true when the match joined an open case, false for a new case
   */
  record OpenedCase(Long caseId, String caseNo, boolean joined) {}
}
