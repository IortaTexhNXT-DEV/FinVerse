package com.iortatechnxt.brokerverse.system.domain;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link JobRun}. */
public interface JobRunRepository extends JpaRepository<JobRun, Long> {

  /**
   * Latest run of a job.
   *
   * @param jobName job name
   * @return latest run if any
   */
  Optional<JobRun> findFirstByJobNameOrderByStartedAtDesc(String jobName);

  /**
   * Run history since a point in time, optionally for one job.
   *
   * @param jobName job name or null for all jobs
   * @param since lower bound
   * @param pageable paging
   * @return runs, newest first
   */
  @Query(
      """
      select r from JobRun r
      where (:jobName is null or r.jobName = :jobName) and r.startedAt >= :since
      order by r.startedAt desc
      """)
  Page<JobRun> history(
      @Param("jobName") String jobName, @Param("since") Instant since, Pageable pageable);
}
