package com.iortatechnxt.brokerverse.security.service.sso;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Single sign-on of a deployment, bound from {@code brokerverse.security.sso.*}. Which protocol is
 * used is the parameter {@code AUTH_MODE} (OIDC or SAML); the identity provider is configured here,
 * with its secrets from the environment.
 *
 * @param baseUrl public address of the application (e.g. {@code https://bibs.example.com}): the
 *     redirect URI of OpenID Connect and the assertion consumer service of SAML are built on it,
 *     and the web client is reached at {@code <baseUrl>/sso/callback}
 * @param label name of the identity provider on the sign-in page
 * @param breakGlassUsers user names that keep a local password in OIDC or SAML mode (emergency
 *     administrators; the second factor is always required for them)
 * @param usernameClaim claim (OIDC) or attribute (SAML) holding the BrokerVerse user name; {@code
 *     sub} / the NameID when blank
 * @param groupsClaim claim (OIDC) or attribute (SAML) holding the user's groups, for {@code
 *     groupRoles}
 * @param groupRoles optional mapping of an identity provider group to a BrokerVerse role code; when
 *     it is set, a single sign-on is accepted only when the provider asserts a group mapped to a
 *     role the user holds (the provider can withdraw access at once). Roles are never granted from
 *     the groups: they are held through the access requests, with their approvals.
 * @param requireLocalMfa whether the BrokerVerse second factor is also asked after a single sign-on
 *     (off: the identity provider enforces its own)
 * @param clockSkew tolerance on the validity times of tokens and assertions
 * @param oidc OpenID Connect provider
 * @param saml SAML 2.0 identity provider
 */
@ConfigurationProperties(prefix = "brokerverse.security.sso")
public record SsoProperties(
    String baseUrl,
    String label,
    List<String> breakGlassUsers,
    String usernameClaim,
    String groupsClaim,
    Map<String, String> groupRoles,
    boolean requireLocalMfa,
    Duration clockSkew,
    Oidc oidc,
    Saml saml) {

  /** Path of the web client that completes a single sign-on. */
  public static final String WEB_CALLBACK = "/sso/callback";

  /** Applies the defaults. */
  public SsoProperties {
    label = label == null || label.isBlank() ? "your organisation" : label.trim();
    breakGlassUsers =
        breakGlassUsers == null
            ? List.of()
            : breakGlassUsers.stream()
                .filter(Objects::nonNull)
                .map(u -> u.trim().toLowerCase(Locale.ROOT))
                .filter(u -> !u.isEmpty())
                .toList();
    groupRoles = groupRoles == null ? Map.of() : Map.copyOf(groupRoles);
    clockSkew = clockSkew == null ? Duration.ofMinutes(2) : clockSkew;
    oidc = oidc == null ? new Oidc(null, null, null, null, null, null, null, null) : oidc;
    saml = saml == null ? new Saml(null, null, null, null, null) : saml;
  }

  /**
   * Whether a user keeps a local password in OIDC or SAML mode.
   *
   * @param username user name
   * @return true for a break-glass administrator
   */
  public boolean isBreakGlass(String username) {
    return username != null && breakGlassUsers.contains(username.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * The public address without a trailing slash.
   *
   * @return base address, empty when not set
   */
  public String base() {
    if (baseUrl == null || baseUrl.isBlank()) {
      return "";
    }
    String trimmed = baseUrl.trim();
    return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
  }

  /**
   * OpenID Connect provider. The endpoints are read from the discovery document of the issuer
   * ({@code /.well-known/openid-configuration}) unless they are set.
   *
   * @param issuer issuer identifier (the {@code iss} of the ID tokens)
   * @param clientId client id registered at the provider
   * @param clientSecret client secret (environment, {@code BROKERVERSE_OIDC_CLIENT_SECRET})
   * @param scopes scopes asked for (default {@code openid profile email})
   * @param authorizationUri authorisation endpoint, discovered when blank
   * @param tokenUri token endpoint, discovered when blank
   * @param jwkSetUri key set of the ID token signatures, discovered when blank
   * @param jwsAlgorithm signature algorithm of the ID tokens (default RS256)
   */
  public record Oidc(
      String issuer,
      String clientId,
      String clientSecret,
      String scopes,
      String authorizationUri,
      String tokenUri,
      String jwkSetUri,
      String jwsAlgorithm) {

    /**
     * Whether a provider is configured.
     *
     * @return true when the issuer and client id are set
     */
    public boolean configured() {
      return issuer != null && !issuer.isBlank() && clientId != null && !clientId.isBlank();
    }
  }

  /**
   * SAML 2.0 identity provider (HTTP-Redirect binding for the request, HTTP-POST for the response;
   * the assertion or the response must be signed with the provider's key).
   *
   * @param spEntityId entity id of BrokerVerse at the provider (default: the base address)
   * @param idpEntityId entity id of the provider (the Issuer of its responses)
   * @param idpSsoUrl single sign-on address of the provider (HTTP-Redirect binding)
   * @param idpCertificate the provider's signing certificate or public key (PEM, or {@code file:}
   *     path to it)
   * @param nameIdFormat NameID format asked for (default unspecified)
   */
  public record Saml(
      String spEntityId,
      String idpEntityId,
      String idpSsoUrl,
      String idpCertificate,
      String nameIdFormat) {

    /**
     * Whether a provider is configured.
     *
     * @return true when the provider's entity id, address and certificate are set
     */
    public boolean configured() {
      return idpEntityId != null
          && !idpEntityId.isBlank()
          && idpSsoUrl != null
          && !idpSsoUrl.isBlank()
          && idpCertificate != null
          && !idpCertificate.isBlank();
    }
  }
}
