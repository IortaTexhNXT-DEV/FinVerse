package com.iortatechnxt.brokerverse.nbadmin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * User data an access request sets besides the name, e-mail, branch and roles (BRD 1.002.1.1.1,
 * 1.004.1, 1.005.1; UAM-NFR-15): Windows ID, business unit group, user level, the reason of a
 * deactivation and whether a reactivation also unlocks the account.
 *
 * @param windowsId Windows ID (directory identity), null for none / unchanged
 * @param businessUnitCode business unit group (LOV UAM_BUSINESS_UNIT)
 * @param userLevel user level (LOV UAM_USER_LEVEL)
 * @param reasonCode deactivation reason (LOV UAM_DEACTIVATION_REASON)
 * @param unlock whether a reactivation also unlocks a locked account
 */
@Embeddable
public record RequestedUserData(
    @Column(name = "windows_id", length = 50) String windowsId,
    @Column(name = "business_unit_code", length = 40) String businessUnitCode,
    @Column(name = "user_level", length = 40) String userLevel,
    @Column(name = "reason_code", length = 40) String reasonCode,
    @Column(name = "unlock_account", nullable = false) boolean unlock) {

  /** No additional data. */
  public static final RequestedUserData NONE = new RequestedUserData(null, null, null, null, false);

  /** Blank values are absent. */
  public RequestedUserData {
    windowsId = clean(windowsId);
    businessUnitCode = clean(businessUnitCode);
    userLevel = clean(userLevel);
    reasonCode = clean(reasonCode);
  }

  private static String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /**
   * Whether the request sets any of the user attributes (not the reason or the unlock flag).
   *
   * @return true when a Windows ID, business unit or user level is given
   */
  public boolean hasAttributes() {
    return windowsId != null || businessUnitCode != null || userLevel != null;
  }
}
