package com.iortatechnxt.brokerverse.security.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Random single-use tokens (reset links, refresh tokens, sign-in tickets, trusted devices) and the
 * SHA-256 kept in their place: only the hash of a token is ever stored.
 */
public final class SecureTokens {

  private static final int TOKEN_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private SecureTokens() {}

  /**
   * A new random token of 256 bits, URL-safe Base64 without padding.
   *
   * @return token
   */
  public static String newToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * SHA-256 of a token, hex encoded.
   *
   * @param token token
   * @return hash
   */
  public static String sha256(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
