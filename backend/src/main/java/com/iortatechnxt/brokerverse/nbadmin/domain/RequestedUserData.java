package com.iortatechnxt.brokerverse.nbadmin.domain;

import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Optional;

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
 * @param authorizationLimit authorisation limit of the user's approvals by amount (journals,
 *     payables), null for none / unchanged (V1065)
 * @param dataScope companies and branches the user may act for, in the text form of {@link
 *     UserDataScope#text()}; null for unchanged (create: all companies) (V1240)
 */
@Embeddable
public record RequestedUserData(
    @Column(name = "windows_id", length = 50) String windowsId,
    @Column(name = "business_unit_code", length = 40) String businessUnitCode,
    @Column(name = "user_level", length = 40) String userLevel,
    @Column(name = "reason_code", length = 40) String reasonCode,
    @Column(name = "unlock_account", nullable = false) boolean unlock,
    @Column(name = "authorization_limit", precision = 18, scale = 2) BigDecimal authorizationLimit,
    @Column(name = "data_scope", length = 2000) String dataScope) {

  /** No additional data. */
  public static final RequestedUserData NONE =
      new RequestedUserData(null, null, null, null, false, null, null);

  /**
   * User data without an authorisation limit.
   *
   * @param windowsId Windows ID
   * @param businessUnitCode business unit group
   * @param userLevel user level
   * @param reasonCode deactivation reason
   * @param unlock whether a reactivation also unlocks the account
   */
  public RequestedUserData(
      String windowsId,
      String businessUnitCode,
      String userLevel,
      String reasonCode,
      boolean unlock) {
    this(windowsId, businessUnitCode, userLevel, reasonCode, unlock, null, null);
  }

  /**
   * User data without a data scope.
   *
   * @param windowsId Windows ID
   * @param businessUnitCode business unit group
   * @param userLevel user level
   * @param reasonCode deactivation reason
   * @param unlock whether a reactivation also unlocks the account
   * @param authorizationLimit authorisation limit, null for none / unchanged
   */
  public RequestedUserData(
      String windowsId,
      String businessUnitCode,
      String userLevel,
      String reasonCode,
      boolean unlock,
      BigDecimal authorizationLimit) {
    this(windowsId, businessUnitCode, userLevel, reasonCode, unlock, authorizationLimit, null);
  }

  /**
   * The requested data scope.
   *
   * @return scope, empty when the request leaves it unchanged
   */
  public Optional<UserDataScope> requestedScope() {
    return dataScope == null ? Optional.empty() : Optional.of(UserDataScope.parse(dataScope));
  }

  /** Blank values are absent. */
  public RequestedUserData {
    windowsId = clean(windowsId);
    businessUnitCode = clean(businessUnitCode);
    userLevel = clean(userLevel);
    reasonCode = clean(reasonCode);
    dataScope = dataScope == null ? null : dataScope.trim();
  }

  private static String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /**
   * Whether the request sets any of the user attributes (not the reason or the unlock flag).
   *
   * @return true when a Windows ID, business unit, user level or authorisation limit is given
   */
  public boolean hasAttributes() {
    return windowsId != null
        || businessUnitCode != null
        || userLevel != null
        || authorizationLimit != null;
  }
}
