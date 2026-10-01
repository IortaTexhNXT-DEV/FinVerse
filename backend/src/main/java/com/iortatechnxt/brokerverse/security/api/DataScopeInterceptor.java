package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.common.security.DataScope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Checks the company and branch a user API call names against the data scope of the signed-in user,
 * before the controller runs (DATA_SCOPE_DESIGN.md section 4): the query parameter and the path
 * variable {@value DataScopeTargets#COMPANY} and the query parameter {@value
 * DataScopeTargets#BRANCH}. Registered on {@code /api/**} only: the system-to-system APIs under
 * {@code /integration/**} are exempt. A value that is not a number is left to the request binding
 * (HTTP 400).
 */
@Configuration(proxyBeanMethods = false)
public class DataScopeInterceptor implements HandlerInterceptor, WebMvcConfigurer {

  /** Paths of the user APIs the interceptor checks. */
  public static final String USER_API = "/api/**";

  private final DataScope dataScope;

  /**
   * Creates the interceptor.
   *
   * @param dataScope data scope guard
   */
  public DataScopeInterceptor(DataScope dataScope) {
    this.dataScope = dataScope;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this).addPathPatterns(USER_API);
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (handler instanceof HandlerMethod) {
      List<Long> companies = companies(request);
      companies.forEach(dataScope::requireCompany);
      Long branch = number(request.getParameter(DataScopeTargets.BRANCH));
      if (branch != null) {
        dataScope.requireBranch(companies.isEmpty() ? null : companies.get(0), branch);
      }
    }
    return true;
  }

  private static List<Long> companies(HttpServletRequest request) {
    List<Long> found = new ArrayList<>();
    String[] values = request.getParameterValues(DataScopeTargets.COMPANY);
    if (values != null) {
      for (String v : values) {
        addNumber(v, found);
      }
    }
    if (request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)
            instanceof Map<?, ?> variables
        && variables.get(DataScopeTargets.COMPANY) instanceof String v) {
      addNumber(v, found);
    }
    return found;
  }

  private static void addNumber(String value, List<Long> found) {
    Long n = number(value);
    if (n != null) {
      found.add(n);
    }
  }

  private static Long number(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.valueOf(value.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
