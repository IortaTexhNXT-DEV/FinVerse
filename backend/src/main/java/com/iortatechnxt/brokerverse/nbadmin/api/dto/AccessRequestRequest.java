package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * New access request (BRNB.085): a user request names the user; a role-permission change
 * (MODIFY_ROLE_PERMISSIONS, PMADD05) names the role and the permissions added and removed.
 *
 * @param type request type
 * @param username user to create or change (user requests)
 * @param fullName full name (create)
 * @param email e-mail (create)
 * @param roleCodes roles (create, modify roles)
 * @param homeBranchId home branch (create)
 * @param justification business justification
 * @param roleCode role to change (MODIFY_ROLE_PERMISSIONS)
 * @param permissionsAdded permissions to grant (MODIFY_ROLE_PERMISSIONS)
 * @param permissionsRemoved permissions to withdraw (MODIFY_ROLE_PERMISSIONS)
 */
public record AccessRequestRequest(
    @NotNull AccessRequestType type,
    @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
    @Size(max = 120) String fullName,
    @Email @Size(max = 120) String email,
    Set<String> roleCodes,
    Long homeBranchId,
    @NotBlank @Size(max = 1000) String justification,
    @Size(max = 40) String roleCode,
    Set<String> permissionsAdded,
    Set<String> permissionsRemoved) {

  /**
   * Request content.
   *
   * @return content
   */
  public AccessRequestContent content() {
    if (type == AccessRequestType.MODIFY_ROLE_PERMISSIONS) {
      return AccessRequestContent.rolePermissions(
          new RolePermissionChange(roleCode, permissionsAdded, permissionsRemoved), justification);
    }
    return new AccessRequestContent(
        type, username, fullName, email, roleCodes, homeBranchId, justification);
  }
}
