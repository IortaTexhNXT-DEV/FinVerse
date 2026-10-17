package com.iortatechnxt.brokerverse.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits of the system-to-system APIs under {@code /integration/**}, checked before the token:
 *
 * <ul>
 *   <li>A declared body larger than {@code brokerverse.integration.max-body-size} (default 256 KB;
 *       a SCIM User is a few KB) is answered with 413. Bodies without a length are counted by
 *       {@link JsonBodyLimitFilter}.
 *   <li>Each client address may send {@code brokerverse.integration.rate-limit.requests-per-minute}
 *       requests per minute (default 300; 0 switches the limit off); beyond it the answer is 429
 *       with {@code Retry-After}. The count is per instance; the API gateway applies its own quota
 *       in front.
 * </ul>
 *
 * <p>The refusals carry the SCIM error schema and no detail of the request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class IntegrationRequestGuard extends OncePerRequestFilter {

  private static final long MINUTE_MILLIS = 60_000L;
  private static final int MAX_CLIENTS = 10_000;
  private static final String SCIM_ERROR = "urn:ietf:params:scim:api:messages:2.0:Error";

  private final long maxBytes;
  private final int perMinute;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  /**
   * Creates the guard.
   *
   * @param maxSize largest body, e.g. {@code 256KB}
   * @param perMinute requests per client address and minute, 0 for no limit
   * @param clock clock
   */
  public IntegrationRequestGuard(
      @Value("${brokerverse.integration.max-body-size:256KB}") String maxSize,
      @Value("${brokerverse.integration.rate-limit.requests-per-minute:300}") int perMinute,
      Clock clock) {
    this.maxBytes = DataSize.parse(maxSize.trim()).toBytes();
    this.perMinute = perMinute;
    this.clock = clock;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return !path.startsWith(RuntimeRoleRequestFilter.INTEGRATION_PREFIX + "/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxBytes) {
      refuse(response, HttpStatus.PAYLOAD_TOO_LARGE, "The request is larger than allowed");
    } else if (perMinute > 0 && !admitted(String.valueOf(request.getRemoteAddr()))) {
      response.setHeader(HttpHeaders.RETRY_AFTER, "60");
      refuse(response, HttpStatus.TOO_MANY_REQUESTS, "Too many requests; retry later");
    } else {
      chain.doFilter(request, response);
    }
  }

  /** Counts a request of a client in the current minute; false beyond the limit. */
  boolean admitted(String client) {
    long minute = clock.millis() / MINUTE_MILLIS;
    if (windows.size() > MAX_CLIENTS) {
      windows.values().removeIf(w -> w.minute != minute);
    }
    Window window =
        windows.compute(
            client, (k, w) -> w == null || w.minute != minute ? new Window(minute, 1) : w.next());
    return window.count <= perMinute;
  }

  private static void refuse(HttpServletResponse response, HttpStatus status, String detail)
      throws IOException {
    String body =
        "{\"schemas\":[\""
            + SCIM_ERROR
            + "\"],\"status\":\""
            + status.value()
            + "\",\"detail\":\""
            + detail
            + "\"}";
    response.setStatus(status.value());
    response.setContentType("application/scim+json");
    response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
  }

  /** Requests of a client in one minute. */
  private record Window(long minute, int count) {
    Window next() {
      return new Window(minute, count + 1);
    }
  }
}
