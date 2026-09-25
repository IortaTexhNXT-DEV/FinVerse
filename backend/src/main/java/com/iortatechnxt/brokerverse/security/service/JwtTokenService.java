package com.iortatechnxt.brokerverse.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and validates signed JWT access tokens (HS256). Every token carries a unique {@code jti}
 * (token id) so it can be revoked before it expires (logout, {@link TokenRevocationStore}).
 */
@Service
public class JwtTokenService {

  private static final String ISSUER = "inxt-brokerverse";

  private final SecretKey key;
  private final SecurityProperties properties;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param properties security settings
   * @param clock system clock
   */
  public JwtTokenService(SecurityProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
    this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Issues a token for a user.
   *
   * @param username subject
   * @return issued token, its id and its expiry
   */
  public IssuedToken issue(String username) {
    Instant now = clock.instant();
    Instant expiry = now.plus(properties.tokenValidity());
    String tokenId = UUID.randomUUID().toString();
    String token =
        Jwts.builder()
            .id(tokenId)
            .issuer(ISSUER)
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    return new IssuedToken(token, expiry, tokenId);
  }

  /**
   * Validates a token and extracts its subject.
   *
   * @param token compact JWT
   * @return username when the token is valid and unexpired
   */
  public Optional<String> validate(String token) {
    return parse(token).map(TokenClaims::username);
  }

  /**
   * Validates a token and extracts its claims.
   *
   * @param token compact JWT
   * @return claims when the token is valid and unexpired
   */
  public Optional<TokenClaims> parse(String token) {
    try {
      Claims claims =
          Jwts.parser()
              .verifyWith(key)
              .requireIssuer(ISSUER)
              .clock(() -> Date.from(clock.instant()))
              .build()
              .parseSignedClaims(token)
              .getPayload();
      if (claims.getSubject() == null) {
        return Optional.empty();
      }
      Instant expiresAt =
          claims.getExpiration() == null ? null : claims.getExpiration().toInstant();
      return Optional.of(new TokenClaims(claims.getSubject(), claims.getId(), expiresAt));
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
   * @param tokenId token id ({@code jti}); null for tokens issued before token ids existed
   * @param expiresAt expiry
   */
  public record TokenClaims(String username, String tokenId, Instant expiresAt) {}
}
