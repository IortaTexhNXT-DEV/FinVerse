package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * A group-profile change as proposed by the maker (BRD 3.002.1-4): create a group profile, change
 * its permissions or definition, deactivate or reactivate it. The justification is mandatory on
 * submission, not on a draft. Users are maintained in UIDM-ISC: a request about a user is refused
 * by the service (ACCESS_USERS_BY_UIDM).
 *
 * @param type MODIFY_ROLE_PERMISSIONS, CREATE_ROLE, DEACTIVATE_ROLE or REACTIVATE_ROLE
 * @param justification reason of the change
 * @param roleCode group profile
 * @param permissionsAdded permissions granted (CREATE_ROLE: all its permissions)
 * @param permissionsRemoved permissions withdrawn (MODIFY_ROLE_PERMISSIONS); no code may be blank
 * @param roleName name (CREATE_ROLE; optional on a change)
 * @param roleDescription description
 * @param privilegeLevel privilege level
 */
public record AccessRequestRequest(
    @NotNull AccessRequestType type,
    @Size(max = 1000) String justification,
    @Size(max = 40) String roleCode,
    Set<@NotBlank(message = "Select a permission; a permission cannot be empty") String>
        permissionsAdded,
    Set<@NotBlank(message = "Select a permission; a permission cannot be empty") String>
        permissionsRemoved,
    @Size(max = 120) String roleName,
    @Size(max = 500) String roleDescription,
    PrivilegeLevel privilegeLevel) {

  /**
   * The content of the request.
   *
   * @return content
   */
  public AccessRequestContent content() {
    if (!type.isGroupProfile()) {
      return new AccessRequestContent(type, null, null, null, Set.of(), null, justification);
    }
    RequestedRole role =
        roleName == null && roleDescription == null && privilegeLevel == null
            ? null
            : new RequestedRole(roleName, roleDescription, privilegeLevel);
    return AccessRequestContent.groupProfile(
        type,
        new RolePermissionChange(roleCode, permissionsAdded, permissionsRemoved),
        role,
        justification);
  }
}
