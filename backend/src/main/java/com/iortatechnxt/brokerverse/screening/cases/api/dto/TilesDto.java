package com.iortatechnxt.brokerverse.screening.cases.api.dto;

import com.iortatechnxt.brokerverse.screening.cases.service.CaseQueries.Tiles;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Screening Home tiles (SNSRP-402, 405; FR-SS-045).
 *
 * @param openByStage open cases per stage
 * @param dueToday cases due today
 * @param breached cases past their SLA
 * @param potentialMatches potential matches not yet in a case
 * @param lastRunNo last watchlist run number, null when none
 * @param lastRunStatus its status
 * @param lastRunAt its start
 */
public record TilesDto(
    Map<String, Long> openByStage,
    long dueToday,
    long breached,
    long potentialMatches,
    String lastRunNo,
    String lastRunStatus,
    Instant lastRunAt) {

  /**
   * Maps the tiles.
   *
   * @param t the tiles
   * @return the DTO
   */
  public static TilesDto from(Tiles t) {
    Map<String, Long> stages = new LinkedHashMap<>();
    t.openByStage().forEach((stage, count) -> stages.put(stage.name(), count));
    return new TilesDto(
        stages,
        t.dueToday(),
        t.breached(),
        t.potentialMatches(),
        t.lastRun() == null ? null : t.lastRun().getRunNo(),
        t.lastRun() == null ? null : t.lastRun().getStatus().name(),
        t.lastRun() == null ? null : t.lastRun().getStartedAt());
  }
}
