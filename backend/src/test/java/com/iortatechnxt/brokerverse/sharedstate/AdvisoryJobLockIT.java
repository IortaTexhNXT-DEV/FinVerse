package com.iortatechnxt.brokerverse.sharedstate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.sharedstate.service.AdvisoryJobLock;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.domain.JobRun;
import com.iortatechnxt.brokerverse.system.domain.JobRunRepository;
import com.iortatechnxt.brokerverse.system.domain.JobRunStatus;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.service.JobFailureListener;
import com.iortatechnxt.brokerverse.system.service.JobLock;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.JobRunService;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The PostgreSQL advisory job lock (Redis disabled) and the lock taken by every job run: of two
 * concurrent runs of a job one runs and one is recorded SKIPPED_LOCKED.
 */
@IntegrationTest
class AdvisoryJobLockIT extends JobLockContract {

  @Autowired private AdvisoryJobLock lock;
  @Autowired private DataSource dataSource;
  @Autowired private JobRunService jobRuns;
  @Autowired private JobRunRepository runRepository;
  @Autowired private CurrentUser currentUser;
  @Autowired private Clock clock;
  @Autowired private PlatformTransactionManager transactionManager;

  @Override
  JobLock lock() {
    return lock;
  }

  @Override
  JobLock otherInstanceLock() {
    return new AdvisoryJobLock(dataSource);
  }

  @Test
  void ofTwoConcurrentRunsOneRunsAndOneIsSkipped() throws Exception {
    String job = "CONCURRENT_" + UUID.randomUUID().toString().substring(0, 8);
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CompletableFuture<JobRun> first =
        CompletableFuture.supplyAsync(
            () ->
                jobRuns.execute(
                    job,
                    JobTrigger.SCHEDULED,
                    () -> {
                      started.countDown();
                      await(release);
                      return new JobOutcome(1, "done");
                    }));
    assertThat(started.await(30, TimeUnit.SECONDS)).isTrue();

    JobRun second =
        jobRuns.execute(job, JobTrigger.SCHEDULED, () -> new JobOutcome(1, "must not run"));
    assertThat(second.getStatus()).isEqualTo(JobRunStatus.SKIPPED_LOCKED);
    assertThat(second.getItemsProcessed()).isZero();
    assertThat(second.getFinishedAt()).isNotNull();

    release.countDown();
    assertThat(first.get(30, TimeUnit.SECONDS).getStatus()).isEqualTo(JobRunStatus.SUCCEEDED);
    assertThat(
            jobRuns.execute(job, JobTrigger.MANUAL, () -> new JobOutcome(2, "again")).getStatus())
        .isEqualTo(JobRunStatus.SUCCEEDED);
    assertThat(jobRuns.latest(job)).isPresent();
  }

  @Test
  void aJobIsNeverRunWithoutItsLock() {
    List<JobRun> failures = new ArrayList<>();
    JobFailureListener listener = failures::add;
    JobLock unavailable =
        name -> {
          throw new IllegalStateException("lock store down");
        };
    JobRunService guarded =
        new JobRunService(
            runRepository, currentUser, clock, List.of(listener), transactionManager, unavailable);
    JobRun run =
        guarded.execute(
            "LOCK_DOWN_" + UUID.randomUUID().toString().substring(0, 8),
            JobTrigger.SCHEDULED,
            () -> {
              throw new AssertionError("must not run");
            });
    assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILED);
    assertThat(run.getMessage()).contains("Job lock unavailable");
    assertThat(failures).hasSize(1);
  }

  @Test
  void anUnreachableDatabaseIsReported() throws SQLException {
    DataSource broken = mock(DataSource.class);
    when(broken.getConnection()).thenThrow(new SQLException("down"));
    assertThatThrownBy(() -> new AdvisoryJobLock(broken).tryAcquire("ANY"))
        .isInstanceOf(IllegalStateException.class);
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(30, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
