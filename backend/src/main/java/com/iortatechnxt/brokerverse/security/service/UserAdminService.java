package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User and role administration (security administrator functions). */
@Service
@Transactional
public class UserAdminService {

  private static final String USER = "AppUser";
  private static final String USER_LABEL = "User";
  private static final String ROLE = "Role";

  private final AppUserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder passwordEncoder;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param users user repository
   * @param roles role repository
   * @param passwordEncoder password encoder
   * @param audit audit trail
   * @param currentUser current user
   */
  public UserAdminService(
      AppUserRepository users,
      RoleRepository roles,
      PasswordEncoder passwordEncoder,
      AuditTrailService audit,
      CurrentUser currentUser) {
    this.users = users;
    this.roles = roles;
    this.passwordEncoder = passwordEncoder;
    this.audit = audit;
    this.currentUser = currentUser;
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
   * Creates a user with an initial password.
   *
   * @param request request
   * @param initialPassword initial password (user must change it)
   * @return user
   */
  public AppUser createUser(UserRequest request, String initialPassword) {
    if (users.existsByUsernameIgnoreCase(request.username())) {
      throw new DuplicateResourceException(USER_LABEL, request.username());
    }
    AppUser user =
        new AppUser(
            request.username(), request.fullName(), passwordEncoder.encode(initialPassword));
    apply(user, request);
    AppUser saved = users.save(user);
    audit.record(
        USER,
        saved.getUsername(),
        AuditAction.CREATE,
        "Created user with roles " + request.roleCodes());
    return saved;
  }

  /**
   * Updates a user.
   *
   * @param id id
   * @param request request
   * @return user
   */
  public AppUser updateUser(Long id, UserRequest request) {
    AppUser user =
        users.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_LABEL, id));
    if (Objects.equals(user.getUsername(), currentUser.username())
        && !request.roleCodes().equals(roleCodes(user))) {
      throw new BusinessRuleException("SELF_ROLE_CHANGE", "You cannot change your own roles");
    }
    user.setFullName(request.fullName());
    apply(user, request);
    audit.record(
        USER, user.getUsername(), AuditAction.UPDATE, "Updated user; roles " + request.roleCodes());
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
    user.unlock();
    audit.record(USER, user.getUsername(), AuditAction.UPDATE, "Unlocked account");
    return user;
  }

  /**
   * Resets a user's password (administrator).
   *
   * @param id id
   * @param newPassword new password
   */
  public void resetPassword(Long id, String newPassword) {
    AppUser user =
        users.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_LABEL, id));
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.unlock();
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
    user.setPasswordHash(passwordEncoder.encode(newPassword));
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
   * Creates a role.
   *
   * @param request request
   * @return role
   */
  @CacheEvict(cacheNames = SecurityCaches.ROLE_PERMISSIONS, allEntries = true)
  public Role createRole(RoleRequest request) {
    if (roles.findByCode(request.code()).isPresent()) {
      throw new DuplicateResourceException(ROLE, request.code());
    }
    Role role = new Role(request.code(), request.name());
    role.replacePermissions(request.permissions());
    Role saved = roles.save(role);
    audit.record(
        ROLE, saved.getCode(), AuditAction.CREATE, "Created role " + request.permissions());
    return saved;
  }

  /**
   * Updates a role's name and permissions.
   *
   * @param id id
   * @param request request
   * @return role
   */
  @CacheEvict(cacheNames = SecurityCaches.ROLE_PERMISSIONS, allEntries = true)
  public Role updateRole(Long id, RoleRequest request) {
    Role role = roles.findById(id).orElseThrow(() -> new ResourceNotFoundException(ROLE, id));
    role.setName(request.name());
    role.replacePermissions(request.permissions());
    audit.record(ROLE, role.getCode(), AuditAction.UPDATE, "Permissions " + request.permissions());
    return role;
  }

  private void apply(AppUser user, UserRequest request) {
    user.setEmail(request.email());
    user.setHomeBranchId(request.homeBranchId());
    user.setAuthorizationLimit(request.authorizationLimit());
    user.setEnabled(request.enabled());
    List<Role> found = roles.findByCodeIn(request.roleCodes());
    if (found.size() != request.roleCodes().size()) {
      throw new BusinessRuleException(
          "UNKNOWN_ROLE", "One or more roles do not exist: " + request.roleCodes());
    }
    user.replaceRoles(new HashSet<>(found));
  }

  private static Set<String> roleCodes(AppUser user) {
    Set<String> codes = new HashSet<>();
    user.getRoles().forEach(r -> codes.add(r.getCode()));
    return codes;
  }
}
