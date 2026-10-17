package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.time.LocalDate;
import java.util.Set;

/**
 * What an access request asks for (BRD 1.002-1.005, 3.002; PMADD05).
 *
 * @param type request type
 * @param username user to create or change (user requests)
 * @param fullName full name (create; new name on a modification)
 * @param email e-mail (create; new e-mail on a modification)
 * @param roleCodes roles (create, modify)
 * @param homeBranchId home branch (create; new branch on a modification)
 * @param justification business justification (remarks); mandatory on submission
 * @param permissionChange the role and its permissions (group-profile types), else null
 * @param userData Windows ID, business unit, user level, reason, unlock (user requests)
 * @param role name, description and privilege level (CREATE_ROLE; optional on a role change)
 * @param external party and portal role of an external (portal) user, null for an internal user
 * @param effectiveFrom date the change applies (UAM-NFR-14), null for "on approval"
 */
public record AccessRequestContent(
    AccessRequestType type,
    String username,
    String fullName,
    String email,
    Set<String> roleCodes,
    Long homeBranchId,
    String justification,
    RolePermissionChange permissionChange,
    RequestedUserData userData,
    RequestedRole role,
    ExternalParty external,
    LocalDate effectiveFrom) {

  /** Defensive copy; absent user data is empty. */
  public AccessRequestContent {
    roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    userData = userData == null ? RequestedUserData.NONE : userData;
  }

  /**
   * A user request (create, change roles, disable, enable) without the BRD-11 attributes.
   *
   * @param type request type
   * @param username user to create or change
   * @param fullName full name (create)
   * @param email e-mail (create)
   * @param roleCodes roles (create, modify roles)
   * @param homeBranchId home branch (create)
   * @param justification business justification
   */
  public AccessRequestContent(
      AccessRequestType type,
      String username,
      String fullName,
      String email,
      Set<String> roleCodes,
      Long homeBranchId,
      String justification) {
    this(
        type,
        username,
        fullName,
        email,
        roleCodes,
        homeBranchId,
        justification,
        null,
        null,
        null,
        null,
        null);
  }

  /**
   * A request with a role-permission change (PMADD05) and no BRD-11 attributes.
   *
   * @param type request type
   * @param username user
   * @param fullName full name
   * @param email e-mail
   * @param roleCodes roles
   * @param homeBranchId home branch
   * @param justification business justification
   * @param permissionChange role and permissions added / removed
   */
  public AccessRequestContent(
      AccessRequestType type,
      String username,
      String fullName,
      String email,
      Set<String> roleCodes,
      Long homeBranchId,
      String justification,
      RolePermissionChange permissionChange) {
    this(
        type,
        username,
        fullName,
        email,
        roleCodes,
        homeBranchId,
        justification,
        permissionChange,
        null,
        null,
        null,
        null);
  }

  /**
   * A role-permission change request (PMADD05).
   *
   * @param change role and permissions added / removed
   * @param justification business justification
   * @return request content
   */
  public static AccessRequestContent rolePermissions(
      RolePermissionChange change, String justification) {
    return groupProfile(AccessRequestType.MODIFY_ROLE_PERMISSIONS, change, null, justification);
  }

  /**
   * A group-profile request (BRD 3.002): create, change, deactivate or reactivate a role.
   *
   * @param type group-profile type
   * @param change role code and permissions added (create, change) / removed (change)
   * @param role name, description and level (create; optional on a change), else null
   * @param justification remarks
   * @return request content
   */
  public static AccessRequestContent groupProfile(
      AccessRequestType type,
      RolePermissionChange change,
      RequestedRole role,
      String justification) {
    return new AccessRequestContent(
        type, null, null, null, Set.of(), null, justification, change, null, role, null, null);
  }

  /**
   * The same request with user data.
   *
   * @param data Windows ID, business unit, user level, reason, unlock
   * @return content
   */
  public AccessRequestContent withUserData(RequestedUserData data) {
    return copy(data, external, effectiveFrom, justification);
  }

  /**
   * The same request for an external (portal) user.
   *
   * @param party party and portal role
   * @return content
   */
  public AccessRequestContent withExternal(ExternalParty party) {
    return copy(userData, party, effectiveFrom, justification);
  }

  /**
   * The same request with an effective date.
   *
   * @param date date the change applies, null for "on approval"
   * @return content
   */
  public AccessRequestContent withEffectiveFrom(LocalDate date) {
    return copy(userData, external, date, justification);
  }

  /**
   * The same request with other remarks.
   *
   * @param text justification
   * @return content
   */
  public AccessRequestContent withJustification(String text) {
    return copy(userData, external, effectiveFrom, text);
  }

  private AccessRequestContent copy(
      RequestedUserData data, ExternalParty party, LocalDate date, String text) {
    return new AccessRequestContent(
        type,
        username,
        fullName,
        email,
        roleCodes,
        homeBranchId,
        text,
        permissionChange,
        data,
        role,
        party,
        date);
  }

  /**
   * Whom the request is about.
   *
   * @return EXTERNAL when a party is given, else INTERNAL
   */
  public AccessUserType userType() {
    return external == null ? AccessUserType.INTERNAL : AccessUserType.EXTERNAL;
  }

  /**
   * The role code of a group-profile request.
   *
   * @return role code, null for a user request
   */
  public String roleCode() {
    return permissionChange == null ? null : permissionChange.roleCode();
  }
}
