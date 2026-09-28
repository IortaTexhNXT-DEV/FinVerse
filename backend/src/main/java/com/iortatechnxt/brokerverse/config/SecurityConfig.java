package com.iortatechnxt.brokerverse.config;

import com.iortatechnxt.brokerverse.security.api.RefreshCookies;
import com.iortatechnxt.brokerverse.security.service.JwtAuthenticationFilter;
import com.iortatechnxt.brokerverse.security.service.LoginRateLimitFilter;
import com.iortatechnxt.brokerverse.security.service.LoginRateLimiter;
import com.iortatechnxt.brokerverse.security.service.SecurityProperties;
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
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * HTTP security: stateless JWT authentication, method-level permission checks, CORS and security
 * headers.
 *
 * <p>CSRF protection is disabled for requests with a bearer token in the Authorization header (no
 * cookie is involved, so browsers never send credentials implicitly) and for the anonymous sign-in
 * steps ({@link #SIGN_IN}). The refresh of the access token is the one request authenticated by a
 * cookie: the cookie is HttpOnly and SameSite=Strict, and the endpoint also requires the {@code
 * X-Requested-With} header, which a cross-site form cannot set and a cross-site script may only
 * send after a CORS preflight that fails for any origin not allowed.
 *
 * <p>Headers: HSTS (one year, sub-domains) on every HTTPS answer; a Content Security Policy that
 * allows nothing ({@code default-src 'none'}) on the API answers and the API documentation's own
 * resources on {@code /swagger-ui} (local and dev only); no referrer; framing refused.
 *
 * <p>Actuator: health and info are open; the metrics need METRICS_VIEW on the application port, and
 * are open on the separate management port ({@link ManagementPort}), which only the monitoring
 * namespace can reach.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final int BCRYPT_STRENGTH = 12;
  private static final String BEARER_PREFIX = "Bearer ";
  private static final long HSTS_SECONDS = 31_536_000L;
  private static final String CSP = "Content-Security-Policy";
  private static final String API_CSP =
      "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";
  private static final String DOCS_CSP =
      "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline';"
          + " frame-ancestors 'none'; base-uri 'self'; form-action 'self'";

  /** Login is exempt from CSRF: it carries credentials in the body and establishes no session. */
  private static final RequestMatcher LOGIN =
      PathPatternRequestMatcher.withDefaults()
          .matcher(HttpMethod.POST, LoginRateLimitFilter.LOGIN_PATH);

  /**
   * "Forgot password?" (UAM-NFR-37): request a link, check it and set the password with it. Open to
   * anonymous callers and, like login, exempt from CSRF (no session, no cookie); the link token in
   * the body is the credential.
   */
  private static final RequestMatcher PASSWORD_RESET =
      PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/password-reset/**");

  /**
   * The other anonymous sign-in steps: the options of the sign-in page, the renewal of the access
   * token, the second factor with the challenge of the first step, and the single sign-on (start,
   * the answers of the identity provider, the completion with the one-time ticket, the SAML
   * metadata). Each carries its own credential (challenge, ticket, state, signed assertion).
   */
  private static final RequestMatcher SIGN_IN =
      new OrRequestMatcher(
          PathPatternRequestMatcher.withDefaults()
              .matcher(HttpMethod.GET, "/api/v1/auth/sign-in-options"),
          PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/refresh"),
          PathPatternRequestMatcher.withDefaults()
              .matcher(HttpMethod.POST, "/api/v1/auth/mfa/verify"),
          PathPatternRequestMatcher.withDefaults()
              .matcher(HttpMethod.POST, "/api/v1/auth/mfa/enrolment/*"),
          PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/sso/**"));

  private static final RequestMatcher API_DOCS =
      new OrRequestMatcher(
          PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
          PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html"));

  private static final String[] OPEN_ACTUATOR = {
    "/actuator/health", "/actuator/health/**", "/actuator/info", "/livez", "/readyz"
  };

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
   * @param properties security properties
   * @param loginRateLimiter login rate limit
   * @param jwtFilters the bearer token filter
   * @param managementPort the separate management port, when there is one
   * @return filter chain
   * @throws Exception on configuration error
   */
  @Bean
  // Spring's builder API declares "throws Exception", which this factory method must propagate.
  @SuppressWarnings("PMD.SignatureDeclareThrowsException")
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      SecurityProperties properties,
      LoginRateLimiter loginRateLimiter,
      JwtFilterFactory jwtFilters,
      ManagementPort managementPort)
      throws Exception {
    JwtAuthenticationFilter jwt = jwtFilters.create();
    RequestMatcher onManagementPort = managementPort::receives;
    http.csrf(
            c ->
                c.ignoringRequestMatchers(
                    SecurityConfig::carriesBearerToken, LOGIN, PASSWORD_RESET, SIGN_IN))
        .cors(c -> c.configurationSource(corsSource(properties.allowedOrigins())))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .headers(
            h ->
                h.httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_SECONDS))
                    .addHeaderWriter(
                        new DelegatingRequestMatcherHeaderWriter(
                            API_DOCS, new StaticHeadersWriter(CSP, DOCS_CSP)))
                    .addHeaderWriter(
                        new DelegatingRequestMatcherHeaderWriter(
                            new NegatedRequestMatcher(API_DOCS),
                            new StaticHeadersWriter(CSP, API_CSP)))
                    .referrerPolicy(
                        r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(LOGIN, PASSWORD_RESET, SIGN_IN)
                    .permitAll()
                    .requestMatchers(OPEN_ACTUATOR)
                    .permitAll()
                    .requestMatchers(
                        "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(onManagementPort)
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .hasAuthority("METRICS_VIEW")
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
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type", RefreshCookies.REQUEST_HEADER));
    cors.setExposedHeaders(List.of("Content-Disposition"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    return source;
  }
}
