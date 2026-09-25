package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import java.time.Instant;
import java.util.Set;

/**
 * An access request (user or role-permission change).
 *
 * @param id id
 * @param requestNo request number
 * @param type request type
 * @param summary one-line description
 * @param username user
 * @param fullName full name
 * @param email e-mail
 * @param roleCodes requested roles
 * @param homeBranchId home branch
 * @param justification justification
 * @param status status
 * @param requestedBy requester
 * @param requestedAt request time
 * @param decidedBy approver
 * @param decidedAt decision time
 * @param decisionComment decision comment
 * @param roleCode role of a role-permission change (PMADD05), else null
 * @param permissionsAdded permissions granted by a role-permission change
 * @param permissionsRemoved permissions withdrawn by a role-permission change
 * @param returnedCount times the request was returned to the requester (BASAU 2.4.1)
 */
public record AccessRequestResponse(
    Long id,
    String requestNo,
    AccessRequestType type,
    String summary,
    String username,
    String fullName,
    String email,
    Set<String> roleCodes,
    Long homeBranchId,
    String justification,
    AccessRequestStatus status,
    String requestedBy,
    Instant requestedAt,
    String decidedBy,
    Instant decidedAt,
    String decisionComment,
    String roleCode,
    Set<String> permissionsAdded,
    Set<String> permissionsRemoved,
    int returnedCount) {

  /**
   * Maps a request.
   *
   * @param r request
   * @return response
   */
  public static AccessRequestResponse from(AccessRequest r) {
    RolePermissionChange change = r.permissionChange();
    return new AccessRequestResponse(
        r.getId(),
        r.getRequestNo(),
        r.getRequestType(),
        AccessRequestService.describe(r),
        r.getUsername(),
        r.getFullName(),
        r.getEmail(),
        r.roles(),
        r.getHomeBranchId(),
        r.getJustification(),
        r.getStatus(),
        r.getCreatedBy(),
        r.getCreatedAt(),
        r.getDecidedBy(),
        r.getDecidedAt(),
        r.getDecisionComment(),
        r.getRoleCode(),
        change == null ? Set.of() : change.added(),
        change == null ? Set.of() : change.removed(),
        r.getReturnedCount());
  }
}
