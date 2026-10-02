package com.iortatechnxt.brokerverse.sharedstate;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.system.service.JobLock;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Behaviour every job lock must show (Redis and PostgreSQL advisory lock). */
abstract class JobLockContract {

  /**
   * The lock of one application instance.
   *
   * @return lock
   */
  abstract JobLock lock();

  /**
   * The lock as seen by a second instance.
   *
   * @return lock of the second instance
   */
  abstract JobLock otherInstanceLock();

  @Test
  void oneHolderAtATimeWithIncreasingFencingTokens() {
    String job = "LOCK_TEST_" + UUID.randomUUID();
    Optional<JobLock.Lease> first = lock().tryAcquire(job);
    assertThat(first).isPresent();
    assertThat(otherInstanceLock().tryAcquire(job)).isEmpty();
    assertThat(lock().tryAcquire(job)).isEmpty();

    Optional<JobLock.Lease> otherJob = otherInstanceLock().tryAcquire(job + "_OTHER");
    assertThat(otherJob).isPresent();
    otherJob.get().close();

    long firstToken = first.get().fencingToken();
    first.get().close();
    Optional<JobLock.Lease> second = otherInstanceLock().tryAcquire(job);
    assertThat(second).isPresent();
    assertThat(second.get().fencingToken()).isGreaterThan(firstToken);
    second.get().close();
    second.get().close(); // closing twice is harmless
    try (JobLock.Lease third = lock().tryAcquire(job).orElseThrow()) {
      assertThat(third.fencingToken()).isGreaterThan(second.get().fencingToken());
    }
  }
}
