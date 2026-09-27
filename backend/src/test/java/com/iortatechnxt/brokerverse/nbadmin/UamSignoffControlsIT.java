package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.Notification;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessImplementationService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessSettings;
import com.iortatechnxt.brokerverse.nbadmin.service.DormantUserJob;
import com.iortatechnxt.brokerverse.nbadmin.service.SodRuleService;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.AuthService;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
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
import org.springframework.security.authentication.DisabledException;

/**
 * The controls built for the BRD-11 sign-off (V1065): separation-of-duties rules under
 * maker-checker and their check on requests; the authorisation limit through a request; the refusal
 * of a deactivated user without a lock-out count; the dormant-user job; the notice to the members
 * of a deactivated group profile; the second approval of the security parameters.
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

  private String newProfile() {
    String code = "SOD_T_" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
    as.run(
        "admin",
        () ->
            users.createRole(
                new RoleRequest(code, "Profile " + code, Set.of(Permission.REPORT_VIEW))));
    return code;
  }

  private static AccessRequestContent enrol(String user, Set<String> profiles, BigDecimal limit) {
    return new AccessRequestContent(
            AccessRequestType.CREATE_USER,
            user,
            "Controls User",
            user + "@example.ph",
            profiles,
            null,
            "Joined")
        .withUserData(new RequestedUserData(null, null, null, null, false, limit));
  }

  private AccessRequestService.Decision enrolAndApprove(String user, Set<String> profiles) {
    AccessRequest r =
        as.run(
            REQUESTOR,
            () ->
                requests.create(
                    enrol(user, profiles, BigDecimal.valueOf(50_000)), false, List.of(APPROVER)));
    return as.run(APPROVER, () -> requests.approve(r.getId(), null));
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
    assertThat(
            as.run(
                    REQUESTOR,
                    () ->
                        requests.create(
                            enrol(AccessRequestIT.username(), Set.of(a, b), null),
                            false,
                            List.of(APPROVER)))
                .getStatus())
        .isEqualTo(AccessRequestStatus.PENDING);

    assertThat(as.run(INFOSEC, () -> sodRules.authorize(rule.getId())).isActive()).isTrue();
    assertThatThrownBy(
            () ->
                as.run(
                    REQUESTOR,
                    () ->
                        requests.create(
                            enrol(AccessRequestIT.username(), Set.of(a, b), null),
                            false,
                            List.of(APPROVER))))
        .extracting("code")
        .isEqualTo("ACCESS_SOD_CONFLICT");

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
    assertThat(
            as.run(
                    REQUESTOR,
                    () ->
                        requests.create(
                            enrol(AccessRequestIT.username(), Set.of(a, b), null),
                            false,
                            List.of(APPROVER)))
                .getStatus())
        .isEqualTo(AccessRequestStatus.PENDING);

    SodRule refused = as.run(BADMIN, () -> sodRules.create(a, b, "Asked again"));
    assertThat(
            as.run(INFOSEC, () -> sodRules.reject(refused.getId(), "Already refused")).isActive())
        .isFalse();
    assertThat(sodRules.pending()).extracting(SodRule::getId).doesNotContain(refused.getId());
  }

  @Test
  void limitIsSetByRequestAndDeactivatedUserIsRefusedWithoutCount() {
    String name = AccessRequestIT.username();
    String password = enrolAndApprove(name, Set.of("MKT_AO")).temporaryPassword();
    assertThat(users.getByUsername(name).getAuthorizationLimit()).isEqualByComparingTo("50000");

    AccessRequest change =
        as.run(
            REQUESTOR,
            () ->
                requests.create(
                    new AccessRequestContent(
                            AccessRequestType.MODIFY_USER,
                            name,
                            null,
                            null,
                            Set.of(),
                            null,
                            "Higher limit")
                        .withUserData(
                            new RequestedUserData(
                                null, null, null, null, false, BigDecimal.valueOf(75_000))),
                    false,
                    List.of(APPROVER)));
    as.run(APPROVER, () -> requests.approve(change.getId(), null));
    assertThat(users.getByUsername(name).getAuthorizationLimit()).isEqualByComparingTo("75000");

    AccessRequest off =
        as.run(
            REQUESTOR,
            () ->
                requests.create(
                    new AccessRequestContent(
                            AccessRequestType.DISABLE_USER,
                            name,
                            null,
                            null,
                            Set.of(),
                            null,
                            "Resigned")
                        .withUserData(
                            new RequestedUserData(null, null, null, "RESIGNED", false, null)),
                    false,
                    List.of(APPROVER)));
    as.run(APPROVER, () -> requests.approve(off.getId(), null));
    assertThatThrownBy(() -> auth.login(name, password))
        .isInstanceOf(DisabledException.class)
        .hasMessage(AuthService.DEACTIVATED);
    assertThatThrownBy(() -> auth.login(name, "wrong")).isInstanceOf(DisabledException.class);
    assertThat(users.getByUsername(name).getFailedAttempts()).isZero();
    assertThat(users.getByUsername(name).isLocked()).isFalse();
  }

  @Test
  void dormantUsersAreWarnedThenDeactivatedByRequest() {
    String dormant = AccessRequestIT.username();
    enrolAndApprove(dormant, Set.of("MKT_AO"));
    String soon = AccessRequestIT.username();
    enrolAndApprove(soon, Set.of("MKT_AO"));
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
    AccessRequest request =
        requestRepository.findAll().stream()
            .filter(r -> dormant.equals(r.getUsername()))
            .filter(r -> r.getRequestType() == AccessRequestType.DISABLE_USER)
            .findFirst()
            .orElseThrow();
    assertThat(request.getStatus()).isEqualTo(AccessRequestStatus.APPROVED);
    assertThat(request.getUserData().reasonCode()).isEqualTo(DormantUserJob.REASON);
    assertThat(titlesOf(soon)).contains("Your account will be deactivated");
    assertThat(titlesOf(dormant)).contains("Your access changed");

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
    enrolAndApprove(member, Set.of(profile));
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
                    false,
                    List.of(APPROVER)));
    as.run(APPROVER, () -> requests.approve(off.getId(), null));
    as.run("admin", () -> implementations.implement(off.getId()));
    assertThat(
            notifications.findAll().stream()
                .filter(n -> member.equalsIgnoreCase(n.getRecipient()))
                .map(Notification::getBody))
        .anyMatch(b -> b.contains("Deactivate group profile Profile " + profile));
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
