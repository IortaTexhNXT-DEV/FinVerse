package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.migration.intake.service.port.MaskingProvider;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Default {@link MaskingProvider}: HMAC-SHA256 with the key of the environment ({@code
 * brokerverse.migration.masking-key}, from the secrets store; never in the code or the database).
 */
public class HmacMaskingProvider implements MaskingProvider {

  private static final String ALGORITHM = "HmacSHA256";

  private final byte[] key;

  /**
   * Creates the provider.
   *
   * @param key masking key, blank when not configured
   */
  public HmacMaskingProvider(String key) {
    this.key = key == null ? new byte[0] : key.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public boolean configured() {
    return key.length > 0;
  }

  @Override
  public byte[] digest(String purpose, String value) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(key, ALGORITHM));
      return mac.doFinal((purpose + "|" + value).getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("The masking key cannot be used", e);
    }
  }
}
