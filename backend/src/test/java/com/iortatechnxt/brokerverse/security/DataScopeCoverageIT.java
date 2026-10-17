package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.security.DataScopeMappings.Mechanism;
import com.iortatechnxt.brokerverse.security.api.DataScopeBodyAdvice;
import com.iortatechnxt.brokerverse.security.api.DataScopeInterceptor;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

/**
 * Walks every request mapping of the running application and proves that each endpoint taking a
 * company is checked against the data scope (DATA_SCOPE_DESIGN.md section 4): the data scope
 * interceptor is in the handler chain of its path, or the body advice reads its body, or the method
 * checks explicitly ({@code @CompanyScoped}); the system-to-system APIs are the listed exemption.
 * The figures are written to {@code target/data-scope-coverage.txt}.
 */
@IntegrationTest
class DataScopeCoverageIT {

  private static final String INTEGRATION = "/integration/";

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping mappings;

  @Autowired private ApplicationContext context;

  @Test
  void everyEndpointTakingACompanyIsCovered() throws IOException {
    MappedInterceptor interceptor = dataScopeInterceptor();
    int endpoints = 0;
    int takingCompany = 0;
    int guarded = 0;
    Set<String> exempt = new TreeSet<>();
    List<String> uncovered = new ArrayList<>();
    int[] byMechanism = new int[Mechanism.values().length];
    for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mappings.getHandlerMethods().entrySet()) {
      endpoints++;
      Set<Mechanism> mechanisms = DataScopeMappings.mechanisms(e.getValue().getMethod());
      if (mechanisms.isEmpty()) {
        continue;
      }
      takingCompany++;
      mechanisms.forEach(m -> byMechanism[m.ordinal()]++);
      for (String pattern : e.getKey().getPatternValues()) {
        String name = e.getKey().getMethodsCondition() + " " + pattern;
        if (pattern.startsWith(INTEGRATION)) {
          exempt.add(name);
        } else if (mechanisms.contains(Mechanism.UNCOVERED)
            || mechanisms.contains(Mechanism.INTERCEPTOR)
                && !interceptor.matches(request(pattern))) {
          uncovered.add(name + " -> " + e.getValue());
        }
      }
      if (!mechanisms.contains(Mechanism.UNCOVERED)) {
        guarded++;
      }
    }
    write(endpoints, takingCompany, guarded, byMechanism, exempt);
    assertThat(uncovered).as("endpoints taking a company without a data scope check").isEmpty();
    assertThat(takingCompany).isGreaterThan(400);
    assertThat(guarded).isEqualTo(takingCompany);
    assertThat(context.getBean(DataScopeBodyAdvice.class).getClass())
        .hasAnnotation(ControllerAdvice.class);
  }

  private MappedInterceptor dataScopeInterceptor() {
    return Arrays.stream(mappings.getAdaptedInterceptors())
        .filter(MappedInterceptor.class::isInstance)
        .map(MappedInterceptor.class::cast)
        .filter(m -> m.getInterceptor() instanceof DataScopeInterceptor)
        .findFirst()
        .orElseThrow();
  }

  private static MockHttpServletRequest request(String pattern) {
    String path = pattern.replaceAll("\\{[^}]+}", "1");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    ServletRequestPathUtils.parseAndCache(request);
    return request;
  }

  private static void write(
      int endpoints, int takingCompany, int guarded, int[] byMechanism, Set<String> exempt)
      throws IOException {
    StringBuilder text = new StringBuilder(256);
    text.append("Request mappings: ")
        .append(endpoints)
        .append("\nEndpoints taking a company: ")
        .append(takingCompany)
        .append("\nGuarded: ")
        .append(guarded)
        .append('\n');
    for (Mechanism m : Mechanism.values()) {
      text.append("  ").append(m).append(": ").append(byMechanism[m.ordinal()]).append('\n');
    }
    text.append("Exempt (system-to-system): ").append(exempt).append('\n');
    Files.writeString(
        Path.of("target", "data-scope-coverage.txt"), text.toString(), StandardCharsets.UTF_8);
  }
}
