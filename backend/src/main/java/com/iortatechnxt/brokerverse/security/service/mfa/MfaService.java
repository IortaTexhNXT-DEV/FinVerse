package com.iortatechnxt.brokerverse.security.service.mfa;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.domain.MfaRecoveryCode;
import com.iortatechnxt.brokerverse.security.domain.MfaRecoveryCodeRepository;
import com.iortatechnxt.brokerverse.security.domain.MfaTrustedDevice;
import com.iortatechnxt.brokerverse.security.domain.MfaTrustedDeviceRepository;
import com.iortatechnxt.brokerverse.security.domain.UserMfa;
import com.iortatechnxt.brokerverse.security.domain.UserMfaRepository;
import com.iortatechnxt.brokerverse.security.service.SecureTokens;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The second factor of a user: enrolment of an authenticator app (secret shown as a QR code and in
 * Base32, confirmed with a first code), the check of a code at sign-in (TOTP, never the same code
 * twice) or of a single-use recovery code, the ten recovery codes given at enrolment, and the
 * devices remembered when the parameter {@code MFA_REMEMBER_DEVICE_DAYS} allows it. The secret is
 * encrypted at rest ({@link MfaSecretCipher}); codes and device tokens are kept as SHA-256 only.
 */
@Service
@Transactional
public class MfaService {

  /** Recovery codes given at each enrolment. */
  public static final int RECOVERY_CODES = 10;

  private static final String ENTITY = "AppUser";
  private static final String CODE_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
  private static final int CODE_HALF = 5;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserMfaRepository enrolments;
  private final MfaRecoveryCodeRepository recoveryCodes;
  private final MfaTrustedDeviceRepository devices;
  private final MfaSecretCipher cipher;
  private final MfaPolicy policy;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param enrolments authenticator apps
   * @param recoveryCodes recovery codes
   * @param devices remembered devices
   * @param cipher encryption of the secrets
   * @param policy parameters of the second factor
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the second factor
  public MfaService(
      UserMfaRepository enrolments,
      MfaRecoveryCodeRepository recoveryCodes,
      MfaTrustedDeviceRepository devices,
      MfaSecretCipher cipher,
      MfaPolicy policy,
      AuditTrailService audit,
      Clock clock) {
    this.enrolments = enrolments;
    this.recoveryCodes = recoveryCodes;
    this.devices = devices;
    this.cipher = cipher;
    this.policy = policy;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Whether a user has an active authenticator app.
   *
   * @param username user
   * @return true when enrolled
   */
  @Transactional(readOnly = true)
  public boolean enrolled(String username) {
    return active(username).isPresent();
  }

  /**
   * The second factor of a user for My Profile and the administrators.
   *
   * @param username user
   * @return status
   */
  @Transactional(readOnly = true)
  public Status status(String username) {
    Optional<UserMfa> active = active(username);
    return new Status(
        active.isPresent(),
        active.map(UserMfa::getActivatedAt).orElse(null),
        active.map(UserMfa::getLastUsedAt).orElse(null),
        active.isPresent() ? recoveryCodes.countByUsernameIgnoreCaseAndUsedAtIsNull(username) : 0);
  }

  /**
   * The active authenticator apps by lower-case user name (the administrators' list).
   *
   * @return enrolments
   */
  @Transactional(readOnly = true)
  public Map<String, UserMfa> activeEnrolments() {
    return enrolments.findByStatus(UserMfa.ACTIVE).stream()
        .collect(
            Collectors.toMap(m -> m.getUsername().toLowerCase(Locale.ROOT), m -> m, (a, b) -> a));
  }

  /**
   * Starts (or restarts) the enrolment of an authenticator app. An active app stays in use until
   * the new one is confirmed.
   *
   * @param username user
   * @return the secret to scan or type
   */
  public Enrolment startEnrolment(String username) {
    if (!policy.available()) {
      throw new BusinessRuleException(
          "MFA_NOT_AVAILABLE", "The second factor is not set up on this system");
    }
    enrolments
        .findByUsernameIgnoreCaseAndStatus(username, UserMfa.PENDING)
        .ifPresent(
            p -> {
              enrolments.delete(p);
              enrolments.flush();
            });
    byte[] secret = Totp.newSecret();
    enrolments.save(new UserMfa(username, cipher.encrypt(secret, username), clock.instant()));
    String base32 = Totp.base32(secret);
    String issuer = policy.issuer();
    String uri =
        "otpauth://totp/"
            + encode(issuer)
            + ":"
            + encode(username)
            + "?secret="
            + base32
            + "&issuer="
            + encode(issuer)
            + "&algorithm=SHA1&digits="
            + Totp.DIGITS
            + "&period="
            + Totp.STEP_SECONDS;
    return new Enrolment(groups(base32), uri, QrCodes.svgDataUri(uri), issuer);
  }

  /**
   * Confirms the enrolment with the first code of the app: the app becomes the user's second factor
   * (replacing an earlier one) and ten new recovery codes are given, once.
   *
   * @param username user
   * @param code code shown by the app
   * @return the recovery codes, shown once
   * @throws BusinessRuleException MFA_CODE_INVALID when the code does not match
   */
  public List<String> confirmEnrolment(String username, String code) {
    UserMfa pending =
        enrolments
            .findByUsernameIgnoreCaseAndStatus(username, UserMfa.PENDING)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MFA_ENROLMENT_NOT_STARTED", "Start the enrolment of the app first"));
    Instant now = clock.instant();
    OptionalLong step =
        Totp.verify(cipher.decrypt(pending.getSecretCipher(), username), code, now, Long.MIN_VALUE);
    if (step.isEmpty()) {
      throw new BusinessRuleException(
          "MFA_CODE_INVALID", "The code does not match. Check the time of your phone and retry");
    }
    active(username)
        .ifPresent(
            a -> {
              enrolments.delete(a);
              enrolments.flush();
            });
    pending.activate(step.getAsLong(), now);
    devices.deleteAllOf(username);
    List<String> codes = newRecoveryCodes(username, now);
    audit.recordIndependently(
        username, ENTITY, username, AuditAction.UPDATE, "Authenticator app enrolled");
    return codes;
  }

  /**
   * Checks a code at sign-in: a code of the authenticator app (not used before) or an unused
   * recovery code, which is then used up.
   *
   * @param username user
   * @param code code entered
   * @return true when accepted
   */
  public boolean verify(String username, String code) {
    Optional<UserMfa> active = active(username);
    if (active.isEmpty() || code == null || code.isBlank()) {
      return false;
    }
    UserMfa mfa = active.get();
    Instant now = clock.instant();
    OptionalLong step =
        Totp.verify(
            cipher.decrypt(mfa.getSecretCipher(), username), code, now, mfa.getLastUsedStep());
    if (step.isPresent()) {
      mfa.use(step.getAsLong(), now);
      reencrypt(mfa);
      return true;
    }
    Optional<MfaRecoveryCode> recovery =
        recoveryCodes.findFirstByUsernameIgnoreCaseAndCodeHashAndUsedAtIsNull(
            username, SecureTokens.sha256(normalise(code)));
    recovery.ifPresent(
        r -> {
          r.use(now);
          audit.recordIndependently(
              username, ENTITY, username, AuditAction.UPDATE, "Recovery code used to sign in");
        });
    return recovery.isPresent();
  }

  /**
   * Replaces the recovery codes after a code of the app is confirmed.
   *
   * @param username user
   * @param code current code of the app
   * @return the new codes, shown once
   */
  public List<String> regenerateRecoveryCodes(String username, String code) {
    UserMfa mfa =
        active(username)
            .orElseThrow(
                () -> new BusinessRuleException("MFA_NOT_ENROLLED", "No authenticator app"));
    Instant now = clock.instant();
    OptionalLong step =
        Totp.verify(
            cipher.decrypt(mfa.getSecretCipher(), username), code, now, mfa.getLastUsedStep());
    if (step.isEmpty()) {
      throw new BusinessRuleException("MFA_CODE_INVALID", "The code does not match");
    }
    mfa.use(step.getAsLong(), now);
    List<String> codes = newRecoveryCodes(username, now);
    audit.recordIndependently(
        username, ENTITY, username, AuditAction.UPDATE, "Recovery codes replaced");
    return codes;
  }

  /**
   * Remembers the device of a sign-in, when the parameter allows it.
   *
   * @param username user
   * @return the device token to keep in the browser, empty when devices are not remembered
   */
  public Optional<String> rememberDevice(String username) {
    int days = policy.rememberDays();
    if (days <= 0) {
      return Optional.empty();
    }
    String token = SecureTokens.newToken();
    Instant now = clock.instant();
    devices.save(
        new MfaTrustedDevice(
            username, SecureTokens.sha256(token), now, now.plus(Duration.ofDays(days))));
    return Optional.of(token);
  }

  /**
   * Whether a device token is a remembered device of the user, still valid and still allowed.
   *
   * @param username user
   * @param deviceToken token from the browser, may be null
   * @return true when the code need not be asked
   */
  @Transactional(readOnly = true)
  public boolean trustedDevice(String username, String deviceToken) {
    if (deviceToken == null || deviceToken.isBlank() || policy.rememberDays() <= 0) {
      return false;
    }
    Instant now = clock.instant();
    return devices
        .findByTokenHash(SecureTokens.sha256(deviceToken.trim()))
        .filter(d -> d.getUsername().equalsIgnoreCase(username))
        .filter(d -> now.isBefore(d.getExpiresAt()))
        .isPresent();
  }

  /**
   * Removes the second factor of a user: the app, the recovery codes and the remembered devices (an
   * approved reset).
   *
   * @param username user
   */
  public void removeAll(String username) {
    enrolments.deleteAllOf(username);
    recoveryCodes.deleteAllOf(username);
    devices.deleteAllOf(username);
  }

  private Optional<UserMfa> active(String username) {
    return enrolments.findByUsernameIgnoreCaseAndStatus(username, UserMfa.ACTIVE);
  }

  /** Writes the secret again with the current key when it was read with the previous one. */
  private void reencrypt(UserMfa mfa) {
    if (!cipher.readableWithCurrentKey(mfa.getSecretCipher(), mfa.getUsername())) {
      mfa.reencrypt(
          cipher.encrypt(
              cipher.decrypt(mfa.getSecretCipher(), mfa.getUsername()), mfa.getUsername()));
    }
  }

  private List<String> newRecoveryCodes(String username, Instant now) {
    recoveryCodes.deleteAllOf(username);
    List<String> codes = new ArrayList<>();
    for (int i = 0; i < RECOVERY_CODES; i++) {
      String code = randomCode(CODE_HALF) + "-" + randomCode(CODE_HALF);
      codes.add(code);
      recoveryCodes.save(new MfaRecoveryCode(username, SecureTokens.sha256(normalise(code)), now));
    }
    return codes;
  }

  private static String randomCode(int length) {
    StringBuilder out = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      out.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
    }
    return out.toString();
  }

  private static String normalise(String code) {
    return code.replace("-", "").replace(" ", "").trim().toLowerCase(Locale.ROOT);
  }

  private static String groups(String base32) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < base32.length(); i += 4) {
      if (i > 0) {
        out.append(' ');
      }
      out.append(base32, i, Math.min(base32.length(), i + 4));
    }
    return out.toString();
  }

  private static String encode(String text) {
    return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /**
   * An enrolment in progress.
   *
   * @param secret the secret in Base32, in groups of four, for typing into the app
   * @param otpauthUri the {@code otpauth://} URI of the QR code
   * @param qrCode the QR code as an SVG {@code data:} URI
   * @param issuer name of the system in the app
   */
  public record Enrolment(String secret, String otpauthUri, String qrCode, String issuer) {}

  /**
   * The second factor of a user.
   *
   * @param enrolled whether an authenticator app is active
   * @param enrolledAt when it was enrolled
   * @param lastUsedAt when a code was last accepted
   * @param recoveryCodesLeft unused recovery codes
   */
  public record Status(
      boolean enrolled, Instant enrolledAt, Instant lastUsedAt, long recoveryCodesLeft) {}
}
