package com.iortatechnxt.brokerverse.security.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Validation of the OAuth 2.0 access tokens that the API gateway (Apigee X) issues to other BDO
 * systems calling {@code /integration/**}, bound from {@code brokerverse.integration.security.*}.
 *
 * <p>A token is accepted when its signature verifies against the gateway's JSON Web Key Set, its
 * issuer is {@code issuer}, its audience contains one of {@code audiences}, it is within its
 * validity (with {@code clockSkew}) and it carries every scope that the matching {@code apis} rule
 * requires. A path under {@code /integration/} without a rule is refused (deny by default). While
 * {@code jwkSetUri}, {@code issuer} or {@code audiences} is missing every integration call is
 * refused.
 *
 * @param jwkSetUri HTTPS address of the gateway's JSON Web Key Set
 * @param issuer expected {@code iss} claim
 * @param audiences accepted {@code aud} values (any one must be present)
 * @param scopeClaim claim carrying the scopes, space-separated or a list (default {@code scope})
 * @param jwsAlgorithms accepted signature algorithms (default {@code RS256})
 * @param clockSkew tolerance on the {@code exp} and {@code nbf} claims (default 60 seconds)
 * @param jwksTimeout connect and read timeout of the key set download (default 5 seconds)
 * @param apis access rules by API name
 */
@ConfigurationProperties(prefix = "brokerverse.integration.security")
public record IntegrationSecurityProperties(
    String jwkSetUri,
    String issuer,
    List<String> audiences,
    String scopeClaim,
    List<String> jwsAlgorithms,
    Duration clockSkew,
    Duration jwksTimeout,
    Map<String, ApiAccess> apis) {

  private static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(60);
  private static final Duration DEFAULT_JWKS_TIMEOUT = Duration.ofSeconds(5);

  /** Applies the defaults. */
  public IntegrationSecurityProperties {
    audiences = audiences == null ? List.of() : List.copyOf(audiences);
    scopeClaim = scopeClaim == null || scopeClaim.isBlank() ? "scope" : scopeClaim;
    jwsAlgorithms =
        jwsAlgorithms == null || jwsAlgorithms.isEmpty()
            ? List.of("RS256")
            : List.copyOf(jwsAlgorithms);
    clockSkew = Objects.requireNonNullElse(clockSkew, DEFAULT_CLOCK_SKEW);
    jwksTimeout = Objects.requireNonNullElse(jwksTimeout, DEFAULT_JWKS_TIMEOUT);
    apis = apis == null ? Map.of() : Map.copyOf(apis);
  }

  /**
   * Tells whether tokens can be validated at all.
   *
   * @return true when the key set, the issuer and at least one audience are configured
   */
  public boolean configured() {
    return notBlank(jwkSetUri) && notBlank(issuer) && audiences.stream().anyMatch(a -> notBlank(a));
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  /**
   * Access rule of one integration API.
   *
   * @param path path pattern under {@code /integration/} (for example {@code
   *     /integration/v1/receipts/**})
   * @param scopes scopes the token must all carry; empty: any valid gateway token
   */
  public record ApiAccess(String path, List<String> scopes) {

    /** Applies the defaults. */
    public ApiAccess {
      scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }
  }
}
