package com.iortatechnxt.brokerverse.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Largest page of a list ({@code brokerverse.http.max-page-size}, environment {@code
 * BROKERVERSE_MAX_PAGE_SIZE}, default 500): a request of the user APIs whose {@code size} parameter
 * is above it, or not a whole number, is answered with 400 ({@code PAGE_SIZE_TOO_LARGE}) before
 * security and the controllers, so no list can be read in one unbounded query.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class PageSizeLimitFilter extends OncePerRequestFilter {

  /** Error code of a refused page size. */
  public static final String CODE = "PAGE_SIZE_TOO_LARGE";

  private static final String SIZE = "size";

  private final int maxSize;

  /**
   * Creates the filter.
   *
   * @param maxSize largest page size
   */
  public PageSizeLimitFilter(@Value("${brokerverse.http.max-page-size:500}") int maxSize) {
    this.maxSize = maxSize;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String[] sizes = request.getParameterValues(SIZE);
    if (sizes != null) {
      for (String size : sizes) {
        if (!allowed(size)) {
          refuse(response);
          return;
        }
      }
    }
    chain.doFilter(request, response);
  }

  private boolean allowed(String size) {
    try {
      int value = Integer.parseInt(size.trim());
      return value <= maxSize;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private void refuse(HttpServletResponse response) throws IOException {
    String body =
        "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
            + "\"detail\":\"A page holds at most "
            + maxSize
            + " rows\",\"code\":\""
            + CODE
            + "\"}";
    response.setStatus(HttpStatus.BAD_REQUEST.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
  }
}
