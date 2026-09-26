package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeActivity;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLog;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLogRepository;
import com.iortatechnxt.brokerverse.security.domain.AccessSubjectType;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.domain.UserSession;
import com.iortatechnxt.brokerverse.security.service.ChangeAuthority;
import com.iortatechnxt.brokerverse.security.service.JwtTokenService;
import com.iortatechnxt.brokerverse.security.service.RoleEditGuard;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.security.service.UserSessionLog;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Security extensions of User Access Maintenance (BRD-11, V1061): inactive roles grant nothing (BRD
 * 3.002.3 / 3.002.4), the structured access change log (BRD 4.003.1) with its request number and
 * approver, the insert-only rule, password history and forced change (UAM-NFR-36), the session log
 * (UAM-NFR-35) and the direct role-edit guard (PQ17).
 */
@IntegrationTest
class UserAccessExtensionsIT {

  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 1_000_000_000L);
  private static final String PASSWORD = "Str0ng!Passw0rd";
  private static final String ADMIN = "admin";

  @Autowired private UserAdminService admin;
  @Autowired private UserDirectory directory;
  @Autowired private UserDetailsService userDetails;
  @Autowired private AccessChangeLogRepository changeLog;
  @Autowired private UserSessionLog sessions;
  @Autowired private JwtTokenService tokens;
  @Autowired private SystemParameterService parameters;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MockMvc mvc;
  @Autowired private Api api;
  @Autowired private AsUser as;

  private static String unique(String prefix) {
    return prefix + IDS.incrementAndGet();
  }

  private Set<String> authorities(String user) {
    return userDetails.loadUserByUsername(user).getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }

  private List<AccessChangeLog> changes(AccessSubjectType type, String subject) {
    return changeLog.findBySubjectTypeAndSubjectIgnoreCaseOrderByIdDesc(type, subject);
  }

  private Role role(String code) {
    return as.run(
        ADMIN,
        () ->
            admin.createRole(
                new RoleRequest(
                    code,
                    "Access test",
                    Set.of(Permission.UAM_REPORT_VIEW, Permission.REPORT_VIEW),
                    "Test group profile",
                    PrivilegeLevel.HIGH),
                ChangeAuthority.request("UAM-T-" + code, "uamapprover")));
  }

  private AppUser user(String username, String roleCode, String windowsId) {
    return as.run(
        ADMIN,
        () ->
            admin.createUser(
                new UserRequest(
                    username,
                    "Access Tester",
                    null,
                    null,
                    null,
                    Set.of(roleCode),
                    true,
                    windowsId,
                    "BU-TEST",
                    "OFFICER"),
                PASSWORD,
                ChangeAuthority.request("UAM-U-" + username, "uamapprover")));
  }

  @Test
  void aDeactivatedRoleGrantsNothingUntilReactivated() {
    String code = unique("UAMT_");
    String username = unique("uamt");
    Role role = role(code);
    user(username, code, null);
    assertThat(authorities(username)).contains("UAM_REPORT_VIEW");
    assertThat(directory.roleCodes(username)).containsExactly(code);
    assertThat(directory.usersWithPermission("UAM_REPORT_VIEW")).contains(username);

    as.run(
        ADMIN,
        () ->
            admin.deactivateRole(role.getId(), ChangeAuthority.request("UAM-D-1", "secapprover")));
    assertThat(authorities(username)).doesNotContain("UAM_REPORT_VIEW", "REPORT_VIEW");
    assertThat(directory.roleCodes(username)).isEmpty();
    assertThat(directory.usersWithPermission("UAM_REPORT_VIEW")).doesNotContain(username);
    assertThat(admin.getByUsername(username).effectivePermissions()).isEmpty();
    assertThat(admin.getRole(role.getId()).isActive()).isFalse();
    assertThat(admin.getRole(role.getId()).getDeactivatedBy()).isEqualTo(ADMIN);
    assertThatThrownBy(
            () -> as.run(ADMIN, () -> admin.deactivateRole(role.getId(), ChangeAuthority.DIRECT)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("already inactive");

    as.run(ADMIN, () -> admin.reactivateRole(role.getId(), ChangeAuthority.DIRECT));
    assertThat(authorities(username)).contains("UAM_REPORT_VIEW", "REPORT_VIEW");
    assertThatThrownBy(
            () -> as.run(ADMIN, () -> admin.reactivateRole(role.getId(), ChangeAuthority.DIRECT)))
        .isInstanceOf(BusinessRuleException.class);

    assertThat(changes(AccessSubjectType.ROLE, code))
        .extracting(AccessChangeLog::getActivity)
        .contains(
            AccessChangeActivity.CREATE_ROLE,
            AccessChangeActivity.DEACTIVATE_ROLE,
            AccessChangeActivity.REACTIVATE_ROLE);
    assertThat(changes(AccessSubjectType.ROLE, code))
        .filteredOn(c -> c.getActivity() == AccessChangeActivity.DEACTIVATE_ROLE)
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.getRequestNo()).isEqualTo("UAM-D-1");
              assertThat(c.getApprovedBy()).isEqualTo("secapprover");
              assertThat(c.getDoneBy()).isEqualTo(ADMIN);
              assertThat(c.getFromValue()).isEqualTo("true");
              assertThat(c.getToValue()).isEqualTo("false");
            });
  }

  @Test
  void everyChangedUserAttributeIsLoggedWithItsRequest() {
    String code = unique("UAMT_");
    String username = unique("uamt");
    String windowsId = unique("BDO\\W");
    role(code);
    AppUser created = user(username, code, windowsId);
    assertThat(created.getWindowsId()).isEqualTo(windowsId);
    assertThat(created.isMustChangePassword()).isTrue();
    assertThat(changes(AccessSubjectType.USER, username))
        .allSatisfy(
            c -> {
              assertThat(c.getActivity()).isEqualTo(AccessChangeActivity.CREATE_USER);
              assertThat(c.getRequestNo()).isEqualTo("UAM-U-" + username);
              assertThat(c.getApprovedBy()).isEqualTo("uamapprover");
            })
        .extracting(AccessChangeLog::getAttribute)
        .contains("fullName", "roles", "windowsId", "businessUnitCode", "userLevel", "enabled");

    as.run(
        ADMIN,
        () ->
            admin.updateUser(
                created.getId(),
                new UserRequest(
                    username,
                    "Access Tester",
                    "tester@bdoi.test",
                    null,
                    null,
                    Set.of(code, "READ_ONLY"),
                    false)));
    AppUser updated = admin.getByUsername(username);
    assertThat(updated.getWindowsId()).as("kept by the 7-argument form").isEqualTo(windowsId);
    List<AccessChangeLog> direct =
        changes(AccessSubjectType.USER, username).stream()
            .filter(c -> c.getRequestNo() == null)
            .toList();
    assertThat(direct)
        .extracting(AccessChangeLog::getAttribute, AccessChangeLog::getActivity)
        .containsExactlyInAnyOrder(
            tuple("email", AccessChangeActivity.MODIFY_USER),
            tuple("roles", AccessChangeActivity.ROLES_CHANGED),
            tuple("enabled", AccessChangeActivity.DISABLE_USER));

    String other = unique("uamt");
    assertThatThrownBy(() -> user(other, code, windowsId.toLowerCase(Locale.ROOT)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("already used");
  }

  @Test
  void passwordResetForcesAChangeAndKeepsTheHistory() {
    String code = unique("UAMT_");
    String username = unique("uamt");
    role(code);
    AppUser created = user(username, code, null);
    as.run(
        ADMIN,
        () -> {
          admin.resetPassword(created.getId(), "An0ther!Passw0rd");
          return null;
        });
    AppUser reset = admin.getByUsername(username);
    assertThat(reset.isMustChangePassword()).isTrue();
    assertThat(reset.getPasswordChangedAt()).isNotNull();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_password_history where username = ?",
                Integer.class,
                username))
        .isEqualTo(2);
    assertThat(changes(AccessSubjectType.USER, username))
        .extracting(AccessChangeLog::getActivity)
        .contains(AccessChangeActivity.PASSWORD_RESET);

    as.run(
        username,
        () -> {
          admin.changeOwnPassword("An0ther!Passw0rd", "Th1rd!Passw0rd");
          return null;
        });
    assertThat(admin.getByUsername(username).isMustChangePassword()).isFalse();
  }

  @Test
  void theAccessChangeLogIsInsertOnly() {
    String code = unique("UAMT_");
    role(code);
    Long id = changes(AccessSubjectType.ROLE, code).get(0).getId();
    assertThatThrownBy(
            () -> jdbc.update("update sec_access_change_log set to_value = 'x' where id = ?", id))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(() -> jdbc.update("delete from sec_access_change_log where id = ?", id))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void signInOpensASessionThatSignOutEnds() throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"auditor\",\"password\":\"Brokerverse@2026\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(body, "$.accessToken");
    String sessionId = tokens.parse(token).orElseThrow().tokenId();
    assertThat(sessions.sessionsOf("auditor"))
        .filteredOn(s -> s.getSessionId().equals(sessionId))
        .singleElement()
        .satisfies(s -> assertThat(s.getEndedAt()).isNull());
    assertThat(sessions.isOnline("auditor")).isTrue();
    assertThat(sessions.touch(sessionId)).as("throttled").isFalse();

    mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());
    UserSession ended =
        sessions.sessionsOf("auditor").stream()
            .filter(s -> s.getSessionId().equals(sessionId))
            .findFirst()
            .orElseThrow();
    assertThat(ended.getEndReason()).isEqualTo(SessionEndReason.LOGOUT);
    assertThat(ended.isOpen(ended.getEndedAt())).isFalse();
    assertThat(admin.getByUsername("auditor").getLastLogoutAt()).isNotNull();
    assertThat(sessions.end(sessionId, SessionEndReason.ADMIN_ENDED)).isEmpty();
    assertThat(sessions.touch(null)).isFalse();
  }

  @Test
  void roleEditsNeedAnApprovedRequestOrTheEmergencyPath() throws Exception {
    // V1062 closes the emergency path (UAM_DIRECT_ROLE_EDIT = false); it is opened for this test.
    as.run(ADMIN, () -> parameters.update(RoleEditGuard.DIRECT_EDIT_PARAMETER, "true"));
    try {
      emergencyPathIsAuditedAndGuarded();
    } finally {
      as.run(ADMIN, () -> parameters.update(RoleEditGuard.DIRECT_EDIT_PARAMETER, "false"));
    }
  }

  private void emergencyPathIsAuditedAndGuarded() throws Exception {
    String code = unique("UAMT_");
    long id =
        api.read(
                api.doPost(
                        ADMIN,
                        "/api/v1/admin/roles",
                        Json.of(
                            "code",
                            code,
                            "name",
                            "Direct",
                            "permissions",
                            List.of("REPORT_VIEW"),
                            "privilegeLevel",
                            "LOW"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.privilegeLevel").value("LOW")))
            .get("id")
            .asLong();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where dedup_key = ? and status <> 'RESOLVED'",
                Integer.class,
                "UAM_DIRECT_ROLE_EDIT:" + code))
        .isEqualTo(1);
    api.doPost(
            ADMIN,
            "/api/v1/admin/roles?requestNo=UAM-NONE-1",
            Json.of("code", unique("UAMT_"), "name", "Req", "permissions", List.of()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ROLE_REQUEST_NOT_APPROVED"));

    as.run(ADMIN, () -> parameters.update(RoleEditGuard.DIRECT_EDIT_PARAMETER, "false"));
    api.doPut(
            ADMIN,
            "/api/v1/admin/roles/" + id,
            Json.of("code", code, "name", "Changed", "permissions", List.of()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ROLE_EDIT_BY_REQUEST"));
    api.doPost(
            ADMIN,
            "/api/v1/admin/roles",
            Json.of("code", unique("UAMT_"), "name", "Blocked", "permissions", List.of()))
        .andExpect(jsonPath("$.code").value("ROLE_EDIT_BY_REQUEST"));
    as.run(ADMIN, () -> parameters.update(RoleEditGuard.DIRECT_EDIT_PARAMETER, "true"));
    api.doPut(
            ADMIN,
            "/api/v1/admin/roles/" + id,
            Json.of("code", code, "name", "Changed", "permissions", List.of("AUDIT_VIEW")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Changed"));
    assertThat(changes(AccessSubjectType.ROLE, code))
        .extracting(AccessChangeLog::getAttribute)
        .contains("name", "permissions");
  }
}
