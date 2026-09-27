package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.security.service.RoleEditGuard;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import org.springframework.stereotype.Component;

/**
 * The business parameters of User Access Maintenance (USER_ACCESS_DESIGN section 8), read at the
 * time of use so a change applies at once.
 */
@Component
public class AccessSettings {

  /** Any holder of ACCESS_APPROVE may decide, not only the chosen approver (UQ02). */
  public static final String ANY_APPROVER = "UAM_ANY_APPROVER";

  /** Group-profile requests apply at approval instead of being implemented (UQ03). */
  public static final String ROLE_APPLY_ON_APPROVAL = "UAM_ROLE_APPLY_ON_APPROVAL";

  /** Working hours of the out-of-hours risk flag (UAM-NFR-40, UQ07). */
  public static final String WORKING_HOURS = "UAM_WORKING_HOURS";

  /** Format of a new user ID (UAM-NFR-13, UQ05). */
  public static final String USER_ID_PATTERN = "USER_ID_PATTERN";

  private static final String FALSE = Boolean.FALSE.toString();

  private final SystemParameterService parameters;

  /**
   * Creates the settings.
   *
   * @param parameters business parameters
   */
  public AccessSettings(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Whether any approver may decide a request.
   *
   * @return {@value #ANY_APPROVER}
   */
  public boolean anyApprover() {
    return Boolean.parseBoolean(parameters.text(ANY_APPROVER, FALSE));
  }

  /**
   * Whether approved group-profile requests apply at approval.
   *
   * @return {@value #ROLE_APPLY_ON_APPROVAL}
   */
  public boolean roleApplyOnApproval() {
    return Boolean.parseBoolean(parameters.text(ROLE_APPLY_ON_APPROVAL, FALSE));
  }

  /**
   * Whether the emergency direct role edit is open.
   *
   * @return {@code UAM_DIRECT_ROLE_EDIT}
   */
  public boolean directRoleEdit() {
    return Boolean.parseBoolean(parameters.text(RoleEditGuard.DIRECT_EDIT_PARAMETER, FALSE));
  }

  /**
   * The working hours.
   *
   * @return parsed {@value #WORKING_HOURS}; always-open when blank or invalid
   */
  public WorkingHours workingHours() {
    return WorkingHours.parse(parameters.text(WORKING_HOURS, ""));
  }

  /**
   * The format of a new user ID.
   *
   * @return regular expression, blank for none
   */
  public String userIdPattern() {
    return parameters.text(USER_ID_PATTERN, "");
  }
}
