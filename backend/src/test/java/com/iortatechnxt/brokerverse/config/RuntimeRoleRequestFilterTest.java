package com.iortatechnxt.brokerverse.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.runtime.CurrentRuntimeRole;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** The HTTP surface of each runtime role. */
class RuntimeRoleRequestFilterTest {

  @ParameterizedTest(name = "{0} {1} -> {2}")
  @CsvSource({
    "web, /api/v1/auth/me, 200",
    "web, /swagger-ui.html, 200",
    "web, /actuator/health/readiness, 200",
    "web, /integration/v1/ping, 404",
    "jobs, /actuator/health/liveness, 200",
    "jobs, /actuator/prometheus, 200",
    "jobs, /api/v1/auth/me, 404",
    "jobs, /integration/v1/ping, 404",
    "integration, /integration/v1/ping, 200",
    "integration, /integration, 200",
    "integration, /actuator/health, 200",
    "integration, /api/v1/auth/me, 404",
    "integration, /integrationx, 404",
    "all, /api/v1/auth/me, 200",
    "all, /integration/v1/ping, 200",
    "all, /actuator/health, 200",
  })
  void eachRoleServesItsOwnPaths(String role, String path, int status) throws Exception {
    RuntimeRoleRequestFilter filter =
        new RuntimeRoleRequestFilter(
            new CurrentRuntimeRole(
                new MockEnvironment().withProperty("brokerverse.runtime.role", role)));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(chain.getRequest() != null).isEqualTo(status == 200);
  }

  @ParameterizedTest
  @CsvSource({"web, /bibs/api/v1/journals, 200", "jobs, /bibs/api/v1/journals, 404"})
  void theContextPathIsIgnored(String role, String uri, int status) throws Exception {
    RuntimeRoleRequestFilter filter =
        new RuntimeRoleRequestFilter(
            new CurrentRuntimeRole(
                new MockEnvironment().withProperty("brokerverse.runtime.role", role)));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    request.setContextPath("/bibs");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(status);
  }
}
