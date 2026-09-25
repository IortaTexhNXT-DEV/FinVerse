package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessApproverDecision;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestApprover;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessUserType;
import com.iortatechnxt.brokerverse.nbadmin.domain.ExternalParty;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An access request (user, group profile or external user) with its lifecycle data (BRD 1.008).
 *
 * @param id id
 * @param requestNo request number
 * @param type request type
 * @param userType internal or external user
 * @param summary one-line description
 * @param username user
 * @param fullName full name
 * @param email e-mail
 * @param roleCodes requested roles
 * @param homeBranchId home branch
 * @param justification remarks
 * @param status status
 * @param requestedBy requester (creator)
 * @param requestedAt creation time
 * @param decidedBy last deciding approver
 * @param decidedAt decision time
 * @param decisionComment decision comment (return remarks, rejection reason)
 * @param roleCode role of a group-profile request, else null
 * @param permissionsAdded permissions granted by a group-profile request
 * @param permissionsRemoved permissions withdrawn by a role-permission change
 * @param returnedCount times the request was returned to the requester
 * @param details user data, role data, party, effective date and batch
 * @param lifecycle submission, approvers, risk, cancellation and application
 */
public record AccessRequestResponse(
    Long id,
    String requestNo,
    AccessRequestType type,
    AccessUserType userType,
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
    int returnedCount,
    Details details,
    Lifecycle lifecycle) {

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
        r.getUserType(),
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
        r.getReturnedCount(),
        Details.from(r),
        Lifecycle.from(r));
  }

  /**
   * What the request sets besides the basic data.
   *
   * @param windowsId Windows ID
   * @param businessUnitCode business unit group
   * @param userLevel user level
   * @param reasonCode deactivation reason
   * @param unlock reactivation also unlocks
   * @param roleName role name
   * @param roleDescription role description
   * @param privilegeLevel role privilege level
   * @param partyKind party of an external user
   * @param partyCode party code
   * @param portalRole portal role
   * @param effectiveFrom date the change applies
   * @param batchId bulk batch
   */
  public record Details(
      String windowsId,
      String businessUnitCode,
      String userLevel,
      String reasonCode,
      boolean unlock,
      String roleName,
      String roleDescription,
      String privilegeLevel,
      String partyKind,
      String partyCode,
      String portalRole,
      LocalDate effectiveFrom,
      Long batchId) {

    static Details from(AccessRequest r) {
      RequestedUserData d = r.getUserData();
      Optional<RequestedRole> role = Optional.ofNullable(r.getRole());
      Optional<ExternalParty> party = Optional.ofNullable(r.getExternal());
      return new Details(
          d.windowsId(),
          d.businessUnitCode(),
          d.userLevel(),
          d.reasonCode(),
          d.unlock(),
          role.map(RequestedRole::name).orElse(null),
          role.map(RequestedRole::description).orElse(null),
          role.map(RequestedRole::privilegeLevel).map(Enum::name).orElse(null),
          party.map(ExternalParty::kind).map(Enum::name).orElse(null),
          party.map(ExternalParty::code).orElse(null),
          party.map(ExternalParty::portalRole).orElse(null),
          r.getEffectiveFrom(),
          r.getBatchId());
    }
  }

  /**
   * Where the request is in its lifecycle.
   *
   * @param submittedBy submitter
   * @param submittedAt submission time
   * @param assignedApprover approver whose decision is awaited
   * @param approvers approvers in order with their decisions
   * @param riskFlags risk flags (UAM-NFR-40)
   * @param secondApprovalRequired whether a second approval is needed
   * @param cancelReason cancellation reason
   * @param cancelledBy cancelling user
   * @param cancelledAt cancellation time
   * @param appliedAt application time
   * @param implementedBy implementing System Administrator
   * @param implementedAt implementation time
   * @param applyError why a scheduled or bulk line could not be applied
   */
  public record Lifecycle(
      String submittedBy,
      Instant submittedAt,
      String assignedApprover,
      List<ApproverStep> approvers,
      Set<String> riskFlags,
      boolean secondApprovalRequired,
      String cancelReason,
      String cancelledBy,
      Instant cancelledAt,
      Instant appliedAt,
      String implementedBy,
      Instant implementedAt,
      String applyError) {

    static Lifecycle from(AccessRequest r) {
      return new Lifecycle(
          r.getSubmittedBy(),
          r.getSubmittedAt(),
          r.getAssignedApprover(),
          r.getApprovers().stream().map(ApproverStep::from).toList(),
          r.riskFlags().stream().map(Enum::name).collect(Collectors.toSet()),
          r.isSecondApprovalRequired(),
          r.getCancelReason(),
          r.getCancelledBy(),
          r.getCancelledAt(),
          r.getAppliedAt(),
          r.getImplementedBy(),
          r.getImplementedAt(),
          r.getApplyError());
    }
  }

  /**
   * One approver of the request.
   *
   * @param sequence order
   * @param approver user
   * @param decision decision
   * @param remarks remarks
   * @param decidedAt decision time
   */
  public record ApproverStep(
      int sequence,
      String approver,
      AccessApproverDecision decision,
      String remarks,
      Instant decidedAt) {

    static ApproverStep from(AccessRequestApprover a) {
      return new ApproverStep(
          a.getSequence(), a.getApprover(), a.getDecision(), a.getRemarks(), a.getDecidedAt());
    }
  }
}
