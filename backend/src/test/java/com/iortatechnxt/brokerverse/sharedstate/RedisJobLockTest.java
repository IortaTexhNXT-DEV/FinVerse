package com.iortatechnxt.brokerverse.sharedstate;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cache.service.RedisSettings;
import com.iortatechnxt.brokerverse.sharedstate.service.RedisJobLock;
import com.iortatechnxt.brokerverse.support.EmbeddedRedis;
import com.iortatechnxt.brokerverse.system.service.JobLock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

/** The Redis job lock (SET NX PX, fencing token, renewed lease) against the in-JVM Redis. */
class RedisJobLockTest extends JobLockContract {

  private static final RedisSettings SETTINGS = new RedisSettings(true, "test:");
  private static final Duration LEASE = Duration.ofMillis(600);

  private final StringRedisTemplate redis = EmbeddedRedis.template();
  private final RedisJobLock lock = new RedisJobLock(redis, SETTINGS, LEASE);
  private final RedisJobLock otherInstance =
      new RedisJobLock(EmbeddedRedis.template(), SETTINGS, LEASE);

  @AfterEach
  void stopRenewers() {
    lock.destroy();
    otherInstance.destroy();
  }

  @Override
  JobLock lock() {
    return lock;
  }

  @Override
  JobLock otherInstanceLock() {
    return otherInstance;
  }

  @Test
  void theLeaseIsRenewedWhileTheJobRuns() throws InterruptedException {
    String job = "RENEW_" + UUID.randomUUID();
    try (JobLock.Lease lease = lock.tryAcquire(job).orElseThrow()) {
      Thread.sleep(LEASE.toMillis() * 3);
      assertThat(otherInstance.tryAcquire(job)).isEmpty();
      assertThat(redis.opsForValue().get("test:joblock:" + job))
          .endsWith(":" + lease.fencingToken());
    }
    assertThat(redis.hasKey("test:joblock:" + job)).isFalse();
  }

  @Test
  void aLockWhoseOwnerStoppedRenewingExpires() throws InterruptedException {
    String job = "CRASHED_" + UUID.randomUUID();
    redis.opsForValue().set("test:joblock:" + job, "crashed-instance:1", Duration.ofMillis(200));
    assertThat(lock.tryAcquire(job)).isEmpty();
    Thread.sleep(400);
    Optional<JobLock.Lease> lease = lock.tryAcquire(job);
    assertThat(lease).isPresent();
    lease.get().close();
  }

  @Test
  void aLeaseNeverReleasesALockTakenOverByAnotherInstance() {
    String job = "TAKEN_OVER_" + UUID.randomUUID();
    JobLock.Lease stale = lock.tryAcquire(job).orElseThrow();
    // The lease ran out and another instance took the lock.
    redis.opsForValue().set("test:joblock:" + job, "other-instance:99");
    stale.close();
    assertThat(redis.opsForValue().get("test:joblock:" + job)).isEqualTo("other-instance:99");
    redis.delete("test:joblock:" + job);
  }
}
