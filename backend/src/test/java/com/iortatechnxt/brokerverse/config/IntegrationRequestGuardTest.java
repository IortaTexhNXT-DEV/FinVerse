package com.iortatechnxt.brokerverse.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IntegrationRequestGuardTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-15T08:00:10Z"), ZoneOffset.UTC);

  private static MockHttpServletResponse call(IntegrationRequestGuard guard, String path, int size)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    request.setRemoteAddr("10.0.0.5");
    request.setContent(new byte[size]);
    MockHttpServletResponse response = new MockHttpServletResponse();
    guard.doFilter(request, response, new MockFilterChain());
    return response;
  }

  @Test
  void aClientBeyondItsRequestsOfTheMinuteGetsTooManyRequests() throws Exception {
    IntegrationRequestGuard guard = new IntegrationRequestGuard("256KB", 3, CLOCK);
    for (int i = 0; i < 3; i++) {
      assertThat(call(guard, "/integration/v1/scim/v2/Users", 10).getStatus()).isEqualTo(200);
    }
    MockHttpServletResponse refused = call(guard, "/integration/v1/scim/v2/Users", 10);
    assertThat(refused.getStatus()).isEqualTo(429);
    assertThat(refused.getHeader("Retry-After")).isEqualTo("60");
    assertThat(refused.getContentAsString())
        .contains("urn:ietf:params:scim:api:messages:2.0:Error");
    assertThat(call(guard, "/api/v1/auth/me", 10).getStatus()).isEqualTo(200);
  }

  @Test
  void aBodyLargerThanTheLimitIsRefused() throws Exception {
    IntegrationRequestGuard guard = new IntegrationRequestGuard("1KB", 0, CLOCK);
    assertThat(call(guard, "/integration/v1/scim/v2/Users", 1024).getStatus()).isEqualTo(200);
    assertThat(call(guard, "/integration/v1/scim/v2/Users", 1025).getStatus()).isEqualTo(413);
  }
}
