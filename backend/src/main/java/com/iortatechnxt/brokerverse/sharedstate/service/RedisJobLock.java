package com.iortatechnxt.brokerverse.sharedstate.service;

import com.iortatechnxt.brokerverse.cache.service.RedisSettings;
import com.iortatechnxt.brokerverse.system.service.JobLock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Job lock on Redis: {@code SET <prefix>joblock:<job> <owner> NX PX <lease>}. The owner value is
 * {@code <instance>:<fencing token>}, the token coming from {@code INCR
 * <prefix>joblock:<job>:fence}. While the job runs the lease is renewed every third of its length
 * (compare-and-{@code PEXPIRE}), and it is released with a compare-and-{@code DEL}, so an instance
 * never extends or deletes a lock another instance took after its lease ran out. A crashed instance
 * frees the lock when its lease expires.
 */
@Component
@ConditionalOnProperty(name = RedisSettings.ENABLED_PROPERTY, havingValue = "true")
public class RedisJobLock implements JobLock, DisposableBean {

  private static final Logger LOG = LoggerFactory.getLogger(RedisJobLock.class);
  private static final int RENEWALS_PER_LEASE = 3;

  private static final RedisScript<Long> RENEW =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then "
              + "return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end",
          Long.class);
  private static final RedisScript<Long> RELEASE =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then "
              + "return redis.call('del', KEYS[1]) else return 0 end",
          Long.class);

  private final StringRedisTemplate redis;
  private final RedisSettings settings;
  private final Duration lease;
  private final String instanceId = UUID.randomUUID().toString();
  private final ScheduledExecutorService renewer =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread thread = new Thread(r, "job-lock-renewer");
            thread.setDaemon(true);
            return thread;
          });

  /**
   * Creates the lock.
   *
   * @param redis Redis template
   * @param settings key prefix
   * @param lease lease length ({@code brokerverse.jobs.lock-lease}, default 2 minutes)
   */
  public RedisJobLock(
      StringRedisTemplate redis,
      RedisSettings settings,
      @Value("${brokerverse.jobs.lock-lease:PT2M}") Duration lease) {
    this.redis = redis;
    this.settings = settings;
    this.lease = lease;
  }

  @Override
  public Optional<Lease> tryAcquire(String jobName) {
    String key = settings.key("joblock", jobName);
    Long fence = redis.opsForValue().increment(key + ":fence");
    long token = fence == null ? 0 : fence;
    String owner = instanceId + ":" + token;
    if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, owner, lease))) {
      return Optional.empty();
    }
    long period = Math.max(1, lease.toMillis() / RENEWALS_PER_LEASE);
    ScheduledFuture<?> renewal =
        renewer.scheduleAtFixedRate(
            () -> renew(key, owner, jobName), period, period, TimeUnit.MILLISECONDS);
    return Optional.of(new RedisLease(key, owner, token, renewal));
  }

  private void renew(String key, String owner, String jobName) {
    try {
      Long renewed = redis.execute(RENEW, List.of(key), owner, String.valueOf(lease.toMillis()));
      if (renewed == null || renewed == 0) {
        LOG.warn("Job {} lost its lock lease; another instance may start it", jobName);
      }
    } catch (RuntimeException ex) {
      LOG.error("Job {} lock lease could not be renewed", jobName, ex);
    }
  }

  @Override
  public void destroy() {
    renewer.shutdownNow();
  }

  /** A lease held on Redis. */
  private final class RedisLease implements Lease {

    private final String key;
    private final String owner;
    private final long token;
    private final ScheduledFuture<?> renewal;

    private RedisLease(String key, String owner, long token, ScheduledFuture<?> renewal) {
      this.key = key;
      this.owner = owner;
      this.token = token;
      this.renewal = renewal;
    }

    @Override
    public long fencingToken() {
      return token;
    }

    @Override
    public void close() {
      renewal.cancel(false);
      try {
        redis.execute(RELEASE, List.of(key), owner);
      } catch (RuntimeException ex) {
        LOG.error("Job lock {} could not be released; it expires with its lease", key, ex);
      }
    }
  }
}
