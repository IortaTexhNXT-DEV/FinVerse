package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * The request functions of the current user (USER_ACCESS_DESIGN section 6.1). Users and their group
 * profiles are maintained in UIDM-ISC and reach the system over the provisioning interface, so a
 * request about a user is refused ({@value #USERS_BY_UIDM}); the dormant-user job deactivates on
 * its own authority. A group-profile change needs UAM_GROUP_REQUEST or the umbrella ACCESS_REQUEST,
 * and so does the correction of a returned change.
 */
@Component
public class AccessRequestPermissions {

  /** Umbrella of the request functions. */
  public static final String ACCESS_REQUEST = "ACCESS_REQUEST";

  /** Request function of a group-profile change (maker). */
  public static final String GROUP_REQUEST = "UAM_GROUP_REQUEST";

  /** Code of the refusal of a request about a user. */
  public static final String USERS_BY_UIDM = "ACCESS_USERS_BY_UIDM";

  private final CurrentUser currentUser;

  /**
   * Creates the component.
   *
   * @param currentUser current user
   */
  public AccessRequestPermissions(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  /**
   * Requires a group-profile change and the request function to propose it.
   *
   * @param c request content
   * @throws BusinessRuleException ACCESS_USERS_BY_UIDM for a request about a user
   */
  public void requireRequestPermission(AccessRequestContent c) {
    if (c.external() != null || c.type() != null && !c.type().isGroupProfile()) {
      throw new BusinessRuleException(
          USERS_BY_UIDM,
          "Users are created, changed, deactivated and reactivated in UIDM-ISC; the change"
              + " reaches this system over the provisioning interface");
    }
    if (!holds()) {
      throw new AccessDeniedException("Not permitted to propose a group-profile change");
    }
  }

  /**
   * Requires the request function to correct a returned change.
   *
   * @param r request
   */
  public void requireCorrectionRight(AccessRequest r) {
    if (r.getStatus() == AccessRequestStatus.RETURNED && !holds()) {
      throw new AccessDeniedException("Not permitted to correct group-profile changes");
    }
  }

  private boolean holds() {
    return currentUser.hasAuthority(GROUP_REQUEST) || currentUser.hasAuthority(ACCESS_REQUEST);
  }
}
