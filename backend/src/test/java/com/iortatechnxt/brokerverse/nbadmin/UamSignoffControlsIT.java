package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.identity.service.GroupProfileChecks;
import com.iortatechnxt.brokerverse.identity.service.IdentityRefused;
import com.iortatechnxt.brokerverse.messaging.domain.Notification;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessImplementationService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessSettings;
import com.iortatechnxt.brokerverse.nbadmin.service.DormantUserJob;
import com.iortatechnxt.brokerverse.nbadmin.service.SodRuleService;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.AuthService;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.security.service.directory.LocalPasswordAuthenticator;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.domain.SystemParameter;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.SecurityParameterApprovals;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * The controls built for the BRD-11 sign-off (V1065): separation-of-duties rules under
 * maker-checker and their check on the group profiles UIDM-ISC gives a user; the authorisation
 * limit through a request; the refusal of a deactivated user without a lock-out count; the
 * dormant-user job; the notice to the members of a deactivated group profile; the second approval
 * of the security parameters.
 */
@IntegrationTest
class UamSignoffControlsIT {

  private static final String REQUESTOR = "requestor";
  private static final String APPROVER = "uamapprover";
  private static final String BADMIN = "badmin";
  private static final String INFOSEC = "infosec";

  @Autowired private AccessRequestService requests;
  @Autowired private AccessImplementationService implementations;
  @Autowired private AccessRequestRepository requestRepository;
  @Autowired private SodRuleService sodRules;
  @Autowired private DormantUserJob dormantJob;
  @Autowired private AuthService auth;
  @Autowired private UserAdminService users;
  @Autowired private SystemParameterService parameters;
  @Autowired private SecurityParameterApprovals parameterApprovals;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private NotificationRepository notifications;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private GroupProfileChecks membership;

  private String newProfile() {
    String code = "SOD_T_" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
    as.run(
        "admin",
        () ->
            users.createRole(
                new RoleRequest(code, "Profile " + code, Set.of(Permission.REPORT_VIEW))));
    return code;
  }

  /** The code of the refusal of UIDM-ISC giving one user the group profiles, null when given. */
  private String membershipRefusal(Set<String> profiles) {
    try {
      membership.groups(profiles);
      return null;
    } catch (IdentityRefused ex) {
      return ex.getCode();
    }
  }

  private static final String PASSWORD = "Controls#2026pw";

  /** A user as UIDM-ISC provisions it (created directly, without group profiles). */
  private AppUser provision(String user) {
    return as.run(
        "admin",
        () ->
            users.createUser(
                new UserRequest(
                    user,
                    "Controls User",
                    user + "@example.ph",
                    null,
                    BigDecimal.valueOf(50_000),
                    Set.of(),
                    true),
                PASSWORD));
  }

  @Test
  void separationOfDutiesRulesAreAuthorisedAndRefuseConflictingProfiles() {
    String a = newProfile();
    String b = newProfile();
    SodRule rule = as.run(BADMIN, () -> sodRules.create(a, b, "Maker and checker of payments"));
    assertThat(rule.isPending()).isTrue();
    assertThatThrownBy(() -> as.run(BADMIN, () -> sodRules.create(b, a, "Again")))
        .extracting("code")
        .isEqualTo("SOD_RULE_EXISTS");
    assertThatThrownBy(() -> as.run(BADMIN, () -> sodRules.create(a, a, "Same")))
        .extracting("code")
        .isEqualTo("SOD_SAME_PROFILE");
    assertThatThrownBy(() -> as.run(BADMIN, () -> sodRules.create(a, "NO_SUCH_PROFILE", "x")))
        .extracting("code")
        .isEqualTo("SOD_UNKNOWN_PROFILE");
    assertThat(inboxOf(INFOSEC)).contains(rule.getRuleCode());
    assertThat(inboxOf(BADMIN)).doesNotContain(rule.getRuleCode());
    assertThatThrownBy(() -> as.run(BADMIN, () -> sodRules.authorize(rule.getId())))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");

    // Pending: not yet enforced.
    assertThat(membershipRefusal(Set.of(a, b))).isNull();

    assertThat(as.run(INFOSEC, () -> sodRules.authorize(rule.getId())).isActive()).isTrue();
    assertThat(membershipRefusal(Set.of(a, b))).isEqualTo("GROUP_CONFLICT");

    as.run(BADMIN, () -> sodRules.requestDeactivation(rule.getId()));
    assertThat(inboxOf(INFOSEC)).contains(rule.getRuleCode());
    assertThatThrownBy(() -> as.run(INFOSEC, () -> sodRules.reject(rule.getId(), " ")))
        .extracting("code")
        .isEqualTo("REASON_REQUIRED");
    as.run(INFOSEC, () -> sodRules.reject(rule.getId(), "Both profiles are still needed"));
    assertThat(sodRules.list())
        .filteredOn(r -> r.getId().equals(rule.getId()))
        .singleElement()
        .matches(SodRule::isActive);
    as.run(BADMIN, () -> sodRules.requestDeactivation(rule.getId()));
    as.run(INFOSEC, () -> sodRules.authorize(rule.getId()));
    assertThat(membershipRefusal(Set.of(a, b))).isNull();

    SodRule refused = as.run(BADMIN, () -> sodRules.create(a, b, "Asked again"));
    assertThat(
            as.run(INFOSEC, () -> sodRules.reject(refused.getId(), "Already refused")).isActive())
        .isFalse();
    assertThat(sodRules.pending()).extracting(SodRule::getId).doesNotContain(refused.getId());
  }

  @Test
  void aDeactivatedUserIsRefusedWithoutCount() {
    String name = AccessRequestIT.username();
    AppUser user = provision(name);
    as.run(
        "admin",
        () ->
            users.updateUser(
                user.getId(),
                new UserRequest(
                    name,
                    user.getFullName(),
                    user.getEmail(),
                    null,
                    user.getAuthorizationLimit(),
                    Set.of(),
                    false)));
    assertThatThrownBy(() -> auth.login(name, PASSWORD))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage(LocalPasswordAuthenticator.INVALID);
    assertThatThrownBy(() -> auth.login(name, "wrong")).isInstanceOf(BadCredentialsException.class);
    assertThat(users.getByUsername(name).getFailedAttempts()).isZero();
    assertThat(users.getByUsername(name).isLocked()).isFalse();
  }

  @Test
  void dormantUsersAreWarnedThenDeactivatedDirectly() {
    String dormant = AccessRequestIT.username();
    provision(dormant);
    String soon = AccessRequestIT.username();
    provision(soon);
    LocalDate today = BusinessClock.today(Clock.systemUTC());
    backdate(dormant, today.minusDays(800));
    backdate(soon, today.minusDays(723));
    as.run("admin", () -> parameters.update(AccessSettings.DORMANT_DAYS, "730"));
    try {
      JobOutcome outcome = dormantJob.execute(today);
      assertThat(outcome.message()).contains("deactivated");
    } finally {
      as.run("admin", () -> parameters.update(AccessSettings.DORMANT_DAYS, "90"));
    }
    AppUser off = users.getByUsername(dormant);
    assertThat(off.isEnabled()).isFalse();
    assertThat(users.getByUsername(soon).isEnabled()).isTrue();
    assertThat(users.getByUsername("admin").isEnabled()).isTrue();
    assertThat(requestRepository.findAll())
        .noneMatch(
            r ->
                dormant.equals(r.getUsername())
                    && r.getRequestType() == AccessRequestType.DISABLE_USER);
    assertThat(titlesOf(soon)).contains("Your account will be deactivated");
    assertThat(titlesOf("admin")).contains("Dormant users deactivated");

    as.run("admin", () -> parameters.update(AccessSettings.DORMANT_DAYS, "0"));
    try {
      assertThat(dormantJob.execute(today).message()).contains("switched off");
    } finally {
      as.run("admin", () -> parameters.update(AccessSettings.DORMANT_DAYS, "90"));
    }
  }

  @Test
  void membersAreToldWhenTheirGroupProfileIsDeactivated() {
    String profile = newProfile();
    String member = AccessRequestIT.username();
    provision(member);
    // UIDM-ISC gives the user the group profile
    jdbc.update(
        "insert into sec_user_role (user_id, role_id) select u.id, r.id from sec_user u, sec_role r"
            + " where u.username = ? and r.code = ?",
        member,
        profile);
    AccessRequest off =
        as.run(
            BADMIN,
            () ->
                requests.create(
                    AccessRequestContent.groupProfile(
                        AccessRequestType.DEACTIVATE_ROLE,
                        new RolePermissionChange(profile, Set.of(), Set.of()),
                        null,
                        "No longer used"),
                    false));
    as.run(APPROVER, () -> requests.approve(off.getId(), null));
    as.run("admin", () -> implementations.implement(off.getId()));
    assertThat(
            notifications.findAll().stream()
                .filter(n -> member.equalsIgnoreCase(n.getRecipient()))
                .map(Notification::getBody))
        // The notification writes the code-like test name in words; the digits identify the
        // profile.
        .anyMatch(
            b ->
                b.contains("Deactivate group profile Profile ")
                    && b.contains(profile.substring(profile.lastIndexOf('_') + 1)));
  }

  @Test
  void securityParametersWaitForASecondApproval() {
    String key = "PASSWORD_MIN_AGE_DAYS";
    String before = parameters.get(key).getValue();
    SystemParameter asked = as.run("admin", () -> parameterApprovals.change(key, "2"));
    assertThat(asked.getPendingValue()).isEqualTo("2");
    assertThat(parameters.get(key).getValue()).isEqualTo(before);
    assertThatThrownBy(() -> as.run("admin", () -> parameterApprovals.change(key, "3")))
        .extracting("code")
        .isEqualTo("PARAMETER_CHANGE_PENDING");
    assertThat(inboxOf(INFOSEC)).contains(key);
    assertThatThrownBy(() -> as.run("admin", () -> parameterApprovals.approve(key)))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");
    assertThat(as.run(INFOSEC, () -> parameterApprovals.approve(key)).getValue()).isEqualTo("2");
    assertThat(parameters.intValue(key, 0)).isEqualTo(2);

    as.run("admin", () -> parameterApprovals.change(key, before));
    assertThatThrownBy(() -> as.run(INFOSEC, () -> parameterApprovals.reject(key, "")))
        .extracting("code")
        .isEqualTo("REASON_REQUIRED");
    as.run(INFOSEC, () -> parameterApprovals.reject(key, "Keep the current value"));
    assertThat(parameters.get(key).getValue()).isEqualTo("2");
    as.run("admin", () -> parameterApprovals.change(key, "3"));
    as.run("admin", () -> parameterApprovals.reject(key, null));
    assertThat(parameters.get(key).getPendingValue()).isNull();
    assertThatThrownBy(() -> as.run(INFOSEC, () -> parameterApprovals.reject(key, "Again")))
        .extracting("code")
        .isEqualTo("PARAMETER_NO_PENDING_CHANGE");
    as.run("admin", () -> parameterApprovals.change(key, before));
    as.run(INFOSEC, () -> parameterApprovals.approve(key));
    assertThat(parameters.get(key).getValue()).isEqualTo(before);
    // Texts about the change say requested, and name the requester by display name.
    assertThat(
            jdbc.queryForList(
                "select summary from audit_log where entity_type = 'SystemParameter'"
                    + " and entity_id = ?",
                String.class,
                key))
        .anyMatch(t -> t.startsWith("Requested a change of " + key))
        .anyMatch(t -> t.endsWith("requested by SIT System Administrator"))
        .noneMatch(t -> t.contains("asked"))
        .noneMatch(t -> t.endsWith(" by admin"));
    assertThat(notifications.findAll())
        .filteredOn(n -> n.getTitle().startsWith("Security setting to approve"))
        .extracting(Notification::getBody)
        .anyMatch(b -> b.startsWith("SIT System Administrator requests a change from"));

    String footer = parameters.get(SystemParameterService.REPORT_FOOTER_TEXT).getValue();
    SystemParameter direct =
        as.run(
            "admin",
            () -> parameterApprovals.change(SystemParameterService.REPORT_FOOTER_TEXT, footer));
    assertThat(direct.getPendingValue()).isNull();
  }

  private void backdate(String username, LocalDate day) {
    Instant noon = BusinessClock.startOf(day).plus(Duration.ofHours(12));
    jdbc.update(
        "update sec_user set created_at = ? where username = ?", Timestamp.from(noon), username);
  }

  private List<String> titlesOf(String username) {
    return notifications.findAll().stream()
        .filter(n -> username.equalsIgnoreCase(n.getRecipient()))
        .map(Notification::getTitle)
        .toList();
  }

  private List<String> inboxOf(String user) {
    return as.run(user, () -> inbox.inbox(null)).stream().map(PendingApproval::reference).toList();
  }
}
