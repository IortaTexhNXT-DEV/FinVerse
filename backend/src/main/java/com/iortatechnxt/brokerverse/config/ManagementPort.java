package com.iortatechnxt.brokerverse.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * The separate management port ({@code management.server.port}, {@code
 * BROKERVERSE_MANAGEMENT_PORT}) once it listens. The health probes and the Prometheus metrics are
 * answered there without a token: the port is not published by the load balancer and the Kubernetes
 * NetworkPolicy lets only the monitoring namespace reach it. When the actuator shares the
 * application port there is no management port and the metrics need the METRICS_VIEW permission.
 */
@Component
public class ManagementPort implements ApplicationListener<WebServerInitializedEvent> {

  private static final String NAMESPACE = "management";

  private volatile int port = -1;

  @Override
  public void onApplicationEvent(WebServerInitializedEvent event) {
    if (NAMESPACE.equals(event.getApplicationContext().getServerNamespace())) {
      port = event.getWebServer().getPort();
    }
  }

  /**
   * Whether a request came in on the management port.
   *
   * @param request request
   * @return true on the separate management port
   */
  public boolean receives(HttpServletRequest request) {
    int listening = port;
    return listening > 0 && request.getLocalPort() == listening;
  }
}
