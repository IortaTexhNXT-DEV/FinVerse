package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.security.service.SecureTokens;
import com.iortatechnxt.brokerverse.security.service.sso.SsoProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * The identity provider of the Enterprise SSO simulator (SIT and UAT): an OpenID Connect provider
 * standing for EIAM, with the accounts of the simulator. It signs a user in when the account is
 * active and the device is a BDO-issued one (EIAM's device policy, BDOI FRS FRUM.001.01), keeps its
 * own sign-in session until the sign-out page is called (FRUM.001.05), and issues ID tokens signed
 * RS256 whose subject is the Windows ID. Codes and sessions are held in memory.
 */
@Service
@ConditionalOnProperty(name = "brokerverse.identity.simulator", havingValue = "true")
public class EiamSimulator {

  /** Path of the simulator. */
  public static final String PATH = "/eiam-simulator";

  private static final int KEY_SIZE = 2048;
  private static final Duration CODE_VALIDITY = Duration.ofMinutes(2);
  private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(5);

  private final SsoProperties sso;
  private final EnterpriseDirectory directory;
  private final Clock clock;
  private final RSAKey key;
  private final Map<String, Grant> codes = new ConcurrentHashMap<>();
  private final Map<String, String> sessions = new ConcurrentHashMap<>();

  /**
   * Creates the simulator with a new signing key.
   *
   * @param sso single sign-on settings (the registered client and the public address)
   * @param directory the accounts of the simulator
   * @param clock clock
   * @throws JOSEException when the key cannot be made
   */
  public EiamSimulator(SsoProperties sso, EnterpriseDirectory directory, Clock clock)
      throws JOSEException {
    this.sso = sso;
    this.directory = directory;
    this.clock = clock;
    this.key = new RSAKeyGenerator(KEY_SIZE).keyID("eiam-simulator").generate();
  }

  /**
   * The issuer of the simulator.
   *
   * @return issuer address
   */
  public String issuer() {
    return sso.base() + PATH;
  }

  /**
   * The public key set.
   *
   * @return JSON of the key set
   */
  public Map<String, Object> keySet() {
    return new JWKSet(key.toPublicJWK()).toJSONObject();
  }

  /**
   * The Windows ID of an open sign-in session of the simulator.
   *
   * @param session session cookie value, may be null
   * @return Windows ID, empty without an open session
   */
  public Optional<String> sessionUser(String session) {
    return session == null ? Optional.empty() : Optional.ofNullable(sessions.get(session));
  }

  /**
   * Signs a user in at the simulated EIAM.
   *
   * @param windowsId account chosen
   * @param bdoDevice whether the device is a BDO-issued one
   * @return the account signed in
   * @throws IdentityRefused DEVICE_NOT_ALLOWED, ACCOUNT_REFUSED (unknown account) or
   *     ACCOUNT_<status>
   */
  public DirectoryAccount signIn(String windowsId, boolean bdoDevice) {
    if (!bdoDevice) {
      throw new IdentityRefused(
          "DEVICE_NOT_ALLOWED",
          "Access blocked: your organisation's policy allows sign-in only from bank-issued devices");
    }
    DirectoryAccount account =
        directory
            .find(windowsId)
            .orElseThrow(() -> new IdentityRefused("ACCOUNT_REFUSED", "Unknown account"));
    if (!account.status().grantsAccess()) {
      throw new IdentityRefused(
          "ACCOUNT_" + account.status().name(),
          "Your account is "
              + account.status().name().toLowerCase(Locale.ROOT)
              + "; contact the service desk");
    }
    return account;
  }

  /**
   * Opens the simulator's own sign-in session of a user.
   *
   * @param windowsId Windows ID
   * @return the session cookie value
   */
  public String openSession(String windowsId) {
    String session = SecureTokens.newToken();
    sessions.put(session, windowsId);
    return session;
  }

  /**
   * Ends the simulator's sign-in session (sign-out page).
   *
   * @param session session cookie value, may be null
   */
  public void endSession(String session) {
    if (session != null) {
      sessions.remove(session);
    }
  }

  /**
   * Issues the authorisation code of a sign-in.
   *
   * @param account account signed in
   * @param request the authorisation request
   * @return code
   */
  public String code(DirectoryAccount account, AuthorizationRequest request) {
    if (!sso.oidc().clientId().equals(request.clientId())) {
      throw new IdentityRefused("UNKNOWN_CLIENT", "Unknown client");
    }
    String code = SecureTokens.newToken();
    codes.put(
        code,
        new Grant(
            account,
            request.nonce(),
            request.codeChallenge(),
            request.redirectUri(),
            clock.instant().plus(CODE_VALIDITY)));
    return code;
  }

  /**
   * Exchanges a code for the ID token (client secret and PKCE verifier checked).
   *
   * @param code code
   * @param verifier PKCE verifier
   * @param clientId client id of the caller
   * @param clientSecret client secret of the caller
   * @return the signed ID token
   * @throws IdentityRefused INVALID_GRANT, INVALID_CLIENT
   */
  public String token(String code, String verifier, String clientId, String clientSecret) {
    checkClient(clientId, clientSecret);
    Grant grant = takeGrant(code, verifier);
    Instant now = clock.instant();
    DirectoryAccount a = grant.account();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer())
            .audience(clientId)
            .subject(a.windowsId())
            .claim("preferred_username", a.windowsId())
            .claim("upn", a.email())
            .claim("name", a.fullName())
            .claim("email", a.email())
            .claim("nonce", grant.nonce())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(TOKEN_VALIDITY)))
            .build();
    try {
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256)
                  .keyID(key.getKeyID())
                  .type(JOSEObjectType.JWT)
                  .build(),
              claims);
      jwt.sign(new RSASSASigner(key));
      return jwt.serialize();
    } catch (JOSEException ex) {
      throw new IllegalStateException("ID token not signed", ex);
    }
  }

  private void checkClient(String clientId, String clientSecret) {
    boolean known = sso.oidc().clientId().equals(clientId);
    if (!known || !MessageDigest.isEqual(bytes(sso.oidc().clientSecret()), bytes(clientSecret))) {
      throw new IdentityRefused("invalid_client", "Client authentication failed");
    }
  }

  private Grant takeGrant(String code, String verifier) {
    Grant grant = code == null ? null : codes.remove(code);
    if (grant == null
        || clock.instant().isAfter(grant.expiresAt())
        || !grant.challenge().equals(challenge(verifier))) {
      throw new IdentityRefused("invalid_grant", "The code is not valid");
    }
    return grant;
  }

  private static byte[] bytes(String text) {
    return text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
  }

  private static String challenge(String verifier) {
    if (verifier == null) {
      return "";
    }
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * An authorisation request of the relying party.
   *
   * @param clientId client id
   * @param redirectUri redirect URI
   * @param state state
   * @param nonce nonce
   * @param codeChallenge PKCE S256 challenge
   */
  public record AuthorizationRequest(
      String clientId, String redirectUri, String state, String nonce, String codeChallenge) {}

  /** What a code stands for. */
  private record Grant(
      DirectoryAccount account,
      String nonce,
      String challenge,
      String redirectUri,
      Instant expiresAt) {}
}
