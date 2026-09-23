package com.iortatechnxt.finverse.system.service;

import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.system.domain.JobRun;
import com.iortatechnxt.finverse.system.domain.JobTrigger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

/** Registry of all {@link ManagedJob} beans: status for the monitor and on-demand execution. */
@Service
public class JobRegistry {

  /** Cron value that disables scheduling of a job. */
  public static final String DISABLED = "-";

  private final List<ManagedJob> jobs;
  private final JobRunService runs;
  private final Clock clock;

  /**
   * Creates the registry.
   *
   * @param jobs all job beans
   * @param runs run recorder
   * @param clock clock
   */
  public JobRegistry(List<ManagedJob> jobs, JobRunService runs, Clock clock) {
    this.jobs = jobs.stream().sorted(Comparator.comparing(ManagedJob::name)).toList();
    this.runs = runs;
    this.clock = clock;
  }

  /**
   * All registered jobs.
   *
   * @return jobs ordered by name
   */
  public List<ManagedJob> all() {
    return jobs;
  }

  /**
   * Status of every job: last run and next scheduled run.
   *
   * @return statuses ordered by job name
   */
  public List<JobStatus> statuses() {
    return jobs.stream()
        .map(j -> new JobStatus(j, runs.latest(j.name()), nextRun(j.cron())))
        .toList();
  }

  /**
   * Runs a job now.
   *
   * @param name job name
   * @param trigger trigger
   * @return finished run
   */
  public JobRun run(String name, JobTrigger trigger) {
    ManagedJob job = require(name);
    LocalDate businessDate = LocalDate.now(clock);
    return runs.execute(job.name(), trigger, () -> job.execute(businessDate));
  }

  /**
   * Finds a job.
   *
   * @param name job name
   * @return job
   */
  public ManagedJob require(String name) {
    return jobs.stream()
        .filter(j -> j.name().equals(name))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Job", name));
  }

  /**
   * Next fire time of a cron expression.
   *
   * @param cron cron expression or {@value #DISABLED}
   * @return next run, or null when not scheduled
   */
  public Instant nextRun(String cron) {
    if (DISABLED.equals(cron)) {
      return null;
    }
    ZonedDateTime next =
        CronExpression.parse(cron).next(ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    return next == null ? null : next.toInstant();
  }

  /**
   * Job with its latest run and next scheduled run.
   *
   * @param job job
   * @param lastRun latest run
   * @param nextRun next scheduled run (null when manual only)
   */
  public record JobStatus(ManagedJob job, Optional<JobRun> lastRun, Instant nextRun) {}
}
