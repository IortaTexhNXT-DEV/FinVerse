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

  /** The user ID format in words (V1065). */
  public static final String USER_ID_FORMAT_TEXT = "USER_ID_FORMAT_TEXT";

  /** Whether requests for external (portal) users may be raised (V1065; false while no portal). */
  public static final String EXTERNAL_USERS = "UAM_EXTERNAL_USERS";

  /** Days without a sign-in after which a user is deactivated; 0 = never (V1065). */
  public static final String DORMANT_DAYS = "UAM_DORMANT_DAYS";

  /** Days before the dormant deactivation on which the user is told; 0 = no notice (V1065). */
  public static final String DORMANT_NOTICE_DAYS = "UAM_DORMANT_NOTICE_DAYS";

  private static final String DEFAULT_FORMAT_TEXT =
      "a letter followed by nine digits, for example a013000196";

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

  /**
   * The user ID format in words, shown with the format check.
   *
   * @return words
   */
  public String userIdFormatText() {
    String text = parameters.text(USER_ID_FORMAT_TEXT, "");
    return text.isBlank() ? DEFAULT_FORMAT_TEXT : text;
  }

  /**
   * Whether access requests may be raised for external (portal) users.
   *
   * @return true when switched on
   */
  public boolean externalUsers() {
    return Boolean.parseBoolean(parameters.text(EXTERNAL_USERS, FALSE));
  }

  /**
   * Days without a sign-in after which a user is deactivated; 0 = never.
   *
   * @return days
   */
  public int dormantDays() {
    return parameters.intValue(DORMANT_DAYS, 0);
  }

  /**
   * Days before the dormant deactivation on which the user is told; 0 = no notice.
   *
   * @return days
   */
  public int dormantNoticeDays() {
    return parameters.intValue(DORMANT_NOTICE_DAYS, 0);
  }
}
