package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.service.SecurityProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Lockout threshold from configuration (BDOI NFR 1-2, COLLECTIONS_DESIGN section 8). */
class LoginLockoutTest {

  @Test
  void theAccountLocksAtTheConfiguredThreshold() {
    AppUser user = new AppUser("lockout", "Lockout Test", "hash");
    user.recordFailedLogin(3);
    user.recordFailedLogin(3);
    assertThat(user.isLocked()).isFalse();
    user.recordFailedLogin(3);
    assertThat(user.isLocked()).isTrue();
    user.unlock();
    assertThat(user.isLocked()).isFalse();
    assertThat(user.getFailedAttempts()).isZero();
  }

  @Test
  void theDefaultThresholdIsFive() {
    SecurityProperties defaults =
        new SecurityProperties("x".repeat(32), Duration.ofHours(8), List.of(), null);
    assertThat(defaults.maxFailedAttempts())
        .isEqualTo(SecurityProperties.DEFAULT_MAX_FAILED_ATTEMPTS)
        .isEqualTo(5);
    assertThat(
            new SecurityProperties("x".repeat(32), Duration.ofHours(8), List.of(), 3)
                .maxFailedAttempts())
        .isEqualTo(3);
  }
}
