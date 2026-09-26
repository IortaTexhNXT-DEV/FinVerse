package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningTrigger;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskOutcome;
import java.util.List;
import java.util.Optional;

/**
 * The result of one screening run of the {@link ScreeningEngine} (SNSRP-301, 302, 602; FR-SS-030
 * to 033): the run log identity and counts, the matches recorded by the run and the risk outcome
 * of each client concerned. Published with {@link ScreeningCompleted}; the case wave opens cases
 * from it (FR-SS-034). Stable contract for the case wave.
 *
 * @param runId the run ({@code scr_screening_run.id})
 * @param runNo the run number (SCN-yyyy-nnnnnn)
 * @param companyId the company screened
 * @param trigger what started the run
 * @param reference the trigger's reference (client code, account ARN, list change, job date)
 * @param matchVersionId the MATCH_CRITERIA configuration version used
 * @param riskVersionId the RISK_RULES configuration version used, {@code null} when none in force
 * @param clientsScreened the number of clients screened
 * @param entriesScreened the number of watchlist entries screened
 * @param matches the matches recorded by this run (new rows only; a re-run records none)
 * @param riskOutcomes the risk category of each client whose rules qualified
 */
public record ScreeningResult(
    Long runId,
    String runNo,
    Long companyId,
    ScreeningTrigger trigger,
    String reference,
    Long matchVersionId,
    Long riskVersionId,
    int clientsScreened,
    int entriesScreened,
    List<ScreenedMatch> matches,
    List<RiskOutcome> riskOutcomes) {

  /** Defensive copies. */
  public ScreeningResult {
    matches = matches == null ? List.of() : List.copyOf(matches);
    riskOutcomes = riskOutcomes == null ? List.of() : List.copyOf(riskOutcomes);
  }

  /**
   * The matches of one client recorded by this run.
   *
   * @param clientId the client
   * @return the client's matches
   */
  public List<ScreenedMatch> matchesOf(Long clientId) {
    return matches.stream().filter(m -> m.clientId().equals(clientId)).toList();
  }

  /**
   * The risk outcome of one client.
   *
   * @param clientId the client
   * @return the outcome, empty when no rule qualified
   */
  public Optional<RiskOutcome> outcomeOf(Long clientId) {
    return riskOutcomes.stream().filter(o -> o.clientId().equals(clientId)).findFirst();
  }

  /**
   * The matches whose score reaches the case threshold of their rule (FR-SS-034: NAME_MATCH).
   *
   * @return the matches
   */
  public List<ScreenedMatch> caseMatches() {
    return matches.stream().filter(ScreenedMatch::reachesCaseThreshold).toList();
  }
}
