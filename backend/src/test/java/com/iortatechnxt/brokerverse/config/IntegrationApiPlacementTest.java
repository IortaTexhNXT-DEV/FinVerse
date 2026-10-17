package com.iortatechnxt.brokerverse.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The system-to-system APIs stay under the integration security chain: every controller of the
 * package {@code integration.api} (the connectivity check among them) is mapped under {@code
 * /integration}, which only {@link IntegrationSecurityConfig} serves, and no other controller maps
 * a path there.
 */
class IntegrationApiPlacementTest {

  @Test
  void integrationControllersAreMappedUnderTheIntegrationChainOnly() throws Exception {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    List<String> misplaced = new ArrayList<>();
    int integrationControllers = 0;
    for (BeanDefinition definition :
        scanner.findCandidateComponents("com.iortatechnxt.brokerverse")) {
      Class<?> type =
          ClassUtils.forName(definition.getBeanClassName(), getClass().getClassLoader());
      RequestMapping mapping = type.getAnnotation(RequestMapping.class);
      String[] paths = mapping == null ? new String[0] : mapping.value();
      boolean integrationPackage = type.getPackageName().endsWith(".integration.api");
      for (String path : paths) {
        boolean underIntegration =
            path.startsWith(RuntimeRoleRequestFilter.INTEGRATION_PREFIX + "/");
        if (integrationPackage != underIntegration) {
          misplaced.add(type.getSimpleName() + " " + path);
        }
      }
      if (integrationPackage) {
        integrationControllers++;
        if (paths.length == 0) {
          misplaced.add(type.getSimpleName() + " without a class-level mapping");
        }
      }
    }
    assertThat(integrationControllers).isPositive();
    assertThat(misplaced).isEmpty();
  }
}
