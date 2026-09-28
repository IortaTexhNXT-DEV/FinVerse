package com.iortatechnxt.brokerverse.security.service.sso;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * OpenID Connect relying party (authorisation code flow with PKCE S256, confidential client with
 * {@code client_secret_basic}). The endpoints come from the provider's discovery document unless
 * configured. The ID token is validated with the provider's key set: signature (the configured
 * algorithm only), issuer, audience (the client id; {@code azp} when several), expiry and
 * not-before with the clock skew, and the nonce of the request.
 */
@Component
public class OidcClient {

  /** Path of the redirect URI under the base address. */
  public static final String CALLBACK_PATH = "/api/v1/auth/sso/oidc/callback";

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
  private static final String DEFAULT_USERNAME_CLAIM = "preferred_username";

  private final SsoProperties properties;
  private final Clock clock;
  private final RestClient http;
  private final AtomicReference<Endpoints> endpoints = new AtomicReference<>();
  private final AtomicReference<NimbusJwtDecoder> decoder = new AtomicReference<>();

  /**
   * Creates the client.
   *
   * @param properties single sign-on settings
   * @param clock clock
   */
  public OidcClient(SsoProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    factory.setReadTimeout(READ_TIMEOUT);
    this.http = RestClient.builder().requestFactory(factory).build();
  }

  /**
   * Whether a provider is configured.
   *
   * @return true when the issuer and client id are set
   */
  public boolean configured() {
    return properties.oidc().configured() && !properties.base().isEmpty();
  }

  /**
   * The address of the provider's sign-in page for a request.
   *
   * @param state state of the request
   * @param nonce nonce of the ID token
   * @param codeVerifier PKCE verifier (its S256 challenge is sent)
   * @return address to open in the browser
   */
  public String authorizationUrl(String state, String nonce, String codeVerifier) {
    SsoProperties.Oidc oidc = properties.oidc();
    String scopes = oidc.scopes() == null || oidc.scopes().isBlank() ? "openid" : oidc.scopes();
    String base = endpoints().authorization();
    return base
        + (base.contains("?") ? "&" : "?")
        + "response_type=code"
        + "&client_id="
        + encode(oidc.clientId())
        + "&redirect_uri="
        + encode(redirectUri())
        + "&scope="
        + encode(scopes)
        + "&state="
        + encode(state)
        + "&nonce="
        + encode(nonce)
        + "&code_challenge="
        + challenge(codeVerifier)
        + "&code_challenge_method=S256";
  }

  /**
   * Exchanges the authorisation code for the ID token and validates it.
   *
   * @param code authorisation code
   * @param codeVerifier PKCE verifier of the request
   * @param nonce nonce of the request
   * @return the identity
   * @throws SsoException when the provider refuses or the token is not valid
   */
  public SsoIdentity exchange(String code, String codeVerifier, String nonce) {
    SsoProperties.Oidc oidc = properties.oidc();
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", code);
    form.add("redirect_uri", redirectUri());
    form.add("code_verifier", codeVerifier);
    JsonNode answer;
    try {
      answer =
          http.post()
              .uri(URI.create(endpoints().token()))
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .accept(MediaType.APPLICATION_JSON)
              .header(HttpHeaders.AUTHORIZATION, basic(oidc.clientId(), oidc.clientSecret()))
              .body(form)
              .retrieve()
              .body(JsonNode.class);
    } catch (RestClientException ex) {
      throw new SsoException(
          SsoException.PROVIDER_ERROR,
          "The token endpoint refused the code: " + ex.getMessage(),
          ex);
    }
    if (answer == null || !answer.hasNonNull("id_token")) {
      throw new SsoException(SsoException.PROVIDER_ERROR, "The token answer has no ID token");
    }
    Jwt idToken;
    try {
      idToken = decoder().decode(answer.get("id_token").asText());
    } catch (JwtException ex) {
      throw new SsoException(SsoException.INVALID, "ID token refused: " + ex.getMessage(), ex);
    }
    if (nonce == null || !nonce.equals(idToken.getClaimAsString("nonce"))) {
      throw new SsoException(SsoException.INVALID, "ID token refused: the nonce does not match");
    }
    String claim =
        properties.usernameClaim() == null || properties.usernameClaim().isBlank()
            ? DEFAULT_USERNAME_CLAIM
            : properties.usernameClaim();
    String username = idToken.getClaimAsString(claim);
    if (username == null || username.isBlank()) {
      throw new SsoException(SsoException.INVALID, "ID token without the claim " + claim);
    }
    return new SsoIdentity(username.trim(), groups(idToken));
  }

  /**
   * The redirect URI registered at the provider.
   *
   * @return redirect URI
   */
  public String redirectUri() {
    return properties.base() + CALLBACK_PATH;
  }

  private List<String> groups(Jwt idToken) {
    String claim = properties.groupsClaim();
    if (claim == null || claim.isBlank()) {
      return List.of();
    }
    Object value = idToken.getClaims().get(claim);
    List<String> groups = new ArrayList<>();
    if (value instanceof Collection<?> values) {
      values.forEach(v -> groups.add(String.valueOf(v)));
    } else if (value != null) {
      groups.add(String.valueOf(value));
    }
    return groups;
  }

  private NimbusJwtDecoder decoder() {
    NimbusJwtDecoder current = decoder.get();
    if (current != null) {
      return current;
    }
    SsoProperties.Oidc oidc = properties.oidc();
    String algorithm =
        oidc.jwsAlgorithm() == null || oidc.jwsAlgorithm().isBlank()
            ? "RS256"
            : oidc.jwsAlgorithm();
    NimbusJwtDecoder built =
        NimbusJwtDecoder.withJwkSetUri(endpoints().jwkSet())
            .jwsAlgorithm(SignatureAlgorithm.from(algorithm.trim()))
            .build();
    JwtTimestampValidator timestamps = new JwtTimestampValidator(properties.clockSkew());
    timestamps.setClock(clock);
    built.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            timestamps, new JwtIssuerValidator(oidc.issuer()), audience(oidc.clientId())));
    decoder.compareAndSet(null, built);
    return decoder.get();
  }

  private static OAuth2TokenValidator<Jwt> audience(String clientId) {
    return jwt -> {
      List<String> audiences = jwt.getAudience();
      boolean ours = audiences != null && audiences.contains(clientId);
      boolean party =
          audiences == null
              || audiences.size() <= 1
              || clientId.equals(jwt.getClaimAsString("azp"));
      return ours && party
          ? OAuth2TokenValidatorResult.success()
          : OAuth2TokenValidatorResult.failure(
              new OAuth2Error("invalid_token", "The ID token is not for this client", null));
    };
  }

  private Endpoints endpoints() {
    Endpoints current = endpoints.get();
    if (current != null) {
      return current;
    }
    SsoProperties.Oidc oidc = properties.oidc();
    String authorization = oidc.authorizationUri();
    String token = oidc.tokenUri();
    String jwks = oidc.jwkSetUri();
    if (blank(authorization) || blank(token) || blank(jwks)) {
      JsonNode discovery = discover(oidc.issuer());
      authorization =
          blank(authorization) ? text(discovery, "authorization_endpoint") : authorization;
      token = blank(token) ? text(discovery, "token_endpoint") : token;
      jwks = blank(jwks) ? text(discovery, "jwks_uri") : jwks;
    }
    endpoints.compareAndSet(null, new Endpoints(authorization, token, jwks));
    return endpoints.get();
  }

  private JsonNode discover(String issuer) {
    String base = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    try {
      JsonNode document =
          http.get()
              .uri(URI.create(base + "/.well-known/openid-configuration"))
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(JsonNode.class);
      if (document == null) {
        throw new SsoException(SsoException.PROVIDER_ERROR, "Empty discovery document");
      }
      if (!issuer.equals(text(document, "issuer"))) {
        throw new SsoException(
            SsoException.PROVIDER_ERROR, "The discovery document names another issuer");
      }
      return document;
    } catch (RestClientException ex) {
      throw new SsoException(
          SsoException.PROVIDER_ERROR, "Discovery document not available: " + ex.getMessage(), ex);
    }
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.asText().isBlank()) {
      throw new SsoException(SsoException.PROVIDER_ERROR, "Discovery document without " + field);
    }
    return value.asText();
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String basic(String clientId, String secret) {
    String pair = encode(clientId) + ":" + encode(secret == null ? "" : secret);
    return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * The S256 challenge of a PKCE verifier.
   *
   * @param verifier verifier
   * @return challenge, URL-safe Base64 without padding
   */
  static String challenge(String verifier) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private record Endpoints(String authorization, String token, String jwkSet) {}
}
