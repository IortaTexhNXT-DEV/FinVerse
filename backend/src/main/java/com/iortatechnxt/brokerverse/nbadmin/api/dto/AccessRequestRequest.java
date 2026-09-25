package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.ExternalParty;
import com.iortatechnxt.brokerverse.nbadmin.domain.ExternalPartyKind;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * An access request as entered (BRNB.085; BRD 1.002-1.005, 3.002; decision D7). A user request
 * names the user; a group-profile request (MODIFY_ROLE_PERMISSIONS, CREATE_ROLE, DEACTIVATE_ROLE,
 * REACTIVATE_ROLE) names the role; an external (portal) user request names the party. The
 * justification is mandatory on submission, not on a draft.
 *
 * @param type request type
 * @param username user to create or change (user requests)
 * @param fullName full name (create; new name on a modification)
 * @param email e-mail (create; new e-mail on a modification)
 * @param roleCodes roles (create, modify)
 * @param homeBranchId home branch
 * @param justification remarks
 * @param roleCode role (group-profile types)
 * @param permissionsAdded permissions granted (CREATE_ROLE: all its permissions)
 * @param permissionsRemoved permissions withdrawn (MODIFY_ROLE_PERMISSIONS)
 * @param windowsId Windows ID
 * @param businessUnitCode business unit group (LOV UAM_BUSINESS_UNIT)
 * @param userLevel user level (LOV UAM_USER_LEVEL)
 * @param reasonCode deactivation reason (LOV UAM_DEACTIVATION_REASON)
 * @param unlock whether a reactivation also unlocks the account
 * @param roleName role name (CREATE_ROLE; optional on a change)
 * @param roleDescription role description
 * @param privilegeLevel role privilege level
 * @param partyKind party of an external user (INSURER, CLIENT); null for an internal user
 * @param partyCode code of the insurer or client
 * @param portalRole portal role (INSURER_USER, CLIENT_HR)
 * @param effectiveFrom date the change applies, null for "on approval" (UAM-NFR-14)
 * @param approvers approvers in order (submission; a user request has one)
 */
public record AccessRequestRequest(
    @NotNull AccessRequestType type,
    @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
    @Size(max = 120) String fullName,
    @Email @Size(max = 120) String email,
    Set<String> roleCodes,
    Long homeBranchId,
    @Size(max = 1000) String justification,
    @Size(max = 40) String roleCode,
    Set<String> permissionsAdded,
    Set<String> permissionsRemoved,
    @Size(max = 50) @Pattern(regexp = "[a-zA-Z0-9.@\\\\_-]*") String windowsId,
    @Size(max = 40) String businessUnitCode,
    @Size(max = 40) String userLevel,
    @Size(max = 40) String reasonCode,
    Boolean unlock,
    @Size(max = 120) String roleName,
    @Size(max = 500) String roleDescription,
    PrivilegeLevel privilegeLevel,
    ExternalPartyKind partyKind,
    @Size(max = 40) String partyCode,
    @Size(max = 40) String portalRole,
    LocalDate effectiveFrom,
    List<@Size(max = 50) String> approvers) {

  /**
   * Request content.
   *
   * @return content
   */
  public AccessRequestContent content() {
    if (type.isGroupProfile()) {
      RequestedRole role =
          roleName == null && roleDescription == null && privilegeLevel == null
              ? null
              : new RequestedRole(roleName, roleDescription, privilegeLevel);
      return AccessRequestContent.groupProfile(
              type,
              new RolePermissionChange(roleCode, permissionsAdded, permissionsRemoved),
              role,
              justification)
          .withEffectiveFrom(effectiveFrom);
    }
    AccessRequestContent user =
        new AccessRequestContent(
                type, username, fullName, email, roleCodes, homeBranchId, justification)
            .withUserData(
                new RequestedUserData(
                    windowsId,
                    businessUnitCode,
                    userLevel,
                    reasonCode,
                    Boolean.TRUE.equals(unlock)))
            .withEffectiveFrom(effectiveFrom);
    return partyKind == null && partyCode == null
        ? user
        : user.withExternal(new ExternalParty(partyKind, partyCode, portalRole));
  }
}
