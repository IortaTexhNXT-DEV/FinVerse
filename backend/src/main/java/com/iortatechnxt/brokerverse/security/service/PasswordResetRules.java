package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import org.springframework.stereotype.Component;

/**
 * Who may reset a password (BDOI FRS FRUM-004.02 and FRUM-004.03; conflict C11), chosen by the
 * security setting {@value #SETTING}:
 *
 * <ul>
 *   <li>true (BDOI's FRS, delivered): no forgotten-password link on the Login page; a password is
 *       reset only by a System Administrator other than its holder;
 *   <li>false (the earlier behaviour): the self-service reset link for local accounts and the
 *       administrator reset of any local account.
 * </ul>
 *
 * <p>Either way an administrator reset applies only to the local test accounts, where local
 * passwords may be used (never in production): the other users sign in with Microsoft Entra ID.
 */
@Component
public class PasswordResetRules {

  /** The setting. */
  public static final String SETTING = "PASSWORD_RESET_BDOI_RULES";

  private final SystemParameterService parameters;
  private final AuthPasswordPolicy policy;
  private final LocalPasswordSignIn localSignIn;
  private final CurrentUser currentUser;
  private final AppUserRepository users;

  /**
   * Creates the rules.
   *
   * @param parameters business parameters
   * @param policy sign-in mode
   * @param localSignIn whether local passwords may be used in this deployment
   * @param currentUser the administrator
   * @param users users
   */
  public PasswordResetRules(
      SystemParameterService parameters,
      AuthPasswordPolicy policy,
      LocalPasswordSignIn localSignIn,
      CurrentUser currentUser,
      AppUserRepository users) {
    this.users = users;
    this.parameters = parameters;
    this.policy = policy;
    this.localSignIn = localSignIn;
    this.currentUser = currentUser;
  }

  /**
   * Whether BDOI's rules apply.
   *
   * @return the setting, true when it is missing
   */
  public boolean bdoiRules() {
    return Boolean.parseBoolean(parameters.text(SETTING, "true").trim());
  }

  /**
   * Whether "Forgot password?" is offered on the Login page.
   *
   * @return true only for local sign-in under the earlier rules, outside production
   */
  public boolean selfServiceOffered() {
    return localSignIn.allowed() && policy.mode() == AuthMode.LOCAL && !bdoiRules();
  }

  /**
   * Whether local passwords (the test accounts) may be used in this deployment.
   *
   * @return false in production
   */
  public boolean localPasswords() {
    return localSignIn.allowed();
  }

  /**
   * Checks that the current administrator may reset the password of a user.
   *
   * @param userId id of the holder of the password; an unknown id is left to the reset (not found)
   */
  public void checkAdministratorReset(Long userId) {
    users.findById(userId).ifPresent(this::checkAdministratorReset);
  }

  /**
   * Checks that the current administrator may reset the password of a user.
   *
   * @param user the holder of the password
   * @throws BusinessRuleException OWN_PASSWORD_RESET or SSO_PASSWORD_RESET
   */
  public void checkAdministratorReset(AppUser user) {
    if (!localSignIn.allowed()) {
      throw new BusinessRuleException(
          "SSO_PASSWORD_RESET",
          "The password of "
              + user.getFullName()
              + " is kept by the organisation's sign-in service and is reset there");
    }
    if (bdoiRules() && CurrentUser.sameUser(currentUser.username(), user.getUsername())) {
      throw new BusinessRuleException(
          "OWN_PASSWORD_RESET",
          "Your own password is reset by another System Administrator; change it from My Profile");
    }
  }
}
