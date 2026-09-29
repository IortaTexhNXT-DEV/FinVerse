package com.iortatechnxt.brokerverse.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

/** The access token checks fail closed when the denylist or the session log cannot be read. */
class JwtAuthenticationFilterTest {

  private static final String SECRET = "unit-signing-key-for-the-filter-0123456789";

  private final Clock clock = Clock.systemUTC();
  private final JwtTokenService tokens =
      new JwtTokenService(
          new SecurityProperties(SECRET, Duration.ofHours(8), List.of(), null), clock);
  private final TokenRevocationStore revocations = mock(TokenRevocationStore.class);
  private final UserSessionLog sessions = mock(UserSessionLog.class);
  private final UserDetailsService users =
      username -> User.withUsername(username).password("x").authorities("AUDIT_VIEW").build();
  private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
  private final List<Object> events = new ArrayList<>();
  private final JwtAuthenticationFilter filter =
      new JwtAuthenticationFilter(
          tokens,
          users,
          revocations,
          sessions,
          new SecurityStoreAlarm(meters, events::add, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)));

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private MockHttpServletResponse call(String token) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
    request.addHeader("Authorization", "Bearer " + token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  private String token() {
    return tokens.issueAccess("auditor", "session-1", Duration.ofMinutes(15)).token();
  }

  @Test
  void anOpenSessionAuthenticates() throws Exception {
    when(revocations.isRevoked(any())).thenReturn(false);
    when(sessions.check("session-1")).thenReturn(UserSessionLog.SessionState.OPEN);
    assertThat(call(token()).getStatus()).isEqualTo(200);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
  }

  @Test
  void anUnknownOrEndedSessionAuthenticatesNobody() throws Exception {
    when(sessions.check("session-1")).thenReturn(UserSessionLog.SessionState.UNTRACKED);
    call(token());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    when(sessions.check("session-1")).thenReturn(UserSessionLog.SessionState.ENDED);
    call(token());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void withoutTheDenylistTheSessionLogDecides() throws Exception {
    when(revocations.isRevoked(any())).thenThrow(new IllegalStateException("redis down"));
    when(sessions.check("session-1")).thenReturn(UserSessionLog.SessionState.ENDED);
    call(token());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(meters.counter(SecurityStoreAlarm.METRIC, "store", "denylist").count()).isEqualTo(1);
    assertThat(events).singleElement().isInstanceOf(SecurityStoreUnavailable.class);
  }

  @Test
  void withoutTheSessionLogTheRequestIsRefused() throws Exception {
    when(sessions.check("session-1")).thenThrow(new IllegalStateException("database down"));
    MockHttpServletResponse response = call(token());
    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(response.getContentAsString()).contains("SIGN_IN_CHECK_UNAVAILABLE");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(meters.counter(SecurityStoreAlarm.METRIC, "store", "sessions").count()).isEqualTo(1);
  }

  @Test
  void aChallengeTokenIsNotAnAccessToken() throws Exception {
    String challenge =
        tokens
            .issueChallenge(
                "auditor", JwtTokenService.MFA_CHALLENGE, "PASSWORD", Duration.ofMinutes(5))
            .token();
    when(sessions.check(any())).thenReturn(UserSessionLog.SessionState.OPEN);
    call(challenge);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(tokens.parseChallenge(challenge, JwtTokenService.MFA_CHALLENGE)).isPresent();
    assertThat(tokens.parseChallenge(token(), JwtTokenService.MFA_CHALLENGE)).isEmpty();
  }
}
