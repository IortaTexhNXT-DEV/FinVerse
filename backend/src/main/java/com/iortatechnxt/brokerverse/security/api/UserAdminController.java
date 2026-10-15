package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.security.api.dto.PasswordChangeRequest;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.api.dto.RoleResponse;
import com.iortatechnxt.brokerverse.security.api.dto.UserProfileResponse;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.service.ChangeAuthority;
import com.iortatechnxt.brokerverse.security.service.PasswordResetRules;
import com.iortatechnxt.brokerverse.security.service.RoleEditGuard;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.system.service.ProductModules;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Security administration: the users (read, unlock, the password of a local test account), roles
 * and permissions. Users are created and changed by UIDM-ISC over SCIM. Role creation and changes
 * are guarded (PQ17, {@link RoleEditGuard}): they carry the number of an approved group-profile
 * request, or use the audited emergency path {@code UAM_DIRECT_ROLE_EDIT}.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class UserAdminController {

  private static final String USERS = "hasAuthority('USER_MANAGE')";
  private static final String READ_USERS = "hasAnyAuthority('USER_MANAGE','USER_VIEW')";
  private static final String ROLES = "hasAuthority('ROLE_MANAGE')";

  private final UserAdminService service;
  private final RoleEditGuard guard;
  private final ProductModules modules;
  private final PasswordResetRules resetRules;

  /**
   * Creates the controller.
   *
   * @param service user administration
   * @param guard role edit guard
   * @param modules product module switches
   * @param resetRules who may reset a password (PASSWORD_RESET_BDOI_RULES)
   */
  public UserAdminController(
      UserAdminService service,
      RoleEditGuard guard,
      ProductModules modules,
      PasswordResetRules resetRules) {
    this.service = service;
    this.resetRules = resetRules;
    this.guard = guard;
    this.modules = modules;
  }

  /**
   * Lists users.
   *
   * @return users
   */
  @GetMapping("/users")
  @PreAuthorize(READ_USERS)
  public List<UserProfileResponse> users() {
    return service.listUsers().stream().map(UserProfileResponse::from).toList();
  }

  /**
   * Unlocks a user.
   *
   * @param id id
   * @return user
   */
  @PostMapping("/users/{id}/unlock")
  @PreAuthorize(USERS)
  public UserProfileResponse unlock(@PathVariable Long id) {
    return UserProfileResponse.from(service.unlock(id));
  }

  /**
   * Resets the password of a local test account: refused where local passwords are not used (in
   * production; {@code SSO_PASSWORD_RESET}) and for the administrator's own password.
   *
   * @param id id
   * @param request new password
   */
  @PostMapping("/users/{id}/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(USERS)
  public void resetPassword(
      @PathVariable Long id, @Valid @RequestBody PasswordChangeRequest request) {
    resetRules.checkAdministratorReset(id);
    service.resetPassword(id, request.newPassword());
  }

  /**
   * Lists roles.
   *
   * @return roles
   */
  @GetMapping("/roles")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGE','USER_MANAGE','USER_VIEW')")
  public List<RoleResponse> roles() {
    return service.listRoles().stream()
        .map(r -> RoleResponse.from(r, modules.moduleOfProfile(permissionCodes(r))))
        .toList();
  }

  /**
   * Lists the permissions a role can be given (the permissions of switched-off product modules are
   * not offered).
   *
   * @return permissions
   */
  @GetMapping("/permissions")
  @PreAuthorize(ROLES)
  public List<Permission> permissions() {
    return Arrays.stream(Permission.values())
        .filter(p -> modules.isPermissionActive(p.name()))
        .toList();
  }

  /**
   * Creates a role (implementation of an approved group-profile request, or the emergency path).
   *
   * @param request request
   * @param requestNo number of the approved group-profile request (optional)
   * @return role
   */
  @PostMapping("/roles")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(ROLES)
  public RoleResponse createRole(
      @Valid @RequestBody RoleRequest request, @RequestParam(required = false) String requestNo) {
    ChangeAuthority authority = guard.authorize(request.code(), requestNo);
    RoleResponse created = RoleResponse.from(service.createRole(request, authority));
    if (authority.isDirect()) {
      guard.directEditUsed(created.code(), "created");
    }
    return created;
  }

  /**
   * Updates a role (implementation of an approved group-profile request, or the emergency path).
   *
   * @param id id
   * @param request request
   * @param requestNo number of the approved group-profile request (optional)
   * @return role
   */
  @PutMapping("/roles/{id}")
  @PreAuthorize(ROLES)
  public RoleResponse updateRole(
      @PathVariable Long id,
      @Valid @RequestBody RoleRequest request,
      @RequestParam(required = false) String requestNo) {
    ChangeAuthority authority = guard.authorize(service.getRole(id).getCode(), requestNo);
    RoleResponse updated = RoleResponse.from(service.updateRole(id, request, authority));
    if (authority.isDirect()) {
      guard.directEditUsed(updated.code(), "changed");
    }
    return updated;
  }

  private static List<String> permissionCodes(Role role) {
    return role.getPermissions().stream().map(Enum::name).toList();
  }
}
