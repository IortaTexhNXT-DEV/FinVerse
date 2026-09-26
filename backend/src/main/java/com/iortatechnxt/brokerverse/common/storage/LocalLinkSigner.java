package com.iortatechnxt.brokerverse.common.storage;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signs and verifies the tokens of the local store's presigned links: method, object, expiry, one
 * extra value (file name of a GET, SHA-256 of a PUT) and content type, each Base64url encoded,
 * followed by an HMAC-SHA256 over them.
 */
final class LocalLinkSigner {

  private static final String HMAC = "HmacSHA256";
  private static final int SECRET_BYTES = 32;
  private static final int PARTS = 7;
  private static final int METHOD = 0;
  private static final int BUCKET = 1;
  private static final int KEY = 2;
  private static final int EXPIRES = 3;
  private static final int EXTRA = 4;
  private static final int TYPE = 5;
  private static final int SIGNATURE = 6;
  private static final String INVALID = "Invalid link";
  private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder B64D = Base64.getUrlDecoder();
  private static final SecureRandom RANDOM = new SecureRandom();

  private final byte[] secret;
  private final Clock clock;

  /**
   * Creates the signer.
   *
   * @param configured configured secret; a random one when empty
   * @param clock clock (expiry)
   */
  LocalLinkSigner(String configured, Clock clock) {
    if (configured == null || configured.isBlank()) {
      this.secret = new byte[SECRET_BYTES];
      RANDOM.nextBytes(this.secret);
    } else {
      this.secret = configured.getBytes(StandardCharsets.UTF_8);
    }
    this.clock = clock;
  }

  /**
   * A token.
   *
   * @param method HTTP method
   * @param ref object
   * @param expires end of validity
   * @param extra file name (GET) or SHA-256 (PUT)
   * @param type content type
   * @return token
   */
  String sign(String method, ObjectRef ref, Instant expires, String extra, String type) {
    String payload =
        String.join(
            ".",
            b64(method),
            b64(ref.bucket().name()),
            b64(ref.key()),
            b64(Long.toString(expires.getEpochSecond())),
            b64(extra == null ? "" : extra),
            b64(type == null ? "" : type));
    return payload + "." + B64.encodeToString(hmac(payload));
  }

  /**
   * Verifies a token for a method.
   *
   * @param token token
   * @param method expected method
   * @return the signed facts
   * @throws FileStoreException when the token is invalid, for another method or expired
   */
  Signed verify(String token, String method) {
    String[] parts = authentic(token);
    if (!method.equals(text(parts[METHOD]))) {
      throw new FileStoreException("The link is not valid for " + method);
    }
    if (clock.instant().getEpochSecond() > Long.parseLong(text(parts[EXPIRES]))) {
      throw new FileStoreException("The link has expired");
    }
    ObjectRef ref = new ObjectRef(BucketClass.valueOf(text(parts[BUCKET])), text(parts[KEY]));
    return new Signed(ref, text(parts[EXTRA]), text(parts[TYPE]));
  }

  private String[] authentic(String token) {
    String[] parts = token == null ? new String[0] : token.split("\\.");
    if (parts.length != PARTS) {
      throw new FileStoreException(INVALID);
    }
    byte[] signature;
    try {
      signature = B64D.decode(parts[SIGNATURE]);
    } catch (IllegalArgumentException e) {
      throw new FileStoreException(INVALID, e);
    }
    if (!MessageDigest.isEqual(signature, hmac(token.substring(0, token.lastIndexOf('.'))))) {
      throw new FileStoreException(INVALID);
    }
    return parts;
  }

  private byte[] hmac(String payload) {
    try {
      Mac mac = Mac.getInstance(HMAC);
      mac.init(new SecretKeySpec(secret, HMAC));
      return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC-SHA256 is not available", e);
    }
  }

  private static String b64(String text) {
    return B64.encodeToString(text.getBytes(StandardCharsets.UTF_8));
  }

  private static String text(String part) {
    return new String(B64D.decode(part), StandardCharsets.UTF_8);
  }

  /**
   * Facts of a verified token.
   *
   * @param ref object
   * @param extra file name (GET) or SHA-256 (PUT)
   * @param contentType content type
   */
  record Signed(ObjectRef ref, String extra, String contentType) {}
}
