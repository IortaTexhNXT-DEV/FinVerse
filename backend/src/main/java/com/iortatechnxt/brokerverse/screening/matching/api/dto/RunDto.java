package com.iortatechnxt.brokerverse.screening.matching.api.dto;

import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRun;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRunStatus;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningTrigger;
import java.time.Instant;

/**
 * A screening run log line (SNSRP-602; FR-SS-030 "trigger, scope, configuration version, clients
 * and entries screened, matches, cases opened, status, start and end time").
 *
 * @param id id
 * @param runNo run number
 * @param trigger trigger
 * @param reference trigger reference
 * @param scope clients in scope
 * @param fullRescreen whether the whole list was screened
 * @param matchVersionId matching criteria version
 * @param riskVersionId risk rules version
 * @param clientsScreened clients screened
 * @param entriesScreened entries screened
 * @param matches new matches
 * @param riskChanges risk-profile changes
 * @param casesOpened cases opened
 * @param status status
 * @param error reason for failure
 * @param startedAt started
 * @param endedAt ended
 * @param createdBy who started it
 */
public record RunDto(
    Long id,
    String runNo,
    ScreeningTrigger trigger,
    String reference,
    String scope,
    boolean fullRescreen,
    Long matchVersionId,
    Long riskVersionId,
    int clientsScreened,
    int entriesScreened,
    int matches,
    int riskChanges,
    int casesOpened,
    ScreeningRunStatus status,
    String error,
    Instant startedAt,
    Instant endedAt,
    String createdBy) {

  /**
   * Maps a run.
   *
   * @param r run
   * @return DTO
   */
  public static RunDto from(ScreeningRun r) {
    return new RunDto(
        r.getId(),
        r.getRunNo(),
        r.getTrigger(),
        r.getReference(),
        r.getScope(),
        r.isFullRescreen(),
        r.getMatchVersionId(),
        r.getRiskVersionId(),
        r.getClientsScreened(),
        r.getEntriesScreened(),
        r.getMatches(),
        r.getRiskChanges(),
        r.getCasesOpened(),
        r.getStatus(),
        r.getError(),
        r.getStartedAt(),
        r.getEndedAt(),
        r.getCreatedBy());
  }
}
