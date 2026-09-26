package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.api.dto.LoginResponse;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.PasswordHistory;
import com.iortatechnxt.brokerverse.security.domain.PasswordHistoryRepository;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * The password rules of LOCAL sign-in (UAM-NFR-36; FR-UA-005 R2): no reuse of the last {@code
 * PASSWORD_HISTORY_COUNT} passwords, a change due after {@code PASSWORD_MAX_AGE_DAYS} and no second
 * change within {@code PASSWORD_MIN_AGE_DAYS}. Length and complexity (R1) are checked on the
 * request ({@code PasswordChangeRequest}). With {@code AUTH_MODE} = DIRECTORY the password belongs
 * to BDO and none of these rules apply (UQ08).
 */
@Component
public class AuthPasswordPolicy {

  /** Parameter: previous passwords that may not be reused. */
  public static final String HISTORY_COUNT = "PASSWORD_HISTORY_COUNT";

  /** Parameter: days after which a password must be changed (0 = never). */
  public static final String MAX_AGE_DAYS = "PASSWORD_MAX_AGE_DAYS";

  /** Parameter: days before a changed password may be changed again. */
  public static final String MIN_AGE_DAYS = "PASSWORD_MIN_AGE_DAYS";

  private static final int DEFAULT_HISTORY = 8;
  private static final int DEFAULT_MAX_AGE = 90;
  private static final int DEFAULT_MIN_AGE = 1;
  private static final int MAX_HISTORY = 24;

  private final SystemParameterService parameters;
  private final PasswordHistoryRepository history;
  private final PasswordEncoder encoder;

  /**
   * Creates the policy.
   *
   * @param parameters business parameters
   * @param history password history
   * @param encoder password encoder
   */
  public AuthPasswordPolicy(
      SystemParameterService parameters,
      PasswordHistoryRepository history,
      PasswordEncoder encoder) {
    this.parameters = parameters;
    this.history = history;
    this.encoder = encoder;
  }

  /**
   * The current sign-in mode ({@code AUTH_MODE}).
   *
   * @return mode
   */
  public AuthMode mode() {
    return AuthMode.of(parameters.text(AuthMode.PARAMETER, AuthMode.LOCAL.name()));
  }

  /**
   * The rules in force.
   *
   * @return settings
   */
  public Settings settings() {
    return new Settings(
        mode(),
        Math.min(MAX_HISTORY, Math.max(0, parameters.intValue(HISTORY_COUNT, DEFAULT_HISTORY))),
        Math.max(0, parameters.intValue(MIN_AGE_DAYS, DEFAULT_MIN_AGE)),
        Math.max(0, parameters.intValue(MAX_AGE_DAYS, DEFAULT_MAX_AGE)));
  }

  /**
   * Refuses password changes when the directory owns the passwords.
   *
   * @throws BusinessRuleException PASSWORD_MANAGED_BY_DIRECTORY in DIRECTORY mode
   */
  public void requireLocalMode() {
    if (mode() == AuthMode.DIRECTORY) {
      throw new BusinessRuleException(
          "PASSWORD_MANAGED_BY_DIRECTORY",
          "Your password is managed by the BDO directory; change it there");
    }
  }

  /**
   * When a user's password expires; empty when it never does (no maximum age, or a password set
   * before the password dates were kept).
   *
   * @param user user
   * @return expiry time
   */
  public Optional<Instant> expiresAt(AppUser user) {
    int maxAge = settings().maxAgeDays();
    if (maxAge == 0 || user.getPasswordChangedAt() == null) {
      return Optional.empty();
    }
    return Optional.of(user.getPasswordChangedAt().plus(Duration.ofDays(maxAge)));
  }

  /**
   * Whether a user's password has expired (LOCAL mode only).
   *
   * @param user user
   * @param now time
   * @return true when a change is due
   */
  public boolean isExpired(AppUser user, Instant now) {
    return mode() == AuthMode.LOCAL && expiresAt(user).map(e -> !now.isBefore(e)).orElse(false);
  }

  /**
   * Why a user must change the password before working (LOCAL mode only): RESET after a password
   * set by someone else, EXPIRED once older than the maximum age.
   *
   * @param user user
   * @param now time
   * @return RESET, EXPIRED or empty
   */
  public Optional<String> changeReason(AppUser user, Instant now) {
    if (mode() != AuthMode.LOCAL) {
      return Optional.empty();
    }
    if (user.isMustChangePassword()) {
      return Optional.of(LoginResponse.RESET);
    }
    return isExpired(user, now) ? Optional.of(LoginResponse.EXPIRED) : Optional.empty();
  }

  /**
   * Refuses a second change within the minimum age. A password set by someone else (creation,
   * administrator reset) may be changed at once.
   *
   * @param user user
   * @param now time
   * @throws BusinessRuleException PASSWORD_CHANGED_TOO_SOON
   */
  public void checkMinimumAge(AppUser user, Instant now) {
    int minAge = settings().minAgeDays();
    Instant changed = user.getPasswordChangedAt();
    if (minAge == 0 || changed == null || user.isMustChangePassword()) {
      return;
    }
    if (now.isBefore(changed.plus(Duration.ofDays(minAge)))) {
      throw new BusinessRuleException(
          "PASSWORD_CHANGED_TOO_SOON",
          "You changed your password less than "
              + (minAge == 1 ? "a day" : minAge + " days")
              + " ago");
    }
  }

  /**
   * Refuses the current password and the last {@code PASSWORD_HISTORY_COUNT} ones.
   *
   * @param user user
   * @param rawPassword the new password
   * @throws BusinessRuleException PASSWORD_REUSED
   */
  public void checkReuse(AppUser user, String rawPassword) {
    List<String> hashes = new ArrayList<>();
    hashes.add(user.getPasswordHash());
    int count = settings().historyCount();
    if (count > 0) {
      history.findTop24ByUsernameIgnoreCaseOrderByChangedAtDesc(user.getUsername()).stream()
          .limit(count)
          .map(PasswordHistory::getPasswordHash)
          .forEach(hashes::add);
    }
    if (hashes.stream().distinct().anyMatch(h -> encoder.matches(rawPassword, h))) {
      throw new BusinessRuleException(
          "PASSWORD_REUSED", "You used this password recently. Choose another one");
    }
  }

  /**
   * The password rules in force.
   *
   * @param mode sign-in mode
   * @param historyCount previous passwords that may not be reused
   * @param minAgeDays days before a changed password may be changed again
   * @param maxAgeDays days after which a password must be changed (0 = never)
   */
  public record Settings(AuthMode mode, int historyCount, int minAgeDays, int maxAgeDays) {}
}
