package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.api.dto.PasswordStatusResponse;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.PasswordHistory;
import com.iortatechnxt.brokerverse.security.domain.PasswordHistoryRepository;
import com.iortatechnxt.brokerverse.security.domain.PasswordResetToken;
import com.iortatechnxt.brokerverse.security.domain.PasswordResetTokenRepository;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service passwords of LOCAL sign-in (UAM-NFR-31, 36, 37; FR-UA-005): the own password change
 * under the rules of {@link AuthPasswordPolicy}, the "Forgot password?" link (single use, valid 30
 * minutes, e-mailed to the registered address) and the passwords that expire soon (job {@code
 * PASSWORD_EXPIRY_NOTICE}). Every change keeps the password history and dates and clears the forced
 * change flag; nothing is available in DIRECTORY mode, where BDO owns the passwords.
 */
@Service
@Transactional
public class AuthPasswordService {

  /** Validity of a reset link (FR-UA-005 R3). */
  public static final Duration LINK_VALIDITY = Duration.ofMinutes(30);

  private static final String ENTITY = "AppUser";
  private static final int TOKEN_BYTES = 32;
  private static final String DEFAULT_ORIGIN = "http://localhost:5173";
  private static final String RESET_PATH = "/reset-password";

  private final AppUserRepository users;
  private final PasswordHistoryRepository history;
  private final PasswordResetTokenRepository links;
  private final AuthPasswordPolicy policy;
  private final PasswordEncoder encoder;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final ApplicationEventPublisher events;
  private final Clock clock;
  private final String resetUrl;
  private final SecureRandom random = new SecureRandom();

  /**
   * Creates the service.
   *
   * @param users users
   * @param history password history
   * @param links reset links
   * @param policy password rules
   * @param encoder password encoder
   * @param audit audit trail
   * @param currentUser current user
   * @param events event publisher (the reset link e-mail)
   * @param clock clock
   * @param properties security settings (the first allowed origin is the web client)
   * @param resetUrl address of the reset page ({@code brokerverse.security.password-reset-url});
   *     blank = the first allowed origin + {@value #RESET_PATH}
   */
  @SuppressWarnings("java:S107") // collaborators of the password functions
  public AuthPasswordService(
      AppUserRepository users,
      PasswordHistoryRepository history,
      PasswordResetTokenRepository links,
      AuthPasswordPolicy policy,
      PasswordEncoder encoder,
      AuditTrailService audit,
      CurrentUser currentUser,
      ApplicationEventPublisher events,
      Clock clock,
      SecurityProperties properties,
      @Value("${brokerverse.security.password-reset-url:}") String resetUrl) {
    this.users = users;
    this.history = history;
    this.links = links;
    this.policy = policy;
    this.encoder = encoder;
    this.audit = audit;
    this.currentUser = currentUser;
    this.events = events;
    this.clock = clock;
    this.resetUrl = resolveResetUrl(resetUrl, properties);
  }

  /**
   * Changes the signed-in user's password after checking the current one, the minimum age and the
   * history (FR-UA-005).
   *
   * @param currentPassword current password
   * @param newPassword new password (length and complexity checked on the request)
   */
  public void changeOwnPassword(String currentPassword, String newPassword) {
    policy.requireLocalMode();
    AppUser user = currentUserEntity();
    if (currentPassword == null || !encoder.matches(currentPassword, user.getPasswordHash())) {
      throw new BusinessRuleException("INVALID_PASSWORD", "Current password is incorrect");
    }
    Instant now = clock.instant();
    policy.checkMinimumAge(user, now);
    policy.checkReuse(user, newPassword);
    setPassword(user, newPassword, now);
    audit.record(ENTITY, user.getUsername(), AuditAction.UPDATE, "Changed own password");
  }

  /**
   * The rules in force and the signed-in user's password dates (My Profile; the web client asks for
   * a due change after sign-in).
   *
   * @return status
   */
  @Transactional(readOnly = true)
  public PasswordStatusResponse status() {
    AppUser user = currentUserEntity();
    AuthPasswordPolicy.Settings settings = policy.settings();
    String reason = policy.changeReason(user, clock.instant()).orElse(null);
    return new PasswordStatusResponse(
        settings.mode().name(),
        settings.historyCount(),
        settings.minAgeDays(),
        settings.maxAgeDays(),
        user.getPasswordChangedAt(),
        settings.mode() == AuthMode.LOCAL ? policy.expiresAt(user).orElse(null) : null,
        reason != null,
        reason);
  }

  /**
   * "Forgot password?": e-mails a single-use link to the registered address. The answer is the same
   * whether or not the user exists, so the screen reveals no user names; users without an e-mail
   * address, disabled users and DIRECTORY mode get no link.
   *
   * @param userId user name
   */
  public void requestReset(String userId) {
    if (userId != null && !userId.isBlank() && policy.mode() == AuthMode.LOCAL) {
      users
          .findByUsernameIgnoreCase(userId.trim())
          .filter(u -> u.isEnabled() && u.getEmail() != null && !u.getEmail().isBlank())
          .ifPresent(this::issueLink);
    }
  }

  private void issueLink(AppUser user) {
    Instant now = clock.instant();
    links.findByUsernameIgnoreCaseAndUsedAtIsNull(user.getUsername()).forEach(l -> l.use(now));
    String token = newToken();
    Instant expiresAt = now.plus(LINK_VALIDITY);
    links.save(new PasswordResetToken(sha256(token), user.getUsername(), now, expiresAt));
    events.publishEvent(
        new PasswordResetRequested(
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            resetUrl + "?token=" + token,
            expiresAt));
    audit.recordIndependently(
        user.getUsername(),
        ENTITY,
        user.getUsername(),
        AuditAction.UPDATE,
        "Password reset link e-mailed");
  }

  /**
   * Whether a reset link can still be used (the reset page checks it before showing the form).
   *
   * @param token token of the link
   * @return the link's user and expiry
   * @throws BusinessRuleException RESET_LINK_INVALID or RESET_LINK_EXPIRED
   */
  @Transactional(readOnly = true)
  public LinkStatus checkLink(String token) {
    PasswordResetToken link = usableLink(token);
    return new LinkStatus(link.getUsername(), link.getExpiresAt());
  }

  /**
   * Sets a new password with a reset link; the link works once (FR-UA-005 R3).
   *
   * @param token token of the link
   * @param newPassword new password (length and complexity checked on the request)
   */
  public void resetWithLink(String token, String newPassword) {
    policy.requireLocalMode();
    PasswordResetToken link = usableLink(token);
    AppUser user =
        users
            .findByUsernameIgnoreCase(link.getUsername())
            .filter(AppUser::isEnabled)
            .orElseThrow(AuthPasswordService::invalidLink);
    policy.checkReuse(user, newPassword);
    Instant now = clock.instant();
    link.use(now);
    setPassword(user, newPassword, now);
    audit.recordIndependently(
        user.getUsername(),
        ENTITY,
        user.getUsername(),
        AuditAction.UPDATE,
        "Password set with a reset link");
  }

  /**
   * Users of LOCAL sign-in whose password expires within the next days (job {@code
   * PASSWORD_EXPIRY_NOTICE}, UAM-NFR-36). Nothing in DIRECTORY mode or without a maximum age.
   *
   * @param days look-ahead in days
   * @return users and the expiry of their password
   */
  @Transactional(readOnly = true)
  public List<PasswordExpiry> expiringWithin(int days) {
    AuthPasswordPolicy.Settings settings = policy.settings();
    if (settings.mode() != AuthMode.LOCAL || settings.maxAgeDays() == 0) {
      return List.of();
    }
    Instant now = clock.instant();
    Duration maxAge = Duration.ofDays(settings.maxAgeDays());
    return users
        .findByEnabledTrueAndPasswordChangedAtGreaterThanEqualAndPasswordChangedAtLessThan(
            now.minus(maxAge), now.minus(maxAge).plus(Duration.ofDays(days)))
        .stream()
        .map(
            u ->
                new PasswordExpiry(
                    u.getUsername(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getPasswordChangedAt().plus(maxAge)))
        .toList();
  }

  private void setPassword(AppUser user, String password, Instant now) {
    String hash = encoder.encode(password);
    user.changePassword(hash, now, false);
    history.save(new PasswordHistory(user.getUsername(), hash, now));
  }

  private PasswordResetToken usableLink(String token) {
    if (token == null || token.isBlank()) {
      throw invalidLink();
    }
    PasswordResetToken link =
        links.findByTokenHash(sha256(token.trim())).orElseThrow(AuthPasswordService::invalidLink);
    if (link.isUsed()) {
      throw invalidLink();
    }
    if (link.isExpired(clock.instant())) {
      throw new BusinessRuleException(
          "RESET_LINK_EXPIRED", "This password reset link has expired. Request a new one");
    }
    return link;
  }

  private AppUser currentUserEntity() {
    return users
        .findByUsernameIgnoreCase(currentUser.username())
        .orElseThrow(() -> new BusinessRuleException("UNKNOWN_USER", "Unknown user"));
  }

  private String newToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static BusinessRuleException invalidLink() {
    return new BusinessRuleException(
        "RESET_LINK_INVALID", "This password reset link is not valid or was already used");
  }

  private static String resolveResetUrl(String configured, SecurityProperties properties) {
    if (configured != null && !configured.isBlank()) {
      return configured.trim();
    }
    List<String> origins = properties.allowedOrigins();
    String origin = origins == null || origins.isEmpty() ? DEFAULT_ORIGIN : origins.get(0);
    return (origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin) + RESET_PATH;
  }

  /**
   * SHA-256 of a token, hex encoded (only the hash is stored).
   *
   * @param token token
   * @return hash
   */
  static String sha256(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * A usable reset link.
   *
   * @param username user of the link
   * @param expiresAt end of validity
   */
  public record LinkStatus(String username, Instant expiresAt) {}

  /**
   * A password that expires soon.
   *
   * @param username user
   * @param fullName full name
   * @param email e-mail address, may be null
   * @param expiresAt expiry
   */
  public record PasswordExpiry(String username, String fullName, String email, Instant expiresAt) {}
}
