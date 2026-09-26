package com.iortatechnxt.brokerverse.config;

import com.iortatechnxt.brokerverse.security.service.IntegrationSecurityProperties;
import com.iortatechnxt.brokerverse.security.service.IntegrationSecurityProperties.ApiAccess;
import com.iortatechnxt.brokerverse.security.service.IntegrationTokenDecoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Security chain of the system-to-system APIs under {@code /integration/**}, separate from the user
 * chain of {@link SecurityConfig} and evaluated first.
 *
 * <ul>
 *   <li>Only OAuth 2.0 access tokens issued by the API gateway (Apigee X) are accepted, validated
 *       by {@link IntegrationTokenDecoder}: a user token issued by BIBS is refused here, and a
 *       gateway token is refused on the user APIs (the user chain only knows BIBS-signed tokens).
 *   <li>Every API needs a rule in {@code brokerverse.integration.security.apis} naming its path and
 *       the scopes the token must carry; any other path is refused (deny by default). When several
 *       rules match, the most specific path (longest pattern) applies.
 *   <li>Stateless: no session, no request cache, no CSRF token (no cookie is ever involved).
 * </ul>
 *
 * <p>Unauthenticated or invalid tokens get 401 with a {@code WWW-Authenticate: Bearer} header; a
 * valid token lacking a scope gets 403.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IntegrationSecurityProperties.class)
public class IntegrationSecurityConfig {

  /** Paths of the chain. */
  public static final String INTEGRATION_PATHS =
      RuntimeRoleRequestFilter.INTEGRATION_PREFIX + "/**";

  private static final String SCOPE_PREFIX = "SCOPE_";

  /**
   * The integration security chain.
   *
   * @param http http security
   * @param properties token validation and access rules
   * @return filter chain
   * @throws Exception on configuration error
   */
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  // Spring's builder API declares "throws Exception", which this factory method must propagate.
  @SuppressWarnings("PMD.SignatureDeclareThrowsException")
  public SecurityFilterChain integrationFilterChain(
      HttpSecurity http, IntegrationSecurityProperties properties) throws Exception {
    JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    scopes.setAuthoritiesClaimName(properties.scopeClaim());
    scopes.setAuthorityPrefix(SCOPE_PREFIX);
    JwtAuthenticationConverter authentication = new JwtAuthenticationConverter();
    authentication.setJwtGrantedAuthoritiesConverter(scopes);
    http.securityMatcher(INTEGRATION_PATHS)
        .csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(
            o ->
                o.jwt(
                    j ->
                        j.decoder(IntegrationTokenDecoder.create(properties))
                            .jwtAuthenticationConverter(authentication)))
        .authorizeHttpRequests(
            a -> {
              for (ApiAccess api : rules(properties.apis())) {
                a.requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(api.path()))
                    .access(access(api));
              }
              a.anyRequest().denyAll();
            });
    return http.build();
  }

  /**
   * Access rules ordered from the most specific path to the least specific one.
   *
   * @param apis rules by API name
   * @return ordered rules with a path
   */
  static List<ApiAccess> rules(Map<String, ApiAccess> apis) {
    List<ApiAccess> rules = new ArrayList<>();
    for (ApiAccess api : apis.values()) {
      if (api != null && api.path() != null && !api.path().isBlank()) {
        rules.add(api);
      }
    }
    rules.sort(
        Comparator.comparingInt((ApiAccess api) -> api.path().length())
            .reversed()
            .thenComparing(ApiAccess::path));
    return rules;
  }

  private static AuthorizationManager<RequestAuthorizationContext> access(ApiAccess api) {
    AuthorizationManager<RequestAuthorizationContext> manager =
        AuthenticatedAuthorizationManager.authenticated();
    for (String scope : api.scopes()) {
      AuthorizationManager<RequestAuthorizationContext> required =
          AuthorityAuthorizationManager.hasAuthority(SCOPE_PREFIX + scope.trim());
      manager = AuthorizationManagers.allOf(manager, required);
    }
    return manager;
  }
}
