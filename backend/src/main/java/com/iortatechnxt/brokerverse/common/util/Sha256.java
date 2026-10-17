package com.iortatechnxt.brokerverse.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 checksums as lower-case hex: duplicate upload detection (CSHID.008, PRCID.010) and
 * payload hashes of interface records (BRQID.005).
 */
public final class Sha256 {

  private Sha256() {}

  /**
   * The checksum of some bytes.
   *
   * @param content bytes
   * @return 64 hex characters
   */
  public static String hex(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  /**
   * The checksum of a text (UTF-8).
   *
   * @param text text
   * @return 64 hex characters
   */
  public static String hex(String text) {
    return hex(text.getBytes(StandardCharsets.UTF_8));
  }
}
