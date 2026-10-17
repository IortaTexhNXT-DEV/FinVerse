package com.iortatechnxt.brokerverse.dashboard.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.dashboard.domain.HomeWidget;
import com.iortatechnxt.brokerverse.dashboard.domain.RoleHome;
import com.iortatechnxt.brokerverse.dashboard.domain.RoleHomeRepository;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Role Home Set-up (Setup &amp; Administration, FR-CR-010): the System Administrator chooses and
 * orders the home-page widgets of each role with a reason, another user approves the change; the
 * home page of a user shows the widgets of the user's roles once each.
 */
@Service
@Transactional
public class RoleHomeService {

  private static final String ENTITY = "RoleHome";

  private final RoleHomeRepository homes;
  private final RoleRepository roles;
  private final AppUserRepository appUsers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final UserDisplayNames users;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param homes widget sets
   * @param roles roles
   * @param appUsers users and their roles
   * @param audit audit trail
   * @param currentUser current user
   * @param users display names in the audit texts
   * @param clock clock
   */
  public RoleHomeService(
      RoleHomeRepository homes,
      RoleRepository roles,
      AppUserRepository appUsers,
      AuditTrailService audit,
      CurrentUser currentUser,
      UserDisplayNames users,
      Clock clock) {
    this.homes = homes;
    this.roles = roles;
    this.appUsers = appUsers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.users = users;
    this.clock = clock;
  }

  /**
   * The home page of every active role.
   *
   * @return one entry per role, by role name
   */
  @Transactional(readOnly = true)
  public List<Entry> list() {
    Map<String, RoleHome> sets =
        homes.findAll().stream()
            .collect(Collectors.toMap(RoleHome::getRoleCode, Function.identity(), (a, b) -> a));
    return roles.findAll().stream()
        .filter(Role::isActive)
        .sorted(Comparator.comparing(Role::getName))
        .map(r -> new Entry(r, sets.getOrDefault(r.getCode(), new RoleHome(r.getCode()))))
        .toList();
  }

  /**
   * Requests a new set of widgets for a role; it waits for approval.
   *
   * @param roleCode role
   * @param widgets widget codes in display order
   * @param reason why
   * @return the role's home page
   */
  public Entry request(String roleCode, List<String> widgets, String reason) {
    Role role = role(roleCode);
    List<String> codes = validated(widgets);
    RoleHome home = homes.findByRoleCode(role.getCode()).orElseGet(() -> new RoleHome(roleCode));
    home.requestChange(codes, reason, currentUser.username(), clock.instant());
    homes.save(home);
    audit.record(
        ENTITY,
        roleCode,
        AuditAction.SUBMIT,
        "Requested the home-page widgets of "
            + role.getName()
            + ": "
            + names(codes)
            + ", waiting for approval. Reason: "
            + home.getPendingReason());
    return new Entry(role, home);
  }

  /**
   * Approves the requested set of a role (not by its requester) and applies it.
   *
   * @param roleCode role
   * @return the role's home page
   */
  public Entry approve(String roleCode) {
    Role role = role(roleCode);
    RoleHome home = require(roleCode);
    String requestedBy = home.getPendingBy();
    home.approveChange(currentUser.username(), clock.instant());
    homes.saveAndFlush(home);
    audit.record(
        ENTITY,
        roleCode,
        AuditAction.AUTHORIZE,
        "Home-page widgets of "
            + role.getName()
            + " set to "
            + names(home.getWidgets())
            + " as requested by "
            + users.displayName(requestedBy));
    return new Entry(role, home);
  }

  /**
   * Rejects the requested set with a reason; the requester may withdraw it without one.
   *
   * @param roleCode role
   * @param reason why
   * @return the role's home page
   */
  public Entry reject(String roleCode, String reason) {
    Role role = role(roleCode);
    RoleHome home = require(roleCode);
    String requestedBy = home.getPendingBy();
    boolean withdrawn = CurrentUser.sameUser(currentUser.username(), requestedBy);
    if (!withdrawn && (reason == null || reason.isBlank())) {
      throw new BusinessRuleException("REASON_REQUIRED", "Give the reason for the rejection");
    }
    home.rejectChange();
    audit.record(
        ENTITY,
        roleCode,
        AuditAction.REJECT,
        (withdrawn ? "Withdrew" : "Rejected")
            + " the home-page widgets of "
            + role.getName()
            + " requested by "
            + users.displayName(requestedBy)
            + (reason == null || reason.isBlank() ? "" : ". Reason: " + reason.strip()));
    return new Entry(role, home);
  }

  /**
   * The widgets of the current user's home page: the approved sets of the user's active roles, each
   * widget once, in the order of the first role (by code) that shows it. A user none of whose roles
   * has an approved set sees the delivered widgets.
   *
   * @return widget codes in display order
   */
  @Transactional(readOnly = true)
  public List<String> mine() {
    List<String> roleCodes =
        appUsers
            .findByUsernameIgnoreCase(currentUser.username())
            .map(
                u ->
                    u.getRoles().stream()
                        .filter(Role::isActive)
                        .map(Role::getCode)
                        .sorted()
                        .toList())
            .orElse(List.of());
    Map<String, RoleHome> sets =
        homes.findByRoleCodeIn(roleCodes).stream()
            .filter(h -> !h.isDelivered())
            .collect(Collectors.toMap(RoleHome::getRoleCode, Function.identity(), (a, b) -> a));
    if (sets.isEmpty()) {
      return HomeWidget.defaults();
    }
    Set<String> shown = new LinkedHashSet<>();
    for (String code : roleCodes) {
      RoleHome home = sets.get(code);
      if (home != null) {
        shown.addAll(home.getWidgets());
      }
    }
    return new ArrayList<>(shown);
  }

  private static List<String> validated(List<String> widgets) {
    if (widgets == null || widgets.isEmpty()) {
      throw new BusinessRuleException(
          "ROLE_HOME_EMPTY", "Choose at least one widget for the home page");
    }
    Set<String> codes = new LinkedHashSet<>();
    for (String code : widgets) {
      checkKnown(code);
      if (!codes.add(code)) {
        throw new BusinessRuleException(
            "ROLE_HOME_DUPLICATE_WIDGET", "Each widget can be chosen once");
      }
    }
    return List.copyOf(codes);
  }

  private static void checkKnown(String code) {
    if (code == null || HomeWidget.byCode(code).isEmpty()) {
      throw new BusinessRuleException("ROLE_HOME_UNKNOWN_WIDGET", "Unknown widget " + code);
    }
  }

  private static String names(List<String> codes) {
    return codes.stream()
        .map(c -> HomeWidget.byCode(c).map(HomeWidget::displayName).orElse(c))
        .collect(Collectors.joining(", "));
  }

  private Role role(String code) {
    return roles
        .findByCode(code)
        .filter(Role::isActive)
        .orElseThrow(() -> new ResourceNotFoundException("Role", code));
  }

  private RoleHome require(String roleCode) {
    return homes
        .findByRoleCode(roleCode)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "NO_ROLE_HOME_CHANGE_PENDING",
                    "No change of this role's home page waits for approval"));
  }

  /**
   * The home page of a role.
   *
   * @param role role
   * @param home its widget set (a new, unsaved one when the role shows the delivered widgets)
   */
  public record Entry(Role role, RoleHome home) {}
}
