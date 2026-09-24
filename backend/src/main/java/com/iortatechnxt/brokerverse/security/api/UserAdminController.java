package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.security.api.dto.PasswordChangeRequest;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.api.dto.RoleResponse;
import com.iortatechnxt.brokerverse.security.api.dto.UserProfileResponse;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Security administration: users, roles and permissions. */
@RestController
@RequestMapping("/api/v1/admin")
public class UserAdminController {

  private static final String USERS = "hasAuthority('USER_MANAGE')";
  private static final String ROLES = "hasAuthority('ROLE_MANAGE')";

  private final UserAdminService service;

  /**
   * Creates the controller.
   *
   * @param service user administration
   */
  public UserAdminController(UserAdminService service) {
    this.service = service;
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
   * Lists all permissions.
   *
   * @return permissions
   */
  @GetMapping("/permissions")
  @PreAuthorize(ROLES)
  public List<Permission> permissions() {
    return Arrays.asList(Permission.values());
  }

  /**
   * Creates a role.
   *
   * @param request request
   * @return role
   */
  @PostMapping("/roles")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(ROLES)
  public RoleResponse createRole(@Valid @RequestBody RoleRequest request) {
    return RoleResponse.from(service.createRole(request));
  }

  /**
   * Updates a role.
   *
   * @param id id
   * @param request request
   * @return role
   */
  @PutMapping("/roles/{id}")
  @PreAuthorize(ROLES)
  public RoleResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
    return RoleResponse.from(service.updateRole(id, request));
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
