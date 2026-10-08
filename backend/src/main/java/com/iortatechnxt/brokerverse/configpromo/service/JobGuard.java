package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.system.service.JobLock;
import com.iortatechnxt.brokerverse.system.service.JobRegistry;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Keeps an import apart from the work that reads the configuration, with the cluster-wide job
 * locks: one import at a time (lock {@value #APPLY_LOCK}), and no import while a period close or a
 * batch job runs (their locks are tried and released at once; a lock that is taken means the job
 * runs).
 */
@Component
public class JobGuard {

  /** Lock held for the length of an apply. */
  public static final String APPLY_LOCK = "CONFIG_PROMOTION_APPLY";

  private static final Set<String> CLOSE_JOBS = Set.of("GL_PERIOD_CLOSE", "BROKING_BOOKS_CLOSE");

  private final JobLock locks;
  private final JobRegistry jobs;

  /**
   * Creates the guard.
   *
   * @param locks job locks
   * @param jobs registered jobs
   */
  public JobGuard(JobLock locks, JobRegistry jobs) {
    this.locks = locks;
    this.jobs = jobs;
  }

  /**
   * Takes the apply lock after checking that no period close or batch job runs.
   *
   * @return the held lock; close it when the apply is over
   */
  public JobLock.Lease acquire() {
    Optional<JobLock.Lease> lease = locks.tryAcquire(APPLY_LOCK);
    if (lease.isEmpty()) {
      throw new BusinessRuleException(
          "CONFIG_IMPORT_RUNNING",
          "Another configuration import is being applied; try again later");
    }
    try {
      for (String job : guardedJobs()) {
        Optional<JobLock.Lease> probe = locks.tryAcquire(job);
        if (probe.isEmpty()) {
          throw new BusinessRuleException(
              "CONFIG_IMPORT_JOB_RUNNING",
              "The job "
                  + job
                  + " is running and reads the configuration; apply the import after it ends");
        }
        probe.get().close();
      }
    } catch (RuntimeException e) {
      lease.get().close();
      throw e;
    }
    return lease.get();
  }

  private Set<String> guardedJobs() {
    Set<String> names = new LinkedHashSet<>(CLOSE_JOBS);
    for (ManagedJob job : jobs.all()) {
      if (job.workload() == Workload.BATCH) {
        names.add(job.name());
      }
    }
    return names;
  }
}
