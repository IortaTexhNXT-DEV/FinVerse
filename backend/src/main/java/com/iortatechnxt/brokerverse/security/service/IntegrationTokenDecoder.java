package com.iortatechnxt.brokerverse.security.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

/**
 * Builds the decoder of the gateway-issued tokens of {@code /integration/**}: signature against the
 * gateway's key set (asymmetric algorithms only, so a user token signed by BIBS never verifies),
 * then issuer, audience and validity. Not a Spring bean on purpose: it serves only the integration
 * security chain and must never be picked up for the user APIs.
 */
public final class IntegrationTokenDecoder {

  private static final Logger LOG = LoggerFactory.getLogger(IntegrationTokenDecoder.class);

  private IntegrationTokenDecoder() {}

  /**
   * Creates the decoder. When the key set, issuer or audiences are missing, the decoder refuses
   * every token.
   *
   * @param properties integration token settings
   * @return decoder
   */
  public static JwtDecoder create(IntegrationSecurityProperties properties) {
    if (!properties.configured()) {
      LOG.warn(
          "Integration API tokens are not configured (brokerverse.integration.security.*);"
              + " every /integration request is refused");
      return token -> {
        throw new BadJwtException("Integration API tokens are not configured");
      };
    }
    SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
    http.setConnectTimeout(properties.jwksTimeout());
    http.setReadTimeout(properties.jwksTimeout());
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri())
            .jwsAlgorithms(algorithms -> algorithms.addAll(algorithms(properties)))
            .restOperations(new RestTemplate(http))
            .build();
    decoder.setJwtValidator(validator(properties));
    return decoder;
  }

  /**
   * Validator of the claims: validity window, issuer and audience.
   *
   * @param properties integration token settings
   * @return validator
   */
  static OAuth2TokenValidator<Jwt> validator(IntegrationSecurityProperties properties) {
    Set<String> accepted = Set.copyOf(properties.audiences());
    return new DelegatingOAuth2TokenValidator<>(
        new JwtTimestampValidator(properties.clockSkew()),
        new JwtIssuerValidator(properties.issuer()),
        new JwtClaimValidator<Collection<String>>(
            JwtClaimNames.AUD,
            audience -> audience != null && audience.stream().anyMatch(accepted::contains)));
  }

  private static List<SignatureAlgorithm> algorithms(IntegrationSecurityProperties properties) {
    List<SignatureAlgorithm> algorithms = new ArrayList<>();
    for (String name : properties.jwsAlgorithms()) {
      SignatureAlgorithm algorithm = SignatureAlgorithm.from(name.trim());
      if (algorithm == null) {
        throw new IllegalStateException(
            "brokerverse.integration.security.jws-algorithms: unsupported algorithm " + name);
      }
      algorithms.add(algorithm);
    }
    return algorithms;
  }
}
