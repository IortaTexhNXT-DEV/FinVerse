package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.screening.config.service.MatchCriteria;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRun;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskOutcome;
import java.util.ArrayList;
import java.util.List;

/**
 * A screening run in progress (SNSRP-602): the run log row, the configuration it runs with and the
 * matches and risk outcomes collected so far. Used within the run's transaction only.
 */
public final class ScreeningRunContext {

  private final ScreeningRun run;
  private final MatchCriteria criteria;
  private final Long riskVersionId;
  private final List<ScreenedMatch> matches = new ArrayList<>();
  private final List<RiskOutcome> outcomes = new ArrayList<>();

  ScreeningRunContext(ScreeningRun run, MatchCriteria criteria, Long riskVersionId) {
    this.run = run;
    this.criteria = criteria;
    this.riskVersionId = riskVersionId;
  }

  ScreeningRun run() {
    return run;
  }

  MatchCriteria criteria() {
    return criteria;
  }

  Long riskVersionId() {
    return riskVersionId;
  }

  void addMatch(ScreenedMatch match) {
    matches.add(match);
  }

  void addOutcome(RiskOutcome outcome) {
    outcomes.add(outcome);
  }

  List<ScreenedMatch> matches() {
    return matches;
  }

  List<RiskOutcome> outcomes() {
    return outcomes;
  }

  /**
   * The run number.
   *
   * @return run number
   */
  public String runNo() {
    return run.getRunNo();
  }

  /**
   * The company screened.
   *
   * @return company id
   */
  public Long companyId() {
    return run.getCompanyId();
  }
}
