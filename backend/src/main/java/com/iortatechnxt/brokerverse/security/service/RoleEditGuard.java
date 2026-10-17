package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guard of the direct role edits on the Roles screen (PQ17; USER_ACCESS_DESIGN section 9): a role
 * is created or changed only on the authority of an approved group-profile request waiting for
 * implementation ({@link ApprovedRoleRequests}), or through the audited emergency path {@value
 * #DIRECT_EDIT_PARAMETER}. An emergency edit is audited and published as {@link DirectRoleEditUsed}
 * (alert {@code UAM_DIRECT_ROLE_EDIT}).
 *
 * <p>The parameter is seeded true (today's behaviour) until the implement-request flow is live.
 */
@Service
@Transactional
public class RoleEditGuard {

  /** Business parameter of the emergency path. */
  public static final String DIRECT_EDIT_PARAMETER = "UAM_DIRECT_ROLE_EDIT";

  private static final String ENTITY = "Role";

  private final ApprovedRoleRequests approvedRequests;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final ApplicationEventPublisher events;

  /**
   * Creates the guard.
   *
   * @param approvedRequests approved group-profile requests (port)
   * @param parameters business parameters
   * @param audit audit trail
   * @param currentUser current user
   * @param events event publisher
   */
  public RoleEditGuard(
      ApprovedRoleRequests approvedRequests,
      SystemParameterService parameters,
      AuditTrailService audit,
      CurrentUser currentUser,
      ApplicationEventPublisher events) {
    this.approvedRequests = approvedRequests;
    this.parameters = parameters;
    this.audit = audit;
    this.currentUser = currentUser;
    this.events = events;
  }

  /**
   * The authority for a role edit.
   *
   * @param roleCode role to create or change
   * @param requestNo number of the approved group-profile request, null for none
   * @return the request and its approver, or {@link ChangeAuthority#DIRECT} for the emergency path
   * @throws BusinessRuleException {@code ROLE_EDIT_BY_REQUEST} when neither allows the edit
   */
  @Transactional(readOnly = true)
  public ChangeAuthority authorize(String roleCode, String requestNo) {
    if (requestNo != null && !requestNo.isBlank()) {
      return approvedRequests
          .approverOf(requestNo.strip(), roleCode)
          .map(approver -> ChangeAuthority.request(requestNo.strip(), approver))
          .orElseThrow(
              () ->
                  new BusinessRuleException(
                      "ROLE_REQUEST_NOT_APPROVED",
                      "Request "
                          + requestNo
                          + " is not an approved group-profile request for role "
                          + roleCode));
    }
    if (!directEditAllowed()) {
      throw new BusinessRuleException(
          "ROLE_EDIT_BY_REQUEST",
          "Roles are changed through an approved group-profile request; implement the request"
              + " instead");
    }
    return ChangeAuthority.DIRECT;
  }

  /**
   * Whether the emergency path is open.
   *
   * @return true when {@value #DIRECT_EDIT_PARAMETER} is true
   */
  @Transactional(readOnly = true)
  public boolean directEditAllowed() {
    return Boolean.parseBoolean(parameters.text(DIRECT_EDIT_PARAMETER, Boolean.FALSE.toString()));
  }

  /**
   * Records the use of the emergency path after a direct edit.
   *
   * @param roleCode role
   * @param change what was done ("created" / "changed")
   */
  public void directEditUsed(String roleCode, String change) {
    audit.record(
        ENTITY,
        roleCode,
        AuditAction.UPDATE,
        "Role " + change + " directly (emergency path " + DIRECT_EDIT_PARAMETER + ")");
    events.publishEvent(new DirectRoleEditUsed(roleCode, change, currentUser.username()));
  }
}
