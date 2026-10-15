package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.service.DataScopeService;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full checks of a request about an internal user on submission and again at approval (BRD
 * 1.002-1.005; FR-UA-011 to FR-UA-014): the user ID format for a new user (USER_ID_PATTERN), the
 * user exists or not, is enabled or not, the Windows ID is free, a modification changes something,
 * and a requested data scope names known companies and branches within the scope of the requester
 * or approver (V1240). A request carries no group profiles: UIDM-ISC orchestrates the membership of
 * the group profiles, applied through the provisioning interface (decision option A).
 */
@Component
@Transactional(readOnly = true)
public class UserRequestValidator {

  /** Letters, digits, dots, dashes and underscores (the user name rule of the platform). */
  static final Pattern USERNAME = Pattern.compile("[a-zA-Z0-9._-]{3,50}");

  /** Error code of group profiles on a request about a user. */
  public static final String GROUP_PROFILES_BY_UIDM = "ACCESS_GROUP_PROFILES_BY_UIDM";

  /** The group profiles of a user come from UIDM-ISC (decision option A). */
  public static final String GROUP_PROFILES_BY_UIDM_TEXT =
      "The group profiles of a user are assigned through UIDM-ISC; a request does not give,"
          + " change or remove them";

  private static final String USER = "User ";

  private final AppUserRepository users;
  private final AccessSettings settings;
  private final DataScopeService dataScopes;

  /**
   * Creates the validator.
   *
   * @param users users
   * @param settings parameters (user ID format)
   * @param dataScopes data scope administration
   */
  public UserRequestValidator(
      AppUserRepository users, AccessSettings settings, DataScopeService dataScopes) {
    this.dataScopes = dataScopes;
    this.users = users;
    this.settings = settings;
  }

  /**
   * Validates and normalises a request about an internal user.
   *
   * @param c request content
   * @return normalised content
   */
  public AccessRequestContent validate(AccessRequestContent c) {
    String username = requireUsername(c.username());
    AppUser user = users.findByUsernameIgnoreCase(username).orElse(null);
    if (c.type() == AccessRequestType.CREATE_USER) {
      requireNewUser(c, username, user);
    } else {
      UserStateRules.requireState(c.type(), username, user);
      username = user.getUsername();
    }
    Set<String> roleCodes = requireNoRoles(c);
    requireFreeWindowsId(c.userData(), user);
    RequestedUserData data = RequestedScopeRule.check(c.type(), c.userData(), dataScopes);
    AccessRequestContent clean =
        new AccessRequestContent(
            c.type(),
            username,
            trimmed(c.fullName()),
            trimmed(c.email()),
            roleCodes,
            c.homeBranchId(),
            c.justification(),
            null,
            data,
            null,
            null,
            c.effectiveFrom());
    if (user != null) {
      UserStateRules.requireChange(clean, user, dataScopes.scopeOf(user.getId()));
    }
    return clean;
  }

  /**
   * The user name, trimmed, in the platform format.
   *
   * @param raw user name as entered
   * @return trimmed user name
   */
  static String requireUsername(String raw) {
    String username = raw == null ? "" : raw.trim();
    if (!USERNAME.matcher(username).matches()) {
      throw new BusinessRuleException(
          "ACCESS_USERNAME",
          "The user name has 3 to 50 letters, digits, dots, dashes or underscores");
    }
    return username;
  }

  private void requireNewUser(AccessRequestContent c, String username, AppUser existing) {
    if (existing != null) {
      throw new BusinessRuleException("ACCESS_USER_EXISTS", USER + username + " already exists");
    }
    String pattern = settings.userIdPattern();
    if (!pattern.isBlank() && !matches(pattern, username)) {
      throw new BusinessRuleException(
          "ACCESS_USER_ID_FORMAT", "The user ID must be " + settings.userIdFormatText());
    }
    if (c.fullName() == null || c.fullName().isBlank()) {
      throw new BusinessRuleException("ACCESS_FULL_NAME", "Enter the full name of the new user");
    }
  }

  private static boolean matches(String pattern, String username) {
    try {
      return Pattern.compile(pattern).matcher(username).matches();
    } catch (PatternSyntaxException e) {
      return true;
    }
  }

  /**
   * Refuses group profiles on a request about a user: UIDM-ISC orchestrates the membership of the
   * group profiles, which reaches the system through the provisioning interface.
   *
   * @param c request content
   * @return no group profiles
   */
  static Set<String> requireNoRoles(AccessRequestContent c) {
    if (c.type() == AccessRequestType.MODIFY_ROLES || !c.roleCodes().isEmpty()) {
      throw new BusinessRuleException(GROUP_PROFILES_BY_UIDM, GROUP_PROFILES_BY_UIDM_TEXT);
    }
    return Set.of();
  }

  private void requireFreeWindowsId(RequestedUserData data, AppUser user) {
    String windowsId = data.windowsId();
    if (windowsId == null) {
      return;
    }
    boolean used =
        user == null
            ? users.existsByWindowsIdIgnoreCase(windowsId)
            : users.existsByWindowsIdIgnoreCaseAndIdNot(windowsId, user.getId());
    if (used) {
      throw new BusinessRuleException(
          "ACCESS_WINDOWS_ID_IN_USE", "Windows ID " + windowsId + " belongs to another user");
    }
  }

  private static String trimmed(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
