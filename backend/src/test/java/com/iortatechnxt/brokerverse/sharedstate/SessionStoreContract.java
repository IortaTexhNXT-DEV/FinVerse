package com.iortatechnxt.brokerverse.sharedstate;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.security.service.LoginAttemptTracker;
import com.iortatechnxt.brokerverse.security.service.LoginProtectionProperties;
import com.iortatechnxt.brokerverse.security.service.LoginRateLimiter;
import com.iortatechnxt.brokerverse.security.service.SharedCounterStore;
import com.iortatechnxt.brokerverse.security.service.TokenRevocationStore;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Behaviour every session store must show (Redis and database): fixed-window counters, the token
 * denylist, and counters shared by two instances of the application.
 */
abstract class SessionStoreContract {

  /**
   * The store of one application instance.
   *
   * @return counters and denylist
   */
  abstract SharedCounterStore counters();

  /**
   * The same store as seen by a second instance (another connection).
   *
   * @return counters of the second instance
   */
  abstract SharedCounterStore otherInstanceCounters();

  /**
   * The denylist.
   *
   * @return denylist
   */
  abstract TokenRevocationStore revocations();

  private static String key() {
    return "test:" + UUID.randomUUID();
  }

  @Test
  void countersCountResetAndRestartAfterTheirWindow() throws InterruptedException {
    String key = key();
    assertThat(counters().current(key)).isZero();
    assertThat(counters().increment(key, Duration.ofMinutes(1))).isEqualTo(1);
    assertThat(counters().increment(key, Duration.ofMinutes(1))).isEqualTo(2);
    assertThat(counters().current(key)).isEqualTo(2);
    counters().reset(key);
    assertThat(counters().current(key)).isZero();

    String shortLived = key();
    assertThat(counters().increment(shortLived, Duration.ofMillis(300))).isEqualTo(1);
    assertThat(counters().increment(shortLived, Duration.ofMillis(300))).isEqualTo(2);
    Thread.sleep(600);
    assertThat(counters().current(shortLived)).isZero();
    assertThat(counters().increment(shortLived, Duration.ofMillis(300))).isEqualTo(1);
  }

  @Test
  void aCounterIsSharedByEveryInstance() {
    LoginProtectionProperties settings = new LoginProtectionProperties(2, null, null);
    LoginAttemptTracker podA = new LoginAttemptTracker(counters(), settings);
    LoginAttemptTracker podB = new LoginAttemptTracker(otherInstanceCounters(), settings);
    String user = "shared" + UUID.randomUUID().toString().substring(0, 8);
    // Both pods read the same stale database count (1) at the same time: none is lost.
    assertThat(podA.recordFailure(user, 0)).isEqualTo(1);
    assertThat(podA.recordFailure(user, 1)).isEqualTo(2);
    assertThat(podB.recordFailure(user, 1)).isEqualTo(3);
    podB.reset(user);
    assertThat(podA.recordFailure(user, 0)).isEqualTo(1);

    LoginRateLimiter limiterA = new LoginRateLimiter(counters(), settings);
    LoginRateLimiter limiterB = new LoginRateLimiter(otherInstanceCounters(), settings);
    String address = "10.0.0." + UUID.randomUUID().toString().substring(0, 4);
    assertThat(limiterA.tryAcquire(address)).isTrue();
    assertThat(limiterB.tryAcquire(address)).isTrue();
    assertThat(limiterA.tryAcquire(address)).isFalse();
    assertThat(limiterA.window()).isEqualTo(Duration.ofMinutes(1));
  }

  @Test
  void revokedTokensStayRevokedUntilTheyExpire() {
    String revoked = UUID.randomUUID().toString();
    revocations().revoke(revoked, "auditor", Instant.now().plusSeconds(60));
    assertThat(revocations().isRevoked(revoked)).isTrue();
    assertThat(revocations().isRevoked(UUID.randomUUID().toString())).isFalse();
    revocations().revoke(revoked, "auditor", Instant.now().plusSeconds(60));
    assertThat(revocations().isRevoked(revoked)).isTrue();

    String expired = UUID.randomUUID().toString();
    revocations().revoke(expired, "auditor", Instant.now().minusSeconds(1));
    assertThat(revocations().isRevoked(expired)).isFalse();
  }
}
