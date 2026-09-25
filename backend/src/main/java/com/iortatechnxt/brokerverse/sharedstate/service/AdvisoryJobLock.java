package com.iortatechnxt.brokerverse.sharedstate.service;

import com.iortatechnxt.brokerverse.cache.service.RedisSettings;
import com.iortatechnxt.brokerverse.system.service.JobLock;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Job lock on PostgreSQL when Redis is disabled: a session-level advisory lock ({@code
 * pg_try_advisory_lock(0x4A4F42, hashtext(<job>))}) held on a dedicated connection for the length
 * of the run. The database releases it when the connection ends, so a crashed instance never leaves
 * a job locked; no lease renewal is needed. The fencing token comes from the sequence {@code
 * sys_job_lock_fence_seq}.
 */
@Component
@ConditionalOnProperty(
    name = RedisSettings.ENABLED_PROPERTY,
    havingValue = "false",
    matchIfMissing = true)
public class AdvisoryJobLock implements JobLock {

  private static final Logger LOG = LoggerFactory.getLogger(AdvisoryJobLock.class);

  /** First key of every job lock ("JOB"), so job locks never collide with other advisory locks. */
  static final int LOCK_CLASS = 0x4A4F42;

  private static final String TRY_LOCK = "select pg_try_advisory_lock(?, hashtext(?))";
  private static final String UNLOCK = "select pg_advisory_unlock(?, hashtext(?))";
  private static final String NEXT_FENCE = "select nextval('sys_job_lock_fence_seq')";

  private final DataSource dataSource;

  /**
   * Creates the lock.
   *
   * @param dataSource connection pool (a connection is held for each running job)
   */
  public AdvisoryJobLock(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  // The connection stays open while the job runs; the lease closes it (see AdvisoryLease.close).
  @Override
  public Optional<Lease> tryAcquire(String jobName) {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      connection.setAutoCommit(true);
      if (!lockCall(connection, true, jobName)) {
        connection.close();
        return Optional.empty();
      }
      return Optional.of(new AdvisoryLease(connection, jobName, nextFence(connection)));
    } catch (SQLException ex) {
      closeQuietly(connection);
      throw new IllegalStateException("Advisory job lock unavailable for " + jobName, ex);
    }
  }

  private static boolean lockCall(Connection connection, boolean lock, String jobName)
      throws SQLException {
    try (PreparedStatement statement =
        lock ? connection.prepareStatement(TRY_LOCK) : connection.prepareStatement(UNLOCK)) {
      statement.setInt(1, LOCK_CLASS);
      statement.setString(2, jobName);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() && result.getBoolean(1);
      }
    }
  }

  private static long nextFence(Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(NEXT_FENCE);
        ResultSet result = statement.executeQuery()) {
      if (!result.next()) {
        throw new SQLException("No fencing token returned");
      }
      return result.getLong(1);
    }
  }

  private static void closeQuietly(Connection connection) {
    if (connection == null) {
      return;
    }
    try {
      connection.close();
    } catch (SQLException ex) {
      LOG.warn("Job lock connection could not be closed", ex);
    }
  }

  /** A lock held on a dedicated connection. */
  private static final class AdvisoryLease implements Lease {

    private final Connection connection;
    private final String jobName;
    private final long token;
    private final AtomicBoolean released = new AtomicBoolean();

    private AdvisoryLease(Connection connection, String jobName, long token) {
      this.connection = connection;
      this.jobName = jobName;
      this.token = token;
    }

    @Override
    public long fencingToken() {
      return token;
    }

    @Override
    public void close() {
      if (!released.compareAndSet(false, true)) {
        return;
      }
      try (Connection held = connection) {
        unlock(held);
      } catch (SQLException ex) {
        LOG.warn("Job lock connection of {} could not be closed", jobName, ex);
      }
    }

    private void unlock(Connection held) {
      try {
        lockCall(held, false, jobName);
      } catch (SQLException ex) {
        // A pooled connection must never keep the lock: end its database session instead.
        LOG.warn("Advisory lock of {} not released explicitly; ending its session", jobName, ex);
        abortQuietly(held);
      }
    }

    private static void abortQuietly(Connection connection) {
      try {
        connection.abort(Runnable::run);
      } catch (SQLException ex) {
        LOG.warn("Job lock connection could not be aborted", ex);
      }
    }
  }
}
