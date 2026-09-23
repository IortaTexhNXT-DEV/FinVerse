package com.iortatechnxt.finverse.system.api.dto;

import com.iortatechnxt.finverse.system.domain.JobRun;
import com.iortatechnxt.finverse.system.domain.JobRunStatus;
import com.iortatechnxt.finverse.system.domain.JobTrigger;
import java.time.Instant;

/**
 * One job run.
 *
 * @param id id
 * @param jobName job
 * @param trigger scheduled or manual
 * @param triggeredBy user or SYSTEM
 * @param startedAt start
 * @param finishedAt end
 * @param status status
 * @param itemsProcessed items processed
 * @param message summary or error
 */
public record JobRunResponse(
    Long id,
    String jobName,
    JobTrigger trigger,
    String triggeredBy,
    Instant startedAt,
    Instant finishedAt,
    JobRunStatus status,
    int itemsProcessed,
    String message) {

  /**
   * Maps an entity.
   *
   * @param r run
   * @return response
   */
  public static JobRunResponse from(JobRun r) {
    return new JobRunResponse(
        r.getId(),
        r.getJobName(),
        r.getTrigger(),
        r.getTriggeredBy(),
        r.getStartedAt(),
        r.getFinishedAt(),
        r.getStatus(),
        r.getItemsProcessed(),
        r.getMessage());
  }
}
