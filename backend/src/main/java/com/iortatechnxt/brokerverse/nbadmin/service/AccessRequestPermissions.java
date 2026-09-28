package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * The request functions of the current user (BRD 4.002.2.1-9; USER_ACCESS_DESIGN section 6.1): each
 * request type needs its own permission, or ACCESS_REQUEST (compatibility); a correction needs
 * UAM_CORRECT. A request for an external user needs the function of its type like any other: the
 * separate portal request permission of decision D7 comes with the parked partner portal.
 */
@Component
public class AccessRequestPermissions {

  /** Umbrella of the request functions. */
  public static final String ACCESS_REQUEST = "ACCESS_REQUEST";

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
   * Requires the request function of the type.
   *
   * @param c request content
   */
  public void requireRequestPermission(AccessRequestContent c) {
    if (!holds(requestPermission(c))) {
      throw new AccessDeniedException("Not permitted to raise this access request");
    }
  }

  /**
   * Requires the correction function for a returned request (BRD 1.006; UAM_CORRECT).
   *
   * @param r request
   */
  public void requireCorrectionRight(AccessRequest r) {
    if (r.getStatus() == AccessRequestStatus.RETURNED && !holds("UAM_CORRECT")) {
      throw new AccessDeniedException("Not permitted to correct access requests");
    }
  }

  private boolean holds(String permission) {
    return currentUser.hasAuthority(permission) || currentUser.hasAuthority(ACCESS_REQUEST);
  }

  /**
   * The permission of the request function of a request.
   *
   * @param c request content
   * @return permission name
   */
  static String requestPermission(AccessRequestContent c) {
    return switch (c.type()) {
      case CREATE_USER -> "UAM_ENROLL";
      case MODIFY_ROLES, MODIFY_USER -> "UAM_MODIFY";
      case DISABLE_USER -> "UAM_DEACTIVATE";
      case ENABLE_USER -> "UAM_REACTIVATE";
      default -> "UAM_GROUP_REQUEST";
    };
  }
}
