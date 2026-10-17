package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.system.domain.JobRun;
import com.iortatechnxt.brokerverse.system.domain.JobRunRepository;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Records background job runs (job monitor). The run record is written in its own transactions
 * before and after the work, so the history survives a failing job; the work itself runs outside
 * any transaction. Other modules call {@link #execute} for batch runs they start themselves (for
 * example a recurring journal run started from its screen).
 *
 * <p>Every run holds the cluster-wide {@link JobLock} of its job name: when another instance (or
 * another thread) is already running the job, the run is not executed and is recorded as {@code
 * SKIPPED_LOCKED}. When the lock store cannot be reached the run is recorded as failed (and the
 * failure listeners are told), never executed unguarded.
 */
@Service
public class JobRunService {

  private static final Logger LOG = LoggerFactory.getLogger(JobRunService.class);

  private final JobRunRepository runs;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final List<JobFailureListener> failureListeners;
  private final TransactionTemplate newTransaction;
  private final JobLock jobLock;

  /**
   * Creates the service.
   *
   * @param runs run repository
   * @param currentUser current user
   * @param clock clock
   * @param failureListeners failure listeners (alert engine)
   * @param transactionManager transaction manager
   * @param jobLock cluster-wide job lock
   */
  public JobRunService(
      JobRunRepository runs,
      CurrentUser currentUser,
      Clock clock,
      List<JobFailureListener> failureListeners,
      PlatformTransactionManager transactionManager,
      JobLock jobLock) {
    this.runs = runs;
    this.currentUser = currentUser;
    this.clock = clock;
    this.failureListeners = List.copyOf(failureListeners);
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.jobLock = jobLock;
  }

  /**
   * Runs work as a recorded job run, under the job's cluster-wide lock. Failures are recorded (and
   * reported to the failure listeners), not rethrown.
   *
   * @param jobName job name
   * @param trigger trigger
   * @param work the job body
   * @return the finished run (status {@code SKIPPED_LOCKED} when the job was already running)
   */
  public JobRun execute(String jobName, JobTrigger trigger, Supplier<JobOutcome> work) {
    Optional<JobLock.Lease> lease;
    try {
      lease = jobLock.tryAcquire(jobName);
    } catch (RuntimeException ex) {
      LOG.error("Job {} not run: the job lock is unavailable", jobName, ex);
      JobRun failed =
          complete(start(jobName, trigger), run -> run.fail("Job lock unavailable", now()));
      notifyFailure(failed);
      return failed;
    }
    if (lease.isEmpty()) {
      LOG.info("Job {} skipped: already running on another instance", jobName);
      return complete(
          start(jobName, trigger),
          run -> run.skipLocked("Skipped: the job is already running on another instance", now()));
    }
    try (JobLock.Lease held = lease.get()) {
      LOG.debug("Job {} runs with fencing token {}", jobName, held.fencingToken());
      return run(jobName, trigger, work);
    }
  }

  private JobRun run(String jobName, JobTrigger trigger, Supplier<JobOutcome> work) {
    Long id = start(jobName, trigger);
    try {
      JobOutcome outcome = work.get();
      return complete(id, run -> run.succeed(outcome.itemsProcessed(), outcome.message(), now()));
    } catch (RuntimeException ex) {
      LOG.error("Job {} failed", jobName, ex);
      String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
      JobRun failed = complete(id, run -> run.fail(reason, now()));
      notifyFailure(failed);
      return failed;
    }
  }

  /**
   * Latest run of a job.
   *
   * @param jobName job name
   * @return latest run if any
   */
  @Transactional(readOnly = true)
  public Optional<JobRun> latest(String jobName) {
    return runs.findFirstByJobNameOrderByStartedAtDesc(jobName);
  }

  /**
   * Run history.
   *
   * @param jobName job name or null for all
   * @param since lower bound of the start time
   * @param pageable paging
   * @return runs, newest first
   */
  @Transactional(readOnly = true)
  public Page<JobRun> history(String jobName, Instant since, Pageable pageable) {
    return runs.history(jobName, since, pageable);
  }

  private Long start(String jobName, JobTrigger trigger) {
    return newTransaction.execute(
        s ->
            runs.save(new JobRun(jobName, trigger, currentUser.username(), clock.instant()))
                .getId());
  }

  private JobRun complete(Long id, Consumer<JobRun> change) {
    return newTransaction.execute(
        s -> {
          JobRun run = runs.findById(id).orElseThrow();
          change.accept(run);
          return run;
        });
  }

  private void notifyFailure(JobRun run) {
    for (JobFailureListener listener : failureListeners) {
      try {
        newTransaction.executeWithoutResult(s -> listener.onFailure(run));
      } catch (RuntimeException ex) {
        LOG.error("Job failure listener {} failed", listener.getClass().getSimpleName(), ex);
      }
    }
  }

  private Instant now() {
    return clock.instant();
  }
}
