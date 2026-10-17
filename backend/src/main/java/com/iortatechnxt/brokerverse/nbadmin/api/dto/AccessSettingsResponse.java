package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.service.AccessSettings;

/**
 * The User Access switches the screens follow (USER_ACCESS_DESIGN section 8).
 *
 * @param directRoleEdit UAM_DIRECT_ROLE_EDIT: the emergency direct role edit is open
 * @param anyApprover UAM_ANY_APPROVER: any approver may decide
 * @param roleApplyOnApproval UAM_ROLE_APPLY_ON_APPROVAL: group profiles apply at approval
 * @param userIdPattern USER_ID_PATTERN: format of a new user ID
 * @param userIdFormatText USER_ID_FORMAT_TEXT: the format in words
 * @param externalUsers UAM_EXTERNAL_USERS: requests for external (portal) users are switched on
 */
public record AccessSettingsResponse(
    boolean directRoleEdit,
    boolean anyApprover,
    boolean roleApplyOnApproval,
    String userIdPattern,
    String userIdFormatText,
    boolean externalUsers) {

  /**
   * Reads the settings.
   *
   * @param s settings
   * @return response
   */
  public static AccessSettingsResponse from(AccessSettings s) {
    return new AccessSettingsResponse(
        s.directRoleEdit(),
        s.anyApprover(),
        s.roleApplyOnApproval(),
        s.userIdPattern(),
        s.userIdFormatText(),
        s.externalUsers());
  }
}
