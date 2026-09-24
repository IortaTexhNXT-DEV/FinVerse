package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import java.time.Instant;
import java.util.Set;

/**
 * A user access request.
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
    String decisionComment) {

  /**
   * Maps a request.
   *
   * @param r request
   * @return response
   */
  public static AccessRequestResponse from(AccessRequest r) {
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
        r.getDecisionComment());
  }
}
