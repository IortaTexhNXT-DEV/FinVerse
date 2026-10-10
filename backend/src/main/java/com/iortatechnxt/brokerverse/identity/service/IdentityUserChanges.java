package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryStatus;
import com.iortatechnxt.brokerverse.security.domain.AccessChange;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeActivity;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLog;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLogRepository;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeSource;
import com.iortatechnxt.brokerverse.security.domain.AccessSubjectType;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.service.SecureTokens;
import com.iortatechnxt.brokerverse.security.service.UserSessionLog;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies an account of the Enterprise SSO platform to the users of the system (BDOI FRS
 * FRUM.002.01 to FRUM.003.03): creates the user of a joiner, updates the details of a mover and
 * keeps the group profiles, deactivates the user of a leaver or of an inactive, disabled, locked or
 * deactivated account and ends the open sessions, and reactivates the user of an active account
 * with the group profiles held before. Every changed attribute is written to the access change log
 * with the source as the actor and the UIDM request number as the reference.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class IdentityUserChanges {

  /** Setting: the group profiles of a UIDM-ISC event are given to the user (option b). */
  public static final String ROLES_SETTING = "UAM_PROVISIONING_ROLES";

  private static final String USER_ID_PATTERN = "USER_ID_PATTERN";
  private static final String ENABLED = "enabled";

  private final AppUserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder encoder;
  private final AccessChangeLogRepository log;
  private final UserSessionLog sessions;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param users users
   * @param roles roles (option b of the provisioning)
   * @param encoder password encoder (an unusable password for single sign-on users)
   * @param log access change log
   * @param sessions session log (a deactivated user's sessions end at once)
   * @param parameters business parameters
   * @param clock clock
   */
  public IdentityUserChanges(
      AppUserRepository users,
      RoleRepository roles,
      PasswordEncoder encoder,
      AccessChangeLogRepository log,
      UserSessionLog sessions,
      SystemParameterService parameters,
      Clock clock) {
    this.users = users;
    this.roles = roles;
    this.encoder = encoder;
    this.log = log;
    this.sessions = sessions;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Creates the user of a joiner, after the checks of FRUM.002.01.
   *
   * @param a the account
   * @param source who made the change (actor of the log) and its reference
   * @return the user created
   * @throws IdentityRefused when a rule refuses the creation
   */
  public AppUser create(DirectoryAccount a, Source source) {
    checkNewUser(a);
    AppUser user = new AppUser(a.userId(), a.fullName(), encoder.encode(SecureTokens.newToken()));
    user.setEmail(a.email());
    user.setWindowsId(a.windowsId());
    user.setEnabled(true);
    Set<Role> given = providedRoles(a);
    user.replaceRoles(given);
    users.save(user);
    record(source, user, AccessChangeActivity.CREATE_USER, "fullName", null, user.getFullName());
    record(source, user, AccessChangeActivity.CREATE_USER, "email", null, user.getEmail());
    record(source, user, AccessChangeActivity.CREATE_USER, "windowsId", null, a.windowsId());
    record(source, user, AccessChangeActivity.CREATE_USER, ENABLED, null, "true");
    if (!given.isEmpty()) {
      record(source, user, AccessChangeActivity.ROLES_CHANGED, "roles", null, codes(given));
    }
    return user;
  }

  /**
   * Updates the details of a user (mover); the group profiles stay unless option b is on.
   *
   * @param user the user
   * @param a the account
   * @param source actor and reference
   * @return number of changed attributes
   */
  public int update(AppUser user, DirectoryAccount a, Source source) {
    int changed = 0;
    String name = a.fullName();
    if (name != null && !name.equals(user.getFullName())) {
      record(source, user, AccessChangeActivity.MODIFY_USER, "fullName", user.getFullName(), name);
      user.setFullName(name);
      changed++;
    }
    if (a.email() != null && !a.email().equals(Objects.toString(user.getEmail(), ""))) {
      record(source, user, AccessChangeActivity.MODIFY_USER, "email", user.getEmail(), a.email());
      user.setEmail(a.email());
      changed++;
    }
    Set<Role> given = providedRoles(a);
    if (!given.isEmpty() && !codes(given).equals(codes(user.getRoles()))) {
      record(
          source,
          user,
          AccessChangeActivity.ROLES_CHANGED,
          "roles",
          codes(user.getRoles()),
          codes(given));
      user.replaceRoles(given);
      changed++;
    }
    return changed + applyStatus(user, a.status(), source);
  }

  /**
   * Deactivates or reactivates a user for the status of the account.
   *
   * @param user the user
   * @param status status of the account
   * @param source actor and reference
   * @return 1 when the user changed, 0 otherwise
   */
  public int applyStatus(AppUser user, DirectoryStatus status, Source source) {
    if (status.grantsAccess()) {
      if (user.isEnabled() && !user.isLocked()) {
        return 0;
      }
      record(source, user, AccessChangeActivity.ENABLE_USER, ENABLED, "false", "true");
      user.setEnabled(true);
      user.unlock();
      return 1;
    }
    if (!user.isEnabled()) {
      return 0;
    }
    record(source, user, AccessChangeActivity.DISABLE_USER, ENABLED, "true", "false");
    user.setEnabled(false);
    sessions.endAll(user.getUsername(), SessionEndReason.ADMIN_ENDED);
    return 1;
  }

  private void checkNewUser(DirectoryAccount a) {
    require(a.userId(), "MISSING_USER_ID", "The event has no user ID");
    require(a.windowsId(), "MISSING_WINDOWS_ID", "The event has no Windows ID");
    require(a.email(), "MISSING_EMAIL", "The event has no e-mail address");
    if (!userIdFollowsFormat(a.userId())) {
      throw new IdentityRefused(
          "USER_ID_FORMAT",
          "The user ID must be a letter followed by nine digits, for example a013000196");
    }
    if (users.findByUsernameIgnoreCase(a.userId()).isPresent()) {
      throw new IdentityRefused("USER_EXISTS", "User " + a.userId() + " already exists");
    }
    users
        .findByWindowsIdIgnoreCase(a.windowsId())
        .ifPresent(
            other -> {
              throw new IdentityRefused(
                  "WINDOWS_ID_TAKEN",
                  sameEmail(other, a)
                      ? "A user with the AD e-mail and Windows ID "
                          + a.windowsId()
                          + " already exists"
                      : "Windows ID " + a.windowsId() + " belongs to another user");
            });
    if (!a.status().grantsAccess()) {
      throw new IdentityRefused(
          "ACCOUNT_NOT_ACTIVE",
          "The Enterprise SSO account of " + a.windowsId() + " is not active");
    }
  }

  private static boolean sameEmail(AppUser user, DirectoryAccount a) {
    return user.getEmail() != null && user.getEmail().equals(a.email());
  }

  private boolean userIdFollowsFormat(String userId) {
    String pattern = parameters.text(USER_ID_PATTERN, "").trim();
    if (pattern.isEmpty()) {
      return true;
    }
    try {
      return Pattern.compile(pattern).matcher(userId).matches();
    } catch (PatternSyntaxException ex) {
      return true;
    }
  }

  private Set<Role> providedRoles(DirectoryAccount a) {
    if (a.groupProfiles().isEmpty()
        || !Boolean.parseBoolean(parameters.text(ROLES_SETTING, "false").trim())) {
      return Set.of();
    }
    Set<Role> found =
        roles.findByCodeIn(a.groupProfiles()).stream()
            .filter(Role::isActive)
            .collect(Collectors.toCollection(HashSet::new));
    if (found.size() != new HashSet<>(a.groupProfiles()).size()) {
      throw new IdentityRefused(
          "UNKNOWN_GROUP_PROFILE",
          "The event names a group profile that is not active: " + a.groupProfiles());
    }
    return found;
  }

  private static String codes(Set<Role> held) {
    return held.stream().map(Role::getCode).sorted().collect(Collectors.joining(","));
  }

  private static void require(String value, String code, String message) {
    if (value == null || value.isBlank()) {
      throw new IdentityRefused(code, message);
    }
  }

  private void record(
      Source source,
      AppUser user,
      AccessChangeActivity activity,
      String attribute,
      String from,
      String to) {
    Instant now = clock.instant();
    log.save(
        new AccessChangeLog(
            new AccessChange(
                AccessSubjectType.USER, user.getUsername(), activity, attribute, from, to),
            new AccessChangeSource(source.reference(), source.actor(), null),
            now));
  }

  /**
   * Who changes the user and on which reference.
   *
   * @param actor actor recorded in the access change log (UIDM-ISC, Enterprise SSO or the user)
   * @param reference UIDM request number, or the number of the event
   */
  public record Source(String actor, String reference) {}
}
