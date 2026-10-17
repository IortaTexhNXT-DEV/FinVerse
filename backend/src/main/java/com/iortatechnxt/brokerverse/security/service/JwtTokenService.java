package com.iortatechnxt.brokerverse.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and validates the signed JWTs of BrokerVerse (HS256).
 *
 * <ul>
 *   <li><b>Access tokens</b> ({@code use=access}): short lived (parameter {@code
 *       ACCESS_TOKEN_MINUTES}), each with a unique {@code jti} (revocable before it expires, {@link
 *       TokenRevocationStore}) and the id of its sign-in session ({@code sid}); the first token of
 *       a session carries the session id as its {@code jti}. A token without a session is refused.
 *   <li><b>Challenge tokens</b> ({@code use=mfa}): prove the password step of a sign-in while the
 *       second factor is asked for; never accepted as an access token.
 * </ul>
 */
@Service
public class JwtTokenService {

  private static final String ISSUER = "inxt-brokerverse";
  private static final String USE = "use";
  private static final String SESSION = "sid";
  private static final String METHOD = "amr";

  /** Use of an access token. */
  public static final String ACCESS = "access";

  /** Use of the challenge token of the second factor. */
  public static final String MFA_CHALLENGE = "mfa";

  private final SecretKey key;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param properties security settings (signing key)
   * @param clock system clock
   */
  public JwtTokenService(SecurityProperties properties, Clock clock) {
    this.clock = clock;
    this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Issues an access token of a session.
   *
   * @param username subject
   * @param sessionId sign-in session
   * @param tokenId token id; the session id for the first token of a session
   * @param validity life of the token
   * @return issued token, its id and its expiry
   */
  public IssuedToken issueAccess(
      String username, String sessionId, String tokenId, Duration validity) {
    Instant now = clock.instant();
    Instant expiry = now.plus(validity);
    String token =
        Jwts.builder()
            .id(tokenId)
            .issuer(ISSUER)
            .subject(username)
            .claim(USE, ACCESS)
            .claim(SESSION, sessionId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    return new IssuedToken(token, expiry, tokenId);
  }

  /**
   * Issues an access token of a session with a new token id.
   *
   * @param username subject
   * @param sessionId sign-in session
   * @param validity life of the token
   * @return issued token
   */
  public IssuedToken issueAccess(String username, String sessionId, Duration validity) {
    return issueAccess(username, sessionId, UUID.randomUUID().toString(), validity);
  }

  /**
   * Issues a challenge token (for example the password step of a sign-in with a second factor).
   *
   * @param username subject
   * @param use purpose of the token, e.g. {@value #MFA_CHALLENGE}
   * @param method how the first step was passed (PASSWORD, OIDC, SAML)
   * @param validity life of the token
   * @return issued token
   */
  public IssuedToken issueChallenge(String username, String use, String method, Duration validity) {
    Instant now = clock.instant();
    Instant expiry = now.plus(validity);
    String tokenId = UUID.randomUUID().toString();
    String token =
        Jwts.builder()
            .id(tokenId)
            .issuer(ISSUER)
            .subject(username)
            .claim(USE, use)
            .claim(METHOD, method)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    return new IssuedToken(token, expiry, tokenId);
  }

  /**
   * Validates an access token and extracts its subject.
   *
   * @param token compact JWT
   * @return username when the token is a valid, unexpired access token
   */
  public Optional<String> validate(String token) {
    return parse(token).map(TokenClaims::username);
  }

  /**
   * Validates an access token and extracts its claims. A token of another use, without a token id
   * or without a session is refused.
   *
   * @param token compact JWT
   * @return claims when the token is a valid, unexpired access token
   */
  public Optional<TokenClaims> parse(String token) {
    return claims(token)
        .filter(c -> ACCESS.equals(c.get(USE, String.class)))
        .filter(c -> c.getId() != null && c.get(SESSION, String.class) != null)
        .map(
            c ->
                new TokenClaims(
                    c.getSubject(),
                    c.getId(),
                    c.getExpiration() == null ? null : c.getExpiration().toInstant(),
                    c.get(SESSION, String.class)));
  }

  /**
   * Validates a challenge token of a purpose.
   *
   * @param token compact JWT
   * @param use expected purpose
   * @return the challenge when the token is valid, unexpired and of that purpose
   */
  public Optional<Challenge> parseChallenge(String token, String use) {
    return claims(token)
        .filter(c -> use.equals(c.get(USE, String.class)))
        .map(c -> new Challenge(c.getSubject(), c.getId(), c.get(METHOD, String.class)));
  }

  private Optional<Claims> claims(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    try {
      Claims claims =
          Jwts.parser()
              .verifyWith(key)
              .requireIssuer(ISSUER)
              .clock(() -> Date.from(clock.instant()))
              .build()
              .parseSignedClaims(token)
              .getPayload();
      return claims.getSubject() == null ? Optional.empty() : Optional.of(claims);
    } catch (JwtException | IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  /**
   * A signed token with its id and expiry.
   *
   * @param token compact JWT
   * @param expiresAt expiry instant
   * @param tokenId unique token id ({@code jti})
   */
  public record IssuedToken(String token, Instant expiresAt, String tokenId) {}

  /**
   * Claims of a valid token.
   *
   * @param username subject
   * @param tokenId token id ({@code jti})
   * @param expiresAt expiry
   * @param sessionId sign-in session ({@code sid}); null for a challenge token
   */
  public record TokenClaims(String username, String tokenId, Instant expiresAt, String sessionId) {}

  /**
   * A valid challenge token.
   *
   * @param username subject
   * @param tokenId token id
   * @param method how the first step was passed (PASSWORD, OIDC, SAML)
   */
  public record Challenge(String username, String tokenId, String method) {}
}
