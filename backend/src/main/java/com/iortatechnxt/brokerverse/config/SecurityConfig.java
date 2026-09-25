package com.iortatechnxt.brokerverse.config;

import com.iortatechnxt.brokerverse.security.service.JwtAuthenticationFilter;
import com.iortatechnxt.brokerverse.security.service.JwtTokenService;
import com.iortatechnxt.brokerverse.security.service.LoginRateLimitFilter;
import com.iortatechnxt.brokerverse.security.service.LoginRateLimiter;
import com.iortatechnxt.brokerverse.security.service.SecurityProperties;
import com.iortatechnxt.brokerverse.security.service.TokenRevocationStore;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * HTTP security: stateless JWT authentication, method-level permission checks, CORS and security
 * headers.
 *
 * <p>CSRF protection is disabled because the API is stateless and authenticates with a bearer token
 * in the Authorization header (no cookies), so browsers never send credentials implicitly.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final int BCRYPT_STRENGTH = 12;
  private static final String BEARER_PREFIX = "Bearer ";

  /** Login is exempt from CSRF: it carries credentials in the body and establishes no session. */
  private static final RequestMatcher LOGIN =
      PathPatternRequestMatcher.withDefaults()
          .matcher(HttpMethod.POST, LoginRateLimitFilter.LOGIN_PATH);

  /**
   * Password hashing (BCrypt, cost 12).
   *
   * @return encoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
  }

  /**
   * Authentication manager used by the login endpoint.
   *
   * @param userDetailsService user loader
   * @param passwordEncoder encoder
   * @return manager
   */
  @Bean
  public AuthenticationManager authenticationManager(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  /**
   * Security filter chain.
   *
   * @param http http security
   * @param tokens token service
   * @param userDetailsService user loader
   * @param properties security properties
   * @param revocations token denylist (logout)
   * @param loginRateLimiter login rate limit
   * @return filter chain
   * @throws Exception on configuration error
   */
  @Bean
  // Spring's builder API declares "throws Exception", which this factory method must propagate.
  @SuppressWarnings("PMD.SignatureDeclareThrowsException")
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      JwtTokenService tokens,
      UserDetailsService userDetailsService,
      SecurityProperties properties,
      TokenRevocationStore revocations,
      LoginRateLimiter loginRateLimiter)
      throws Exception {
    JwtAuthenticationFilter jwt =
        new JwtAuthenticationFilter(tokens, userDetailsService, revocations);
    http.csrf(c -> c.ignoringRequestMatchers(SecurityConfig::carriesBearerToken, LOGIN))
        .cors(c -> c.configurationSource(corsSource(properties.allowedOrigins())))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .headers(
            h ->
                h.contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                    .referrerPolicy(
                        r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(LOGIN)
                    .permitAll()
                    .requestMatchers(
                        "/error",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .hasAuthority("SYSTEM_PARAMETER_MANAGE")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new LoginRateLimitFilter(loginRateLimiter), JwtAuthenticationFilter.class);
    return http.build();
  }

  /**
   * Requests authenticated by an explicit bearer token cannot be forged cross-site: browsers never
   * attach the Authorization header automatically, so CSRF tokens add nothing for them.
   *
   * @param request request
   * @return true when an Authorization: Bearer header is present
   */
  private static boolean carriesBearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    return header != null && header.startsWith(BEARER_PREFIX);
  }

  private static CorsConfigurationSource corsSource(List<String> origins) {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(origins == null ? List.of() : origins);
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    cors.setExposedHeaders(List.of("Content-Disposition"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    return source;
  }
}
