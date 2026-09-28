package com.iortatechnxt.brokerverse.security.service.mfa;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Time-based one-time passwords (RFC 6238 over RFC 4226): HMAC-SHA1, 6 digits, 30-second steps, the
 * settings every authenticator app supports. A code is accepted for the current step and one step
 * either side (clock drift of the phone); the step that matched is returned so a code is never
 * accepted twice. Secrets are 160 random bits, shown to the user in Base32 (RFC 4648).
 */
public final class Totp {

  /** Seconds per step. */
  public static final int STEP_SECONDS = 30;

  /** Digits of a code. */
  public static final int DIGITS = 6;

  /** Steps accepted either side of the current one. */
  public static final int DRIFT_STEPS = 1;

  private static final int SECRET_BYTES = 20;
  private static final int MODULUS = 1_000_000;
  private static final String ALGORITHM = "HmacSHA1";
  private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  private static final int BITS_PER_CHAR = 5;
  private static final int BYTE_MASK = 0xff;
  private static final int OFFSET_MASK = 0x0f;
  private static final int CHAR_MASK = 0x1f;
  private static final SecureRandom RANDOM = new SecureRandom();

  private Totp() {}

  /**
   * A new random secret.
   *
   * @return secret bytes
   */
  public static byte[] newSecret() {
    byte[] secret = new byte[SECRET_BYTES];
    RANDOM.nextBytes(secret);
    return secret;
  }

  /**
   * The step of a time.
   *
   * @param time time
   * @return step number
   */
  public static long step(Instant time) {
    return Math.floorDiv(time.getEpochSecond(), STEP_SECONDS);
  }

  /**
   * The code of a step (RFC 4226 dynamic truncation).
   *
   * @param secret secret
   * @param step step number
   * @return six-digit code with leading zeros
   */
  public static String code(byte[] secret, long step) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret, ALGORITHM));
      byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(step).array());
      int offset = hash[hash.length - 1] & OFFSET_MASK;
      int binary = ByteBuffer.wrap(hash, offset, Integer.BYTES).getInt() & Integer.MAX_VALUE;
      return String.format(Locale.ROOT, "%0" + DIGITS + "d", binary % MODULUS);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("HMAC-SHA1 is not available", ex);
    }
  }

  /**
   * Checks a code at a time, allowing {@value #DRIFT_STEPS} step either side, and never a step at
   * or before the last one used.
   *
   * @param secret secret
   * @param code code entered (spaces ignored)
   * @param now time of the check
   * @param lastUsedStep the step of the last accepted code, or {@link Long#MIN_VALUE}
   * @return the step that matched, empty when the code is wrong or was already used
   */
  public static OptionalLong verify(byte[] secret, String code, Instant now, long lastUsedStep) {
    String entered = code == null ? "" : code.replace(" ", "");
    if (entered.length() != DIGITS || !entered.chars().allMatch(Character::isDigit)) {
      return OptionalLong.empty();
    }
    long current = step(now);
    for (long step = current - DRIFT_STEPS; step <= current + DRIFT_STEPS; step++) {
      boolean same =
          MessageDigest.isEqual(
              code(secret, step).getBytes(StandardCharsets.US_ASCII),
              entered.getBytes(StandardCharsets.US_ASCII));
      if (same && step > lastUsedStep) {
        return OptionalLong.of(step);
      }
    }
    return OptionalLong.empty();
  }

  /**
   * Base32 of bytes, without padding (the form authenticator apps read).
   *
   * @param bytes bytes
   * @return Base32 text
   */
  public static String base32(byte[] bytes) {
    StringBuilder out = new StringBuilder();
    int buffer = 0;
    int bits = 0;
    for (byte b : bytes) {
      buffer = (buffer << Byte.SIZE) | (b & BYTE_MASK);
      bits += Byte.SIZE;
      while (bits >= BITS_PER_CHAR) {
        out.append(BASE32.charAt((buffer >> (bits - BITS_PER_CHAR)) & CHAR_MASK));
        bits -= BITS_PER_CHAR;
      }
    }
    if (bits > 0) {
      out.append(BASE32.charAt((buffer << (BITS_PER_CHAR - bits)) & CHAR_MASK));
    }
    return out.toString();
  }
}
