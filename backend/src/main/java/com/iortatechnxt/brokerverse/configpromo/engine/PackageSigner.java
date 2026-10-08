package com.iortatechnxt.brokerverse.configpromo.engine;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signs and verifies packages with HMAC-SHA256 under the signing key of the platform. The manifest
 * holds the SHA-256 of every data file, so its signature covers the whole package.
 */
public final class PackageSigner {

  /** Signature algorithm. */
  public static final String ALGORITHM = "HmacSHA256";

  private static final int MIN_KEY_LENGTH = 32;
  private static final int KEY_ID_LENGTH = 12;

  private final byte[] key;

  /**
   * Creates a signer.
   *
   * @param key signing key (at least 32 characters)
   */
  public PackageSigner(String key) {
    if (key == null || key.length() < MIN_KEY_LENGTH) {
      throw new PackageException(
          "The signing key of configuration packages is not configured or shorter than "
              + MIN_KEY_LENGTH
              + " characters");
    }
    this.key = key.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Short identifier of the key (not the key): tells which key signed a package.
   *
   * @return identifier
   */
  public String keyId() {
    return Sha256.hex(key).substring(0, KEY_ID_LENGTH);
  }

  /**
   * Signature of some bytes.
   *
   * @param content bytes (the manifest)
   * @return hex signature
   */
  public String sign(byte[] content) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(key, ALGORITHM));
      return HexFormat.of().formatHex(mac.doFinal(content));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC-SHA256 is not available", e);
    }
  }

  /**
   * Whether a signature is the signature of some bytes (constant-time comparison).
   *
   * @param content bytes
   * @param signature hex signature
   * @return true when valid
   */
  public boolean verify(byte[] content, String signature) {
    if (signature == null) {
      return false;
    }
    return MessageDigest.isEqual(
        sign(content).getBytes(StandardCharsets.US_ASCII),
        signature.getBytes(StandardCharsets.US_ASCII));
  }
}
