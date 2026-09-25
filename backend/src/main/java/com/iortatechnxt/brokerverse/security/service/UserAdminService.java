package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.AccessChange;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeActivity;
import com.iortatechnxt.brokerverse.security.domain.AccessSubjectType;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.PasswordHistory;
import com.iortatechnxt.brokerverse.security.domain.PasswordHistoryRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import com.iortatechnxt.brokerverse.security.service.AccessChangeRecorder.Subject;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User and role administration (security administrator functions).
 *
 * <p>Every change writes the structured access change log in the same transaction (BRD 4.003.1,
 * {@link AccessChangeRecorder}): one row per changed attribute, with the request number and the
 * approver when the change applies an approved access request ({@link ChangeAuthority}), none for a
 * direct change. Roles can be deactivated and reactivated (BRD 3.002.3 / 3.002.4): a deactivated
 * role keeps its permissions and members but grants nothing. Password changes keep the password
 * history and dates (UAM-NFR-36); a password set by an administrator must be changed by the user.
 */
@Service
@Transactional
@SuppressWarnings(
    "PMD.GodClass") // one facade of user and role changes: every change writes the change log
public class UserAdminService {

  private static final String USER = "AppUser";
  private static final String USER_LABEL = "User";
  private static final String ROLE = "Role";
  private static final String ROLES_ATTRIBUTE = "roles";
  private static final String ENABLED_ATTRIBUTE = "enabled";

  private final AppUserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder passwordEncoder;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final AccessChangeRecorder changes;
  private final PasswordHistoryRepository passwordHistory;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param users user repository
   * @param roles role repository
   * @param passwordEncoder password encoder
   * @param audit audit trail
   * @param currentUser current user
   * @param changes access change log
   * @param passwordHistory password history
   * @param clock clock
   */
  public UserAdminService(
      AppUserRepository users,
      RoleRepository roles,
      PasswordEncoder passwordEncoder,
      AuditTrailService audit,
      CurrentUser currentUser,
      AccessChangeRecorder changes,
      PasswordHistoryRepository passwordHistory,
      Clock clock) {
    this.users = users;
    this.roles = roles;
    this.passwordEncoder = passwordEncoder;
    this.audit = audit;
    this.currentUser = currentUser;
    this.changes = changes;
    this.passwordHistory = passwordHistory;
    this.clock = clock;
  }

  /**
   * Lists users.
   *
   * @return users ordered by user name
   */
  @Transactional(readOnly = true)
  public List<AppUser> listUsers() {
    return users.findAll(Sort.by("username"));
  }

  /**
   * Gets a user by name.
   *
   * @param username user name
   * @return user
   */
  @Transactional(readOnly = true)
  public AppUser getByUsername(String username) {
    return users
        .findByUsernameIgnoreCase(username)
        .orElseThrow(() -> new ResourceNotFoundException(USER_LABEL, username));
  }

  /**
   * Creates a user with an initial password (a direct change).
   *
   * @param request request
   * @param initialPassword initial password (user must change it)
   * @return user
   */
  public AppUser createUser(UserRequest request, String initialPassword) {
    return createUser(request, initialPassword, ChangeAuthority.DIRECT);
  }

  /**
   * Creates a user with an initial password on the authority of a request (BRD 1.002).
   *
   * @param request request
   * @param initialPassword initial password (user must change it)
   * @param authority request number and approver, or {@link ChangeAuthority#DIRECT}
   * @return user
   */
  public AppUser createUser(
      UserRequest request, String initialPassword, ChangeAuthority authority) {
    if (users.existsByUsernameIgnoreCase(request.username())) {
      throw new DuplicateResourceException(USER_LABEL, request.username());
    }
    AppUser user = new AppUser(request.username(), request.fullName(), null);
    setPassword(user, initialPassword, true);
    apply(user, request);
    AppUser saved = users.save(user);
    changes.recordDifferences(
        new Subject(
            AccessSubjectType.USER, saved.getUsername(), AccessChangeActivity.CREATE_USER, null),
        Map.of(),
        UserAttributes.of(saved),
        authority);
    audit.record(
        USER,
        saved.getUsername(),
        AuditAction.CREATE,
        "Created user with roles " + request.roleCodes() + requestText(authority));
    return saved;
  }

  /**
   * Updates a user (a direct change).
   *
   * @param id id
   * @param request request
   * @return user
   */
  public AppUser updateUser(Long id, UserRequest request) {
    return updateUser(id, request, ChangeAuthority.DIRECT);
  }

  /**
   * Updates a user on the authority of a request (BRD 1.003-1.006): data, roles and status.
   *
   * @param id id
   * @param request request
   * @param authority request number and approver, or {@link ChangeAuthority#DIRECT}
   * @return user
   */
  public AppUser updateUser(Long id, UserRequest request, ChangeAuthority authority) {
    AppUser user =
        users.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_LABEL, id));
    if (Objects.equals(user.getUsername(), currentUser.username())
        && !request.roleCodes().equals(roleCodes(user))) {
      throw new BusinessRuleException("SELF_ROLE_CHANGE", "You cannot change your own roles");
    }
    Map<String, String> before = UserAttributes.of(user);
    user.setFullName(request.fullName());
    apply(user, request);
    changes.recordDifferences(
        new Subject(
            AccessSubjectType.USER,
            user.getUsername(),
            AccessChangeActivity.MODIFY_USER,
            Map.of(
                ROLES_ATTRIBUTE,
                AccessChangeActivity.ROLES_CHANGED,
                ENABLED_ATTRIBUTE,
                user.isEnabled()
                    ? AccessChangeActivity.ENABLE_USER
                    : AccessChangeActivity.DISABLE_USER)),
        before,
        UserAttributes.of(user),
        authority);
    audit.record(
        USER,
        user.getUsername(),
        AuditAction.UPDATE,
        "Updated user; roles " + request.roleCodes() + requestText(authority));
    return user;
  }

  /**
   * Unlocks a locked account.
   *
   * @param id id
   * @return user
   */
  public AppUser unlock(Long id) {
    AppUser user =
        users.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_LABEL, id));
    boolean wasLocked = user.isLocked();
    user.unlock();
    if (wasLocked) {
      changes.record(
          new AccessChange(
              AccessSubjectType.USER,
              user.getUsername(),
              AccessChangeActivity.UNLOCK,
              "locked",
              Boolean.TRUE.toString(),
              Boolean.FALSE.toString()),
          ChangeAuthority.DIRECT);
    }
    audit.record(USER, user.getUsername(), AuditAction.UPDATE, "Unlocked account");
    return user;
  }

  /**
   * Resets a user's password (administrator); the user must change it at the next sign-in.
   *
   * @param id id
   * @param newPassword new password
   */
  public void resetPassword(Long id, String newPassword) {
    AppUser user =
        users.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_LABEL, id));
    setPassword(user, newPassword, true);
    user.unlock();
    changes.record(
        new AccessChange(
            AccessSubjectType.USER,
            user.getUsername(),
            AccessChangeActivity.PASSWORD_RESET,
            "password",
            null,
            "reset by administrator"),
        ChangeAuthority.DIRECT);
    audit.record(USER, user.getUsername(), AuditAction.UPDATE, "Password reset by administrator");
  }

  /**
   * Changes the current user's password after verifying the current one.
   *
   * @param currentPassword current password
   * @param newPassword new password
   */
  public void changeOwnPassword(String currentPassword, String newPassword) {
    AppUser user = getByUsername(currentUser.username());
    if (currentPassword == null
        || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new BusinessRuleException("INVALID_PASSWORD", "Current password is incorrect");
    }
    setPassword(user, newPassword, false);
    audit.record(USER, user.getUsername(), AuditAction.UPDATE, "Changed own password");
  }

  /**
   * Lists roles.
   *
   * @return roles ordered by code
   */
  @Transactional(readOnly = true)
  public List<Role> listRoles() {
    return roles.findAll(Sort.by("code"));
  }

  /**
   * One role.
   *
   * @param id id
   * @return role
   */
  @Transactional(readOnly = true)
  public Role getRole(Long id) {
    return roles.findById(id).orElseThrow(() -> new ResourceNotFoundException(ROLE, id));
  }

  /**
   * Creates a role (a direct change).
   *
   * @param request request
   * @return role
   */
  public Role createRole(RoleRequest request) {
    return createRole(request, ChangeAuthority.DIRECT);
  }

  /**
   * Creates a role on the authority of a group-profile request (BRD 3.002.1).
   *
   * @param request request
   * @param authority request number and approver, or {@link ChangeAuthority#DIRECT}
   * @return role
   */
  @CacheEvict(cacheNames = SecurityCaches.ROLE_PERMISSIONS, allEntries = true)
  public Role createRole(RoleRequest request, ChangeAuthority authority) {
    if (roles.findByCode(request.code()).isPresent()) {
      throw new DuplicateResourceException(ROLE, request.code());
    }
    Role role = new Role(request.code(), request.name());
    applyRole(role, request);
    Role saved = roles.save(role);
    changes.recordDifferences(
        new Subject(
            AccessSubjectType.ROLE, saved.getCode(), AccessChangeActivity.CREATE_ROLE, null),
        Map.of(),
        RoleAttributes.of(saved),
        authority);
    audit.record(
        ROLE,
        saved.getCode(),
        AuditAction.CREATE,
        "Created role " + request.permissions() + requestText(authority));
    return saved;
  }

  /**
   * Updates a role's name and permissions (a direct change).
   *
   * @param id id
   * @param request request
   * @return role
   */
  public Role updateRole(Long id, RoleRequest request) {
    return updateRole(id, request, ChangeAuthority.DIRECT);
  }

  /**
   * Updates a role's name, description, privilege level and permissions on the authority of a
   * group-profile or role-permission request (BRD 3.002.2; PMADD05).
   *
   * @param id id
   * @param request request
   * @param authority request number and approver, or {@link ChangeAuthority#DIRECT}
   * @return role
   */
  @CacheEvict(cacheNames = SecurityCaches.ROLE_PERMISSIONS, allEntries = true)
  public Role updateRole(Long id, RoleRequest request, ChangeAuthority authority) {
    Role role = roles.findById(id).orElseThrow(() -> new ResourceNotFoundException(ROLE, id));
    Map<String, String> before = RoleAttributes.of(role);
    role.setName(request.name());
    applyRole(role, request);
    changes.recordDifferences(
        new Subject(
            AccessSubjectType.ROLE, role.getCode(), AccessChangeActivity.ROLE_PERMISSIONS, null),
        before,
        RoleAttributes.of(role),
        authority);
    audit.record(
        ROLE,
        role.getCode(),
        AuditAction.UPDATE,
        "Permissions " + request.permissions() + requestText(authority));
    return role;
  }

  /**
   * Deactivates a role (BRD 3.002.3): it keeps its permissions and members but grants nothing.
   *
   * @param id id
   * @param authority request number and approver, or {@link ChangeAuthority#DIRECT}
   * @return role
   */
  @CacheEvict(cacheNames = SecurityCaches.ROLE_PERMISSIONS, allEntries = true)
  public Role deactivateRole(Long id, ChangeAuthority authority) {
    Role role = roles.findById(id).orElseThrow(() -> new ResourceNotFoundException(ROLE, id));
    role.deactivate(currentUser.username(), clock.instant());
    recordActivation(role, AccessChangeActivity.DEACTIVATE_ROLE, authority);
    audit.record(
        ROLE, role.getCode(), AuditAction.DEACTIVATE, "Deactivated role" + requestText(authority));
    return role;
  }

  /**
   * Reactivates a role with its last permissions (BRD 3.002.4).
   *
   * @param id id
   * @param authority request number and approver, or {@link ChangeAuthority#DIRECT}
   * @return role
   */
  @CacheEvict(cacheNames = SecurityCaches.ROLE_PERMISSIONS, allEntries = true)
  public Role reactivateRole(Long id, ChangeAuthority authority) {
    Role role = roles.findById(id).orElseThrow(() -> new ResourceNotFoundException(ROLE, id));
    role.reactivate();
    recordActivation(role, AccessChangeActivity.REACTIVATE_ROLE, authority);
    audit.record(
        ROLE, role.getCode(), AuditAction.UPDATE, "Reactivated role" + requestText(authority));
    return role;
  }

  private void recordActivation(
      Role role, AccessChangeActivity activity, ChangeAuthority authority) {
    changes.record(
        new AccessChange(
            AccessSubjectType.ROLE,
            role.getCode(),
            activity,
            "active",
            String.valueOf(!role.isActive()),
            String.valueOf(role.isActive())),
        authority);
  }

  private void setPassword(AppUser user, String password, boolean setByOther) {
    Instant now = clock.instant();
    String hash = passwordEncoder.encode(password);
    user.changePassword(hash, now, setByOther);
    passwordHistory.save(new PasswordHistory(user.getUsername(), hash, now));
  }

  private void apply(AppUser user, UserRequest request) {
    user.setEmail(request.email());
    user.setHomeBranchId(request.homeBranchId());
    user.setAuthorizationLimit(request.authorizationLimit());
    user.setEnabled(request.enabled());
    applyDirectoryAttributes(user, request);
    List<Role> found = roles.findByCodeIn(request.roleCodes());
    if (found.size() != request.roleCodes().size()) {
      throw new BusinessRuleException(
          "UNKNOWN_ROLE", "One or more roles do not exist: " + request.roleCodes());
    }
    user.replaceRoles(new HashSet<>(found));
  }

  private void applyDirectoryAttributes(AppUser user, UserRequest request) {
    if (request.windowsId() != null) {
      String windowsId = blankToNull(request.windowsId());
      if (windowsId != null && windowsIdTaken(windowsId, user.getId())) {
        throw new BusinessRuleException(
            "WINDOWS_ID_IN_USE", "Windows ID " + windowsId + " is already used by another user");
      }
      user.setWindowsId(windowsId);
    }
    if (request.businessUnitCode() != null) {
      user.setBusinessUnitCode(blankToNull(request.businessUnitCode()));
    }
    if (request.userLevel() != null) {
      user.setUserLevel(blankToNull(request.userLevel()));
    }
  }

  private boolean windowsIdTaken(String windowsId, Long userId) {
    return userId == null
        ? users.existsByWindowsIdIgnoreCase(windowsId)
        : users.existsByWindowsIdIgnoreCaseAndIdNot(windowsId, userId);
  }

  private static void applyRole(Role role, RoleRequest request) {
    role.replacePermissions(request.permissions());
    if (request.description() != null) {
      role.setDescription(blankToNull(request.description()));
    }
    role.setPrivilegeLevel(request.privilegeLevel());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static String requestText(ChangeAuthority authority) {
    return authority.isDirect() ? "" : " (request " + authority.requestNo() + ")";
  }

  private static Set<String> roleCodes(AppUser user) {
    Set<String> codes = new HashSet<>();
    user.getRoles().forEach(r -> codes.add(r.getCode()));
    return codes;
  }
}
