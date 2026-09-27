package com.iortatechnxt.brokerverse.config;

import com.iortatechnxt.brokerverse.common.runtime.CurrentRuntimeRole;
import com.iortatechnxt.brokerverse.common.runtime.RuntimeRole;
import com.iortatechnxt.brokerverse.common.runtime.Workload;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits the HTTP surface of an instance to its runtime role, before security and the controllers:
 *
 * <ul>
 *   <li>the actuator ({@code /actuator/**}: health probes, metrics) answers on every role;
 *   <li>{@code /integration/**} (APIs published through the API gateway) answers on the roles with
 *       the integration workload ({@code integration}, {@code all});
 *   <li>everything else (user screens and APIs, API documentation) answers on the roles with the
 *       user workload ({@code web}, {@code all}).
 * </ul>
 *
 * <p>Other requests get 404, so a {@code jobs} instance serves health and metrics only and the
 * ingress path of one deployment never reaches the endpoints of another.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RuntimeRoleRequestFilter extends OncePerRequestFilter {

  /** Prefix of the system-to-system APIs published through the API gateway. */
  public static final String INTEGRATION_PREFIX = "/integration";

  private static final String ACTUATOR_PREFIX = "/actuator";
  private static final String ERROR_PATH = "/error";

  private final RuntimeRole role;

  /**
   * Creates the filter.
   *
   * @param role runtime role of this instance
   */
  public RuntimeRoleRequestFilter(CurrentRuntimeRole role) {
    this.role = role.role();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (serves(path(request))) {
      chain.doFilter(request, response);
    } else {
      response.setStatus(HttpStatus.NOT_FOUND.value());
    }
  }

  /**
   * Tells whether this instance answers a path.
   *
   * @param path request path without the context path
   * @return true when the role serves it
   */
  boolean serves(String path) {
    if (under(path, ACTUATOR_PREFIX) || ERROR_PATH.equals(path)) {
      return true;
    }
    if (under(path, INTEGRATION_PREFIX)) {
      return role.runs(Workload.INTEGRATION);
    }
    return role.runs(Workload.USER_API);
  }

  private static boolean under(String path, String prefix) {
    return path.equals(prefix) || path.startsWith(prefix + "/");
  }

  private static String path(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String context = request.getContextPath();
    return context != null && !context.isEmpty() && uri.startsWith(context)
        ? uri.substring(context.length())
        : uri;
  }
}
