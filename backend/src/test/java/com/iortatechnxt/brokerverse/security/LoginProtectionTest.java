package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.service.LoginAttemptTracker;
import com.iortatechnxt.brokerverse.security.service.LoginProtectionProperties;
import com.iortatechnxt.brokerverse.security.service.LoginRateLimitFilter;
import com.iortatechnxt.brokerverse.security.service.LoginRateLimiter;
import com.iortatechnxt.brokerverse.security.service.SharedCounterStore;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Login rate limit (HTTP 429) and the shared failed-login count applied to the user record. */
class LoginProtectionTest {

  /** Counters of one "cluster" (shared by every tracker or limiter built on it). */
  private static final class MemoryCounters implements SharedCounterStore {
    private final Map<String, Long> values = new ConcurrentHashMap<>();
    private boolean down;

    @Override
    public long increment(String key, Duration window) {
      if (down) {
        throw new IllegalStateException("store down");
      }
      return values.merge(key, 1L, Long::sum);
    }

    @Override
    public long current(String key) {
      return values.getOrDefault(key, 0L);
    }

    @Override
    public void reset(String key) {
      if (down) {
        throw new IllegalStateException("store down");
      }
      values.remove(key);
    }
  }

  @Test
  void theLoginEndpointAnswers429AboveTheLimit() throws Exception {
    MemoryCounters counters = new MemoryCounters();
    LoginRateLimitFilter filter =
        new LoginRateLimitFilter(
            new LoginRateLimiter(counters, new LoginProtectionProperties(2, null, null)));
    assertThat(login(filter).getStatus()).isEqualTo(200);
    assertThat(login(filter).getStatus()).isEqualTo(200);
    MockHttpServletResponse limited = login(filter);
    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
    assertThat(limited.getContentAsString()).contains("LOGIN_RATE_LIMITED");

    MockHttpServletRequest other = new MockHttpServletRequest("GET", "/api/v1/auth/me");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(other, response, new MockFilterChain());
    assertThat(response.getStatus()).isEqualTo(200);

    counters.down = true;
    assertThat(login(filter).getStatus()).isEqualTo(200); // store down: lockout still protects
  }

  @Test
  void theSharedCountLocksTheAccountAndSurvivesAStoreOutage() {
    MemoryCounters counters = new MemoryCounters();
    LoginProtectionProperties settings = new LoginProtectionProperties(null, null, null);
    LoginAttemptTracker podA = new LoginAttemptTracker(counters, settings);
    LoginAttemptTracker podB = new LoginAttemptTracker(counters, settings);
    AppUser user = new AppUser("sharedlock", "Shared Lock", "hash");

    user.recordFailedLogins(podA.recordFailure("sharedlock", user.getFailedAttempts()), 3);
    int stale = user.getFailedAttempts();
    user.recordFailedLogins(podA.recordFailure("SharedLock", stale), 3);
    assertThat(user.isLocked()).isFalse();
    user.recordFailedLogins(podB.recordFailure("sharedlock", stale), 3); // concurrent, stale read
    assertThat(user.getFailedAttempts()).isEqualTo(3);
    assertThat(user.isLocked()).isTrue();

    counters.down = true;
    assertThat(podA.recordFailure("sharedlock", 4)).isEqualTo(5);
    podA.reset("sharedlock");
    assertThat(settings.failedAttemptWindow()).isEqualTo(Duration.ofDays(1));
    assertThat(settings.maxAttemptsPerWindow())
        .isEqualTo(LoginProtectionProperties.DEFAULT_MAX_ATTEMPTS_PER_WINDOW);
  }

  private static MockHttpServletResponse login(LoginRateLimitFilter filter) throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", LoginRateLimitFilter.LOGIN_PATH);
    request.setRemoteAddr("192.0.2.10");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }
}
