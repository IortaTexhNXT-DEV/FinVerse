package com.iortatechnxt.brokerverse.system.service;

import java.util.Optional;

/**
 * Port: cluster-wide lock of a background job, so a job scheduled on every instance runs on one of
 * them at a time. {@link JobRunService} takes it for every run; a run that finds it taken is
 * recorded as {@code SKIPPED_LOCKED}.
 *
 * <p>Implemented on Redis ({@code SET NX PX} with a fencing token and a lease renewed while the job
 * runs) or, when Redis is disabled, on a PostgreSQL advisory lock (module {@code sharedstate}).
 */
public interface JobLock {

  /**
   * Tries to take the lock of a job without waiting.
   *
   * @param jobName job name
   * @return the held lease, empty when another run holds the lock
   */
  Optional<Lease> tryAcquire(String jobName);

  /** A held job lock; closing it releases the lock. */
  interface Lease extends AutoCloseable {

    /**
     * Fencing token: strictly increasing per job across all instances, so work done under a lease
     * that was lost (process paused beyond its lease) can be recognised as stale.
     *
     * @return fencing token
     */
    long fencingToken();

    /** Releases the lock (only if still held by this lease). */
    @Override
    void close();
  }
}
