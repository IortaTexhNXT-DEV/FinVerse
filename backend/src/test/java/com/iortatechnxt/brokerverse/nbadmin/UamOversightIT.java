package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleKind;
import com.iortatechnxt.brokerverse.nbadmin.service.SodRuleService;
import com.iortatechnxt.brokerverse.nbadmin.service.SodViolationJob;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Oversight of User Access under the UIDM-ISC model: the separation-of-duties breaches of the
 * current holders and the notice to Information Security, the certification extract with its log
 * and SHA-256, the identity monitoring (status, dismissal of a refused event, resolution of a
 * mismatch), the user detail (group profiles with their source, access history, end of sessions)
 * and the UAM settings under maker-checker.
 */
@IntegrationTest
class UamOversightIT {

  private static final String INFOSEC = "infosec";
  private static final String UAM_ADMIN = "uamadmin";

  @Autowired private Api api;
  @Autowired private AsUser as;
  @Autowired private UserAdminService users;
  @Autowired private SodRuleService sodRules;
  @Autowired private SodViolationJob violationJob;
  @Autowired private JdbcTemplate jdbc;

  private String newProfile(Permission permission) {
    String code = "OVS_" + Math.floorMod(System.nanoTime(), 1_000_000_000L);
    as.run(
        "admin",
        () -> users.createRole(new RoleRequest(code, "Oversight " + code, Set.of(permission))));
    return code;
  }

  private AppUser holderOf(String... profiles) {
    String name = AccessRequestIT.username();
    AppUser user =
        as.run(
            "admin",
            () ->
                users.createUser(
                    new UserRequest(name, "Oversight User", null, null, null, Set.of(), true),
                    "Oversight#2026pw"));
    for (String profile : profiles) {
      jdbc.update(
          "insert into sec_user_role (user_id, role_id) select ?, id from sec_role where code = ?",
          user.getId(),
          profile);
    }
    return user;
  }

  private int notices(String title) {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from msg_notification where recipient = ? and title like ?",
            Integer.class,
            INFOSEC,
            title + "%");
    return n == null ? 0 : n;
  }

  @Test
  void aBreachOfAnActiveRuleIsListedRecordedOnceAndToldToInformationSecurity() throws Exception {
    String a = newProfile(Permission.REPORT_VIEW);
    String b = newProfile(Permission.AUDIT_VIEW);
    AppUser holder = holderOf(a, b);
    SodRule rule = as.run("badmin", () -> sodRules.create(SodRuleKind.PROFILES, a, b, "Apart"));
    as.run(INFOSEC, () -> sodRules.authorize(rule.getId()));
    try {
      api.doGet(INFOSEC, "/api/v1/nbadmin/sod-rules/violations")
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$[?(@.username == '" + holder.getUsername() + "')].ruleCode")
                  .value(rule.getRuleCode()));
      int before = notices("Separation of duties");
      assertThat(violationJob.execute(LocalDate.now()).itemsProcessed()).isPositive();
      assertThat(notices("Separation of duties")).isGreaterThan(before);
      assertThat(violationJob.execute(LocalDate.now()).itemsProcessed()).isZero();
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from nba_sod_violation where rule_code = ? and username = ?"
                      + " and cleared_at is null",
                  Integer.class,
                  rule.getRuleCode(),
                  holder.getUsername()))
          .isEqualTo(1);
    } finally {
      as.run("badmin", () -> sodRules.requestDeactivation(rule.getId()));
      as.run(INFOSEC, () -> sodRules.authorize(rule.getId()));
    }
    violationJob.execute(LocalDate.now());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from nba_sod_violation where rule_code = ? and cleared_at is null",
                Integer.class,
                rule.getRuleCode()))
        .isZero();
  }

  @Test
  void aCertificationExtractIsLoggedWithItsHashAndToldToInformationSecurity() throws Exception {
    int before = notices("Certification extract taken");
    byte[] csv =
        api.doPost(UAM_ADMIN, "/api/v1/nbadmin/certification/extract", Json.of("format", "CSV"))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(new String(csv, java.nio.charset.StandardCharsets.UTF_8))
        .startsWith("User ID,Windows ID,Name");
    String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(csv));
    JsonNode log = api.read(api.doGet(INFOSEC, "/api/v1/nbadmin/certification/extracts"));
    assertThat(log.get(0).get("sha256").asText()).isEqualTo(sha);
    assertThat(log.get(0).get("takenBy").asText()).isEqualTo(UAM_ADMIN);
    assertThat(log.get(0).get("rows").asInt()).isPositive();
    assertThat(notices("Certification extract taken")).isGreaterThan(before);
    api.doPost(INFOSEC, "/api/v1/nbadmin/certification/extract", Json.of("format", "XLSX"))
        .andExpect(status().isOk());
    api.doPost(INFOSEC, "/api/v1/nbadmin/certification/extract", Json.of("format", "PDF"))
        .andExpect(status().isBadRequest());
    api.doPost("ao", "/api/v1/nbadmin/certification/extract", Json.of("format", "CSV"))
        .andExpect(status().isForbidden());
  }

  @Test
  void refusedEventsAreDismissedAndMismatchesResolvedWithARemark() throws Exception {
    Long event =
        jdbc.queryForObject(
            "insert into idn_identity_event (received_at, source, event_type, windows_id, user_id,"
                + " payload, status, message, attempts) values (now(), 'UIDM_ISC', 'JOINER',"
                + " 'BDO\\\\ovs', 'a999000001', '{}', 'REFUSED', 'Unknown unit', 1) returning id",
            Long.class);
    Long mismatch =
        jdbc.queryForObject(
            "insert into idn_reconciliation_mismatch (run_date, username, kind, detail, found_at)"
                + " values (current_date, 'a999000001', 'STATUS', 'Active here only', now())"
                + " returning id",
            Long.class);
    api.doGet(UAM_ADMIN, "/api/v1/admin/identity/status")
        .andExpect(jsonPath("$.refusedOpen").isNumber())
        .andExpect(jsonPath("$.mismatchesOpen").isNumber());
    api.doGet(UAM_ADMIN, "/api/v1/admin/identity/events?status=REFUSED&open=true&size=100")
        .andExpect(jsonPath("$.content[?(@.id == " + event + ")]").exists());
    api.doPost(
            UAM_ADMIN,
            "/api/v1/admin/identity/events/" + event + "/dismiss",
            Json.of("remark", " "))
        .andExpect(status().isBadRequest());
    api.doPost(
            UAM_ADMIN,
            "/api/v1/admin/identity/events/" + event + "/dismiss",
            Json.of("remark", "Unit closed; UIDM-ISC informed"))
        .andExpect(status().isNoContent());
    api.doGet(UAM_ADMIN, "/api/v1/admin/identity/events?status=REFUSED&open=true&size=100")
        .andExpect(jsonPath("$.content[?(@.id == " + event + ")]").doesNotExist());
    api.doPost(
            UAM_ADMIN,
            "/api/v1/admin/identity/events/" + event + "/dismiss",
            Json.of("remark", "Again"))
        .andExpect(jsonPath("$.code").value("IDENTITY_ALREADY_DISMISSED"));

    api.doPost(
            UAM_ADMIN,
            "/api/v1/admin/identity/reconciliation/" + mismatch + "/resolve",
            Json.of("remark", "Deactivated by UIDM-ISC"))
        .andExpect(status().isNoContent());
    api.doGet(UAM_ADMIN, "/api/v1/admin/identity/reconciliation")
        .andExpect(jsonPath("$[?(@.id == " + mismatch + ")]").doesNotExist());
    api.doGet(UAM_ADMIN, "/api/v1/admin/identity/reconciliation?open=false")
        .andExpect(
            jsonPath("$[?(@.id == " + mismatch + ")].resolutionRemark")
                .value("Deactivated by UIDM-ISC"));
    api.doPost(UAM_ADMIN, "/api/v1/admin/identity/reconciliation/run", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jobName").value("UAM_IDENTITY_RECONCILIATION"));
  }

  @Test
  void theUserDetailShowsTheGroupProfilesWithTheirSourceAndEndsTheSessions() throws Exception {
    String profile = newProfile(Permission.REPORT_VIEW);
    AppUser user = holderOf();
    jdbc.update(
        "insert into sec_access_change_log (occurred_at, subject_type, subject, activity, attribute,"
            + " from_value, to_value, request_no, done_by) values (?, 'USER', ?, 'ROLES_CHANGED',"
            + " 'roles', null, ?, 'UIDM-OVS-1', 'UIDM-ISC')",
        java.sql.Timestamp.from(Instant.now()),
        user.getUsername(),
        profile);
    jdbc.update(
        "insert into sec_user_role (user_id, role_id) select ?, id from sec_role where code = ?",
        user.getId(),
        profile);
    api.doGet(UAM_ADMIN, "/api/v1/admin/users/" + user.getId() + "/group-profiles")
        .andExpect(jsonPath("$[0].code").value(profile))
        .andExpect(jsonPath("$[0].source").value("UIDM-ISC"))
        .andExpect(jsonPath("$[0].requestNo").value("UIDM-OVS-1"));
    // The effective permissions name the group profile that grants each.
    api.doGet(UAM_ADMIN, "/api/v1/admin/users/" + user.getId() + "/permissions")
        .andExpect(jsonPath("$[?(@.permission == 'REPORT_VIEW')].groupProfiles[0]").exists());
    api.doGet(UAM_ADMIN, "/api/v1/admin/users/" + user.getId() + "/access-history")
        .andExpect(jsonPath("$[?(@.requestNo == 'UIDM-OVS-1')]").exists());
    api.doPost(UAM_ADMIN, "/api/v1/admin/sessions/users/" + user.getUsername() + "/end", null)
        .andExpect(jsonPath("$.ended").value(0));
    api.doGet("ao", "/api/v1/admin/users/" + user.getId() + "/group-profiles")
        .andExpect(status().isForbidden());
  }

  @Test
  void aUamSettingWaitsForAnotherUserAndOtherParametersAreRefused() throws Exception {
    String url = "/api/v1/nbadmin/uam-settings/UAM_RECONCILIATION_INTERVAL_DAYS";
    api.doGet(UAM_ADMIN, "/api/v1/nbadmin/uam-settings")
        .andExpect(jsonPath("$[0].key").value("SESSION_TIMEOUT_MINUTES"))
        .andExpect(jsonPath("$[?(@.key == 'REPORT_FOOTER')]").doesNotExist());
    api.doPut(UAM_ADMIN, url, Json.of("value", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendingValue").value("2"));
    api.doPost(UAM_ADMIN, url + "/approve", null).andExpect(status().isForbidden());
    api.doPost(INFOSEC, url + "/approve", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value("2"));
    api.doPut(UAM_ADMIN, url, Json.of("value", "1")).andExpect(status().isOk());
    api.doPost(INFOSEC, url + "/approve", null).andExpect(jsonPath("$.value").value("1"));
    api.doPut(UAM_ADMIN, "/api/v1/nbadmin/uam-settings/REPORT_FOOTER", Json.of("value", "x"))
        .andExpect(jsonPath("$.code").value("UAM_SETTING_UNKNOWN"));
  }
}
