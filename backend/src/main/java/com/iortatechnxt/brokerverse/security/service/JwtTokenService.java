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
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and validates signed JWT access tokens (HS256). */
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
   * @return issued token and its expiry
   */
  public IssuedToken issue(String username) {
    Instant now = clock.instant();
    Instant expiry = now.plus(properties.tokenValidity());
    String token =
        Jwts.builder()
            .issuer(ISSUER)
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    return new IssuedToken(token, expiry);
  }

  /**
   * Validates a token and extracts its subject.
   *
   * @param token compact JWT
   * @return username when the token is valid and unexpired
   */
  public Optional<String> validate(String token) {
    try {
      Claims claims =
          Jwts.parser()
              .verifyWith(key)
              .requireIssuer(ISSUER)
              .clock(() -> Date.from(clock.instant()))
              .build()
              .parseSignedClaims(token)
              .getPayload();
      return Optional.ofNullable(claims.getSubject());
    } catch (JwtException | IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  /**
   * A signed token with its expiry.
   *
   * @param token compact JWT
   * @param expiresAt expiry instant
   */
  public record IssuedToken(String token, Instant expiresAt) {}
}
