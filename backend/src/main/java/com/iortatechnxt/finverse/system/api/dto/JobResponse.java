package com.iortatechnxt.finverse.system.api.dto;

import com.iortatechnxt.finverse.system.service.JobRegistry.JobStatus;
import java.time.Instant;

/**
 * Job monitor row.
 *
 * @param name job name
 * @param description description
 * @param cron cron expression (UTC) or "-" when manual only
 * @param nextRun next scheduled run
 * @param lastRun latest run (null when never run)
 */
public record JobResponse(
    String name, String description, String cron, Instant nextRun, JobRunResponse lastRun) {

  /**
   * Maps a job status.
   *
   * @param s status
   * @return response
   */
  public static JobResponse from(JobStatus s) {
    return new JobResponse(
        s.job().name(),
        s.job().description(),
        s.job().cron(),
        s.nextRun(),
        s.lastRun().map(JobRunResponse::from).orElse(null));
  }
}
