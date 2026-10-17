package com.iortatechnxt.brokerverse.security.service.mfa;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts the authenticator secrets at rest with AES-256-GCM. The key comes from the environment
 * ({@code BROKERVERSE_MFA_ENCRYPTION_KEY}, 32 random bytes in Base64; required outside local by the
 * start-up safeguards); never from the database. A stored value is {@code v1:} and the Base64 of a
 * 12-byte random IV followed by the ciphertext and tag; the user name is bound as additional data,
 * so a secret copied to another user's row does not decrypt. For a key rotation the previous key is
 * set in {@code BROKERVERSE_MFA_PREVIOUS_ENCRYPTION_KEY}: values are read with either key and
 * written with the current one.
 */
@Component
public class MfaSecretCipher {

  private static final Logger LOG = LoggerFactory.getLogger(MfaSecretCipher.class);
  private static final String PREFIX = "v1:";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final int KEY_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final SecretKey current;
  private final List<SecretKey> readKeys;

  /**
   * Creates the cipher.
   *
   * @param key current key, Base64 of 32 bytes; blank when the second factor is not configured
   * @param previousKey previous key during a rotation, may be blank
   */
  public MfaSecretCipher(
      @Value("${brokerverse.security.mfa.encryption-key:}") String key,
      @Value("${brokerverse.security.mfa.previous-encryption-key:}") String previousKey) {
    this.current = key(key);
    List<SecretKey> keys = new ArrayList<>();
    if (current != null) {
      keys.add(current);
    }
    SecretKey previous = key(previousKey);
    if (previous != null) {
      keys.add(previous);
    }
    this.readKeys = List.copyOf(keys);
  }

  /**
   * Whether a key is configured.
   *
   * @return true when secrets can be stored
   */
  public boolean configured() {
    return current != null;
  }

  /**
   * Encrypts a secret of a user.
   *
   * @param secret secret bytes
   * @param username owner (bound as additional data)
   * @return stored value
   */
  public String encrypt(byte[] secret, String username) {
    if (current == null) {
      throw new IllegalStateException("BROKERVERSE_MFA_ENCRYPTION_KEY is not set");
    }
    try {
      byte[] iv = new byte[IV_BYTES];
      RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, current, new GCMParameterSpec(TAG_BITS, iv));
      cipher.updateAAD(aad(username));
      byte[] sealed = cipher.doFinal(secret);
      return PREFIX
          + Base64.getEncoder()
              .encodeToString(
                  ByteBuffer.allocate(iv.length + sealed.length).put(iv).put(sealed).array());
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("The authenticator secret could not be encrypted", ex);
    }
  }

  /**
   * Decrypts a stored secret of a user.
   *
   * @param stored stored value
   * @param username owner
   * @return secret bytes
   */
  public byte[] decrypt(String stored, String username) {
    if (stored == null || !stored.startsWith(PREFIX)) {
      throw new IllegalStateException("Unknown format of an authenticator secret");
    }
    byte[] data = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
    for (SecretKey key : readKeys) {
      try {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
        cipher.updateAAD(aad(username));
        return cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES);
      } catch (GeneralSecurityException ex) {
        LOG.debug("Authenticator secret not readable with this key; trying the next one");
      }
    }
    throw new IllegalStateException("The authenticator secret could not be decrypted");
  }

  /**
   * Whether a stored value was written with the current key (a rotation re-encrypts the others).
   *
   * @param stored stored value
   * @param username owner
   * @return true when the current key reads it
   */
  public boolean readableWithCurrentKey(String stored, String username) {
    if (current == null || stored == null || !stored.startsWith(PREFIX)) {
      return false;
    }
    byte[] data = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, current, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
      cipher.updateAAD(aad(username));
      cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES);
      return true;
    } catch (GeneralSecurityException ex) {
      return false;
    }
  }

  private static byte[] aad(String username) {
    return username.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
  }

  private static SecretKey key(String base64) {
    if (base64 == null || base64.isBlank()) {
      return null;
    }
    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(base64.trim());
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("The key of the authenticator secrets is not Base64", ex);
    }
    if (bytes.length != KEY_BYTES) {
      throw new IllegalStateException(
          "The key of the authenticator secrets must be " + KEY_BYTES + " bytes");
    }
    return new SecretKeySpec(bytes, "AES");
  }
}
