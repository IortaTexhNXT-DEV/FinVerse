package com.iortatechnxt.brokerverse.config;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * The metrics need no user token on the management port only; on the application port they need
 * METRICS_VIEW (a service credential), and every answer carries HSTS and the API's CSP.
 */
@IntegrationTest
class MetricsAccessIT {

  private static final int MANAGEMENT = 19_090;

  @Autowired private MockMvc mvc;
  @Autowired private ManagementPort managementPort;

  private void listen(int port) {
    WebServerApplicationContext context = mock(WebServerApplicationContext.class);
    when(context.getServerNamespace()).thenReturn("management");
    WebServer server = mock(WebServer.class);
    when(server.getPort()).thenReturn(port);
    WebServerInitializedEvent event = mock(WebServerInitializedEvent.class);
    when(event.getApplicationContext()).thenReturn(context);
    when(event.getWebServer()).thenReturn(server);
    managementPort.onApplicationEvent(event);
  }

  private static RequestPostProcessor onPort(int port) {
    return request -> {
      request.setLocalPort(port);
      return request;
    };
  }

  @AfterEach
  void noManagementPort() {
    listen(-1);
  }

  @Test
  void onTheApplicationPortTheMetricsNeedMetricsView() throws Exception {
    mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Security-Policy", startsWith("default-src 'none'")));
    mvc.perform(get("/readyz")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(authorities = "SYSTEM_PARAMETER_MANAGE")
  void aParameterAdministratorNoLongerReadsTheMetrics() throws Exception {
    mvc.perform(get("/actuator/prometheus")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "METRICS_VIEW")
  void aServiceCredentialWithMetricsViewReadsThem() throws Exception {
    mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
  }

  @Test
  void theManagementPortServesTheMetricsWithoutToken() throws Exception {
    listen(MANAGEMENT);
    mvc.perform(get("/actuator/prometheus").with(onPort(MANAGEMENT))).andExpect(status().isOk());
    mvc.perform(get("/actuator/prometheus").with(onPort(8080)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void httpsAnswersCarryHsts() throws Exception {
    mvc.perform(get("/actuator/health").secure(true))
        .andExpect(
            header()
                .string(
                    "Strict-Transport-Security",
                    allOf(startsWith("max-age=31536000"), containsString("includeSubDomains"))));
  }
}
