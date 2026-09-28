package com.iortatechnxt.brokerverse.security.service.mfa;

import com.iortatechnxt.brokerverse.common.util.AsciiCase;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.security.service.sso.SsoProperties;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Who must confirm a sign-in with the second factor (parameter {@code MFA_POLICY}, a security
 * parameter changed under a second approval):
 *
 * <ul>
 *   <li>{@code ALL}: every user; {@code PRIVILEGED} (as delivered): users holding an active role of
 *       privilege level HIGH or ADMIN; {@code OFF}: nobody. An unknown value counts as ALL.
 *   <li>A user who enrolled an authenticator app is always asked for a code, whatever the policy.
 *   <li>In OIDC or SAML mode the break-glass administrators always need the second factor; users
 *       signed in by the identity provider only when {@code
 *       brokerverse.security.sso.require-local-mfa} is on (the provider enforces its own).
 * </ul>
 *
 * <p>On a developer's machine without the key of the secrets ({@code
 * BROKERVERSE_MFA_ENCRYPTION_KEY}) the second factor cannot be enrolled and is not asked; outside
 * local the start-up safeguards require the key.
 */
@Component
public class MfaPolicy {

  /** Parameter of the policy. */
  public static final String PARAMETER = "MFA_POLICY";

  /** Parameter of the days a device may be remembered (0 = never). */
  public static final String REMEMBER_DAYS = "MFA_REMEMBER_DEVICE_DAYS";

  /** Parameter of the name shown in the authenticator app. */
  public static final String ISSUER = "MFA_ISSUER_NAME";

  private static final Logger LOG = LoggerFactory.getLogger(MfaPolicy.class);
  private static final int MAX_REMEMBER_DAYS = 30;

  private final SystemParameterService parameters;
  private final SsoProperties sso;
  private final MfaSecretCipher cipher;
  private final boolean local;

  /**
   * Creates the policy.
   *
   * @param parameters business parameters
   * @param sso single sign-on settings (break-glass administrators)
   * @param cipher key of the secrets
   * @param environment kind of environment
   */
  public MfaPolicy(
      SystemParameterService parameters,
      SsoProperties sso,
      MfaSecretCipher cipher,
      @Value("${brokerverse.environment:local}") String environment) {
    this.parameters = parameters;
    this.sso = sso;
    this.cipher = cipher;
    this.local = AsciiCase.equalsIgnoreCase("local", environment == null ? "" : environment.trim());
  }

  /** The policy values. */
  public enum Level {
    ALL,
    PRIVILEGED,
    OFF
  }

  /**
   * The policy in force.
   *
   * @return level; an unknown value is ALL
   */
  public Level level() {
    String value = AsciiCase.upper(parameters.text(PARAMETER, Level.PRIVILEGED.name()).trim());
    for (Level level : Level.values()) {
      if (level.name().equals(value)) {
        return level;
      }
    }
    return Level.ALL;
  }

  /**
   * Whether the second factor is required for a sign-in of a user, whether or not the user has
   * enrolled one.
   *
   * @param user user
   * @param mode sign-in mode in force
   * @param singleSignOn whether the user signed in at the identity provider
   * @return true when required
   */
  public boolean required(AppUser user, AuthMode mode, boolean singleSignOn) {
    if (!cipher.configured()) {
      if (local) {
        LOG.warn("Second factor not asked: BROKERVERSE_MFA_ENCRYPTION_KEY is not set (local)");
        return false;
      }
      throw new IllegalStateException("BROKERVERSE_MFA_ENCRYPTION_KEY is not set");
    }
    if (singleSignOn) {
      return sso.requireLocalMfa();
    }
    if (mode.singleSignOn() && sso.isBreakGlass(user.getUsername())) {
      return true;
    }
    return switch (level()) {
      case ALL -> true;
      case OFF -> false;
      case PRIVILEGED -> privileged(user);
    };
  }

  /**
   * Whether a user holds an active role of privilege level HIGH or ADMIN.
   *
   * @param user user
   * @return true for a privileged user
   */
  public static boolean privileged(AppUser user) {
    return user.getRoles().stream()
        .filter(Role::isActive)
        .map(Role::getPrivilegeLevel)
        .anyMatch(l -> l == PrivilegeLevel.HIGH || l == PrivilegeLevel.ADMIN);
  }

  /**
   * Days a device may be remembered after a confirmed code.
   *
   * @return days, 0 = never
   */
  public int rememberDays() {
    return Math.clamp(parameters.intValue(REMEMBER_DAYS, 0), 0, MAX_REMEMBER_DAYS);
  }

  /**
   * Name of the system in the authenticator app.
   *
   * @return issuer name
   */
  public String issuer() {
    String issuer = parameters.text(ISSUER, "").trim();
    return issuer.isEmpty() ? "iNXT BrokerVerse" : issuer;
  }

  /**
   * Whether the secrets can be stored (a key is configured).
   *
   * @return true when enrolment is possible
   */
  public boolean available() {
    return cipher.configured();
  }
}
