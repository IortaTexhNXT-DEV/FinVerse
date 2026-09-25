package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessUserType;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The approvers a requester may choose (BRD 1.002.1.1.3, 1.003.1.1.4, 1.004.1.1.4, 1.005.1.1.3,
 * 3.002.x; FR-UA-015, FR-UA-044): enabled holders of the approval right of the request, never the
 * requester nor the user the request is about. A user request has one approver; a group-profile
 * request one or more in order, each once.
 */
@Component
@Transactional(readOnly = true)
public class AccessApprovers {

  /** Approval right of internal requests. */
  public static final String ACCESS_APPROVE = "ACCESS_APPROVE";

  /** Approval right of external (portal) user requests, once the portal declares it (D7). */
  public static final String PORTAL_USER_APPROVE = "PORTAL_USER_APPROVE";

  private static final Set<String> PERMISSIONS =
      new TreeSet<>(Arrays.stream(Permission.values()).map(Enum::name).toList());

  private final UserDirectory directory;
  private final AppUserRepository users;
  private final CurrentUser currentUser;

  /**
   * Creates the component.
   *
   * @param directory permission holders
   * @param users users
   * @param currentUser current user (the requester)
   */
  public AccessApprovers(
      UserDirectory directory, AppUserRepository users, CurrentUser currentUser) {
    this.directory = directory;
    this.users = users;
    this.currentUser = currentUser;
  }

  /**
   * The permission an approver of a request must hold.
   *
   * @param userType user type of the request
   * @return ACCESS_APPROVE, or PORTAL_USER_APPROVE for external users once it exists
   */
  public static String approvalPermission(AccessUserType userType) {
    return userType == AccessUserType.EXTERNAL && PERMISSIONS.contains(PORTAL_USER_APPROVE)
        ? PORTAL_USER_APPROVE
        : ACCESS_APPROVE;
  }

  /**
   * Eligible approvers of a request by the current user.
   *
   * @param userType user type
   * @param subject user the request is about, null for a group-profile request
   * @return approvers with their names, by user name
   */
  public List<ApproverOption> eligible(AccessUserType userType, String subject) {
    String requester = currentUser.username();
    List<ApproverOption> result = new ArrayList<>();
    for (String name : directory.usersWithPermission(approvalPermission(userType))) {
      if (!CurrentUser.sameUser(name, requester) && !CurrentUser.sameUser(name, subject)) {
        result.add(
            new ApproverOption(
                name, users.findByUsernameIgnoreCase(name).map(AppUser::getFullName).orElse(name)));
      }
    }
    return result;
  }

  /**
   * Checks and normalises the chosen approvers on submission.
   *
   * @param chosen approvers in order
   * @param content the request
   * @return the approvers' user names as stored
   */
  public List<String> validate(List<String> chosen, AccessRequestContent content) {
    List<String> names = chosen == null ? List.of() : chosen;
    requireCount(names, content.type().isGroupProfile());
    List<ApproverOption> eligible = eligible(content.userType(), content.username());
    Set<String> seen = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    List<String> result = new ArrayList<>();
    for (String raw : names) {
      String name = raw == null ? "" : raw.trim();
      if (!seen.add(name)) {
        throw new BusinessRuleException(
            "ACCESS_APPROVER_TWICE", name + " is already an approver of this request");
      }
      result.add(resolve(name, eligible));
    }
    return result;
  }

  private static void requireCount(List<String> names, boolean groupProfile) {
    if (names.isEmpty()) {
      throw new BusinessRuleException(
          "ACCESS_APPROVER_REQUIRED",
          groupProfile ? "Add at least one approver" : "Select the approver");
    }
    if (!groupProfile && names.size() > 1) {
      throw new BusinessRuleException(
          "ACCESS_ONE_APPROVER", "A user request is decided by one approver");
    }
  }

  private static String resolve(String name, List<ApproverOption> eligible) {
    return eligible.stream()
        .map(ApproverOption::username)
        .filter(u -> CurrentUser.sameUser(u, name))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "ACCESS_APPROVER_NOT_ELIGIBLE", name + " cannot approve this request"));
  }

  /**
   * An approver in the drop-down.
   *
   * @param username user name
   * @param fullName full name
   */
  public record ApproverOption(String username, String fullName) {

    /**
     * Label "Full Name (user)".
     *
     * @return label
     */
    public String label() {
      return fullName + " (" + username.toLowerCase(Locale.ROOT) + ")";
    }
  }
}
