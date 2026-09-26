package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.security.api.dto.PasswordChangeRequest;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.api.dto.RoleResponse;
import com.iortatechnxt.brokerverse.security.api.dto.UserProfileResponse;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.ChangeAuthority;
import com.iortatechnxt.brokerverse.security.service.RoleEditGuard;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import jakarta.validation.Valid;
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
 * Security administration: users, roles and permissions. Role creation and changes are guarded
 * (PQ17, {@link RoleEditGuard}): they carry the number of an approved group-profile request, or use
 * the audited emergency path {@code UAM_DIRECT_ROLE_EDIT}.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class UserAdminController {

  private static final String USERS = "hasAuthority('USER_MANAGE')";
  private static final String ROLES = "hasAuthority('ROLE_MANAGE')";

  private final UserAdminService service;
  private final RoleEditGuard guard;

  /**
   * Creates the controller.
   *
   * @param service user administration
   * @param guard role edit guard
   */
  public UserAdminController(UserAdminService service, RoleEditGuard guard) {
    this.service = service;
    this.guard = guard;
  }

  /**
   * Lists users.
   *
   * @return users
   */
  @GetMapping("/users")
  @PreAuthorize(USERS)
  public List<UserProfileResponse> users() {
    return service.listUsers().stream().map(UserProfileResponse::from).toList();
  }

  /**
   * Creates a user. The initial password is supplied in a separate object and never echoed.
   *
   * @param request user data
   * @return user
   */
  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(USERS)
  public UserProfileResponse create(@Valid @RequestBody CreateUserRequest request) {
    return UserProfileResponse.from(
        service.createUser(request.user(), request.initialPassword().newPassword()));
  }

  /**
   * Updates a user.
   *
   * @param id id
   * @param request request
   * @return user
   */
  @PutMapping("/users/{id}")
  @PreAuthorize(USERS)
  public UserProfileResponse update(
      @PathVariable Long id, @Valid @RequestBody UserRequest request) {
    return UserProfileResponse.from(service.updateUser(id, request));
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
   * Resets a user's password.
   *
   * @param id id
   * @param request new password
   */
  @PostMapping("/users/{id}/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(USERS)
  public void resetPassword(
      @PathVariable Long id, @Valid @RequestBody PasswordChangeRequest request) {
    service.resetPassword(id, request.newPassword());
  }

  /**
   * Lists roles.
   *
   * @return roles
   */
  @GetMapping("/roles")
  @PreAuthorize("hasAnyAuthority('ROLE_MANAGE','USER_MANAGE')")
  public List<RoleResponse> roles() {
    return service.listRoles().stream().map(RoleResponse::from).toList();
  }

  /**
   * Lists the permissions a role can be given (the insurer-only permissions are not offered).
   *
   * @return permissions
   */
  @GetMapping("/permissions")
  @PreAuthorize(ROLES)
  public List<Permission> permissions() {
    return Permission.offered();
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

  /**
   * User creation payload.
   *
   * @param user user data
   * @param initialPassword initial password (validated against the password policy)
   */
  public record CreateUserRequest(
      @Valid UserRequest user, @Valid PasswordChangeRequest initialPassword) {}
}
