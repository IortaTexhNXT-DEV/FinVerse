package com.iortatechnxt.brokerverse.events.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every HTTP request a correlation id: the caller's {@code X-Correlation-Id} when it is a
 * plain token (letters, digits, dash, underscore; at most 64), otherwise a new UUID. The id is put
 * in the logging context ({@code correlationId}), echoed in the response header and copied into the
 * envelope of every integration event the request publishes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  /** Request and response header. */
  public static final String HEADER = "X-Correlation-Id";

  /** Logging context key. */
  public static final String MDC_KEY = "correlationId";

  private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9_-]{1,64}");

  /**
   * The correlation id of the current request or job.
   *
   * @return id from the logging context, or a new one when there is none
   */
  public static String current() {
    return Optional.ofNullable(MDC.get(MDC_KEY)).orElseGet(() -> UUID.randomUUID().toString());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String supplied = request.getHeader(HEADER);
    String id =
        supplied != null && SAFE.matcher(supplied).matches()
            ? supplied
            : UUID.randomUUID().toString();
    MDC.put(MDC_KEY, id);
    response.setHeader(HEADER, id);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
