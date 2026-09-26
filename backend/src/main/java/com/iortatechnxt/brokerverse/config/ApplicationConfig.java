package com.iortatechnxt.brokerverse.config;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.service.LoginProtectionProperties;
import com.iortatechnxt.brokerverse.security.service.SecurityProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.time.Clock;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Cross-cutting beans: clock, JPA auditing, configuration properties and OpenAPI metadata.
 * Scheduling is enabled per runtime role by {@link SchedulingConfiguration}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableConfigurationProperties({SecurityProperties.class, LoginProtectionProperties.class})
public class ApplicationConfig {

  private static final String BEARER = "bearerAuth";

  /**
   * System clock (UTC). Injected everywhere so tests can fix time.
   *
   * @return clock
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  /**
   * Supplies the current user to {@code @CreatedBy}/{@code @LastModifiedBy}.
   *
   * @param currentUser current user resolver
   * @return auditor aware
   */
  @Bean
  public AuditorAware<String> auditorAware(CurrentUser currentUser) {
    return () -> Optional.of(currentUser.username());
  }

  /**
   * OpenAPI description served at /v3/api-docs and /swagger-ui.html.
   *
   * @return OpenAPI model
   */
  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("iNXT BrokerVerse API")
                .version("1.0.0")
                .description("Insurance General Ledger and Finance platform by IortaTechNXT"))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER));
  }
}
