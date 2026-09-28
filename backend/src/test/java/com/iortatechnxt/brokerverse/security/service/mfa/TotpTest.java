package com.iortatechnxt.brokerverse.security.service.mfa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** TOTP (RFC 6238 test vectors, SHA-1), the replay rule and the encryption of the secrets. */
class TotpTest {

  private static final byte[] RFC_SECRET =
      "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

  @Test
  void theRfc6238VectorsMatch() {
    // RFC 6238 appendix B, SHA-1, last six digits of the eight-digit values.
    assertThat(Totp.code(RFC_SECRET, Totp.step(Instant.ofEpochSecond(59)))).isEqualTo("287082");
    assertThat(Totp.code(RFC_SECRET, Totp.step(Instant.ofEpochSecond(1_111_111_109))))
        .isEqualTo("081804");
    assertThat(Totp.code(RFC_SECRET, Totp.step(Instant.ofEpochSecond(1_234_567_890))))
        .isEqualTo("005924");
    assertThat(Totp.code(RFC_SECRET, Totp.step(Instant.ofEpochSecond(2_000_000_000))))
        .isEqualTo("279037");
  }

  @Test
  void aCodeIsAcceptedOnceWithinTheDrift() {
    Instant now = Instant.ofEpochSecond(1_234_567_890);
    long step = Totp.step(now);
    String previous = Totp.code(RFC_SECRET, step - 1);
    assertThat(Totp.verify(RFC_SECRET, previous, now, Long.MIN_VALUE)).hasValue(step - 1);
    assertThat(Totp.verify(RFC_SECRET, previous, now, step - 1)).isEmpty();
    assertThat(Totp.verify(RFC_SECRET, Totp.code(RFC_SECRET, step - 2), now, Long.MIN_VALUE))
        .isEmpty();
    assertThat(Totp.verify(RFC_SECRET, "12345", now, Long.MIN_VALUE)).isEmpty();
    assertThat(Totp.verify(RFC_SECRET, "abcdef", now, Long.MIN_VALUE)).isEmpty();
  }

  @Test
  void base32IsTheRfc4648Alphabet() {
    assertThat(Totp.base32("foobar".getBytes(StandardCharsets.US_ASCII))).isEqualTo("MZXW6YTBOI");
    assertThat(Totp.base32(Totp.newSecret())).hasSize(32);
  }

  @Test
  void secretsAreEncryptedAndBoundToTheUser() {
    String key = Base64.getEncoder().encodeToString(new byte[32]);
    String other =
        Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII));
    MfaSecretCipher cipher = new MfaSecretCipher(key, "");
    String stored = cipher.encrypt(RFC_SECRET, "Jdoe");
    assertThat(stored).startsWith("v1:").doesNotContain("MTIz");
    assertThat(cipher.decrypt(stored, "jdoe")).isEqualTo(RFC_SECRET);
    assertThatThrownBy(() -> cipher.decrypt(stored, "someone"))
        .isInstanceOf(IllegalStateException.class);

    MfaSecretCipher rotated = new MfaSecretCipher(other, key);
    assertThat(rotated.decrypt(stored, "jdoe")).isEqualTo(RFC_SECRET);
    assertThat(rotated.readableWithCurrentKey(stored, "jdoe")).isFalse();
    assertThat(new MfaSecretCipher("", "").configured()).isFalse();
    assertThatThrownBy(() -> new MfaSecretCipher("c2hvcnQ=", ""))
        .isInstanceOf(IllegalStateException.class);
  }
}
