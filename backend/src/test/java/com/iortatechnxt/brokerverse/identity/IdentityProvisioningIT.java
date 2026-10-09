package com.iortatechnxt.brokerverse.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * BDOI FRS FRUM.002.01 to FRUM.003.03: the provisioning interface of UIDM-ISC (SCIM 2.0) creates
 * the user of a joiner with the UIDM request number, refuses a Windows ID held by another user and
 * alerts the System Administrators, updates a mover and keeps the group profiles, deactivates a
 * leaver and ends the open sessions, reactivates a rehire with the group profiles held before; the
 * monitoring reprocesses a refused event; a user is created from an active Enterprise SSO account
 * of the simulator and synchronised on demand.
 */
@IntegrationTest
class IdentityProvisioningIT {

  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 100_000_000L);
  private static final String USERS = "/integration/v1/scim/v2/Users";
  private static final String SCIM = "application/scim+json";
  private static final String PASSWORD = "Initial!Passw0rd";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Api api;
  @Autowired private AsUser as;
  @Autowired private UserAdminService admin;
  @Autowired private SystemParameterService parameters;

  private static String userId() {
    return "a" + String.format("%09d", 900_000_000L + IDS.incrementAndGet() % 99_999_999L);
  }

  private static MockHttpServletRequestBuilder uidm(MockHttpServletRequestBuilder request) {
    return request.with(
        jwt()
            .jwt(j -> j.subject("uidm-isc"))
            .authorities(new SimpleGrantedAuthority("SCOPE_identity.provision")));
  }

  private Map<String, Object> scimUser(String userId, String windowsId, String email) {
    return Map.of(
        "schemas",
        List.of("urn:ietf:params:scim:schemas:core:2.0:User"),
        "userName",
        userId,
        "name",
        Map.of("givenName", "Juan", "familyName", "Dela Cruz"),
        "displayName",
        "Juan Dela Cruz",
        "emails",
        List.of(Map.of("value", email, "primary", true)),
        "active",
        true,
        "urn:bibs:params:scim:schemas:extension:bdo:2.0:User",
        Map.of(
            "windowsId", windowsId,
            "adGroup", "BIBS Users",
            "unitSegment", "Retail Marketing",
            "location", "Makati",
            "uidmRequestNo", "UIDM-2026-" + userId.substring(4)));
  }

  private String joiner(String userId, String windowsId) throws Exception {
    mvc.perform(
            uidm(post(USERS))
                .contentType(SCIM)
                .content(
                    json.writeValueAsString(scimUser(userId, windowsId, userId + "@bdo.test"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userName").value(userId));
    return userId;
  }

  @Test
  void aJoinerCreatesTheUserWithTheUidmRequestNumberAndTheTimeOfTheEvent() throws Exception {
    String userId = userId();
    String windowsId = "BDO\\j" + userId;
    joiner(userId, windowsId);
    Map<String, Object> user =
        jdbc.queryForMap(
            "select enabled, windows_id, email from sec_user where username = ?", userId);
    assertThat(user.get("enabled")).isEqualTo(true);
    assertThat(user.get("windows_id")).isEqualTo(windowsId);
    api.doGet("admin", "/api/v1/admin/identity/users/" + userId + "/profile")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.uidmRequestNo").value("UIDM-2026-" + userId.substring(4)))
        .andExpect(jsonPath("$.lastEventType").value("JOINER"))
        .andExpect(jsonPath("$.lastEventSource").value("UIDM-ISC"))
        .andExpect(jsonPath("$.lastEventAt").isNotEmpty())
        .andExpect(jsonPath("$.unitSegment").value("Retail Marketing"))
        .andExpect(jsonPath("$.syncStatus").value("SYNCED"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_access_change_log where subject = ?"
                    + " and activity = 'CREATE_USER' and done_by = 'UIDM-ISC' and request_no = ?",
                Integer.class,
                userId,
                "UIDM-2026-" + userId.substring(4)))
        .isEqualTo(4);
  }

  @Test
  void aJoinerWithAWindowsIdHeldByAnotherUserIsRefusedLoggedAndAlerted() throws Exception {
    String userId = userId();
    int notices = notices();
    mvc.perform(
            uidm(post(USERS))
                .contentType(SCIM)
                .content(
                    json.writeValueAsString(scimUser(userId, "BDO\\sit.leaver", "x@bdo.test"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.scimType").value("uniqueness"));
    assertThat(jdbc.queryForList("select 1 from sec_user where username = ?", userId)).isEmpty();
    Map<String, Object> event =
        jdbc.queryForMap(
            "select id, status, message from idn_identity_event where user_id = ?", userId);
    assertThat(event.get("status")).isEqualTo("REFUSED");
    assertThat(event.get("message"))
        .isEqualTo("Windows ID BDO\\sit.leaver belongs to another user");
    assertThat(notices()).isGreaterThan(notices);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where exception_code = 'UAM_IDENTITY_REFUSED'"
                    + " and entity_id = ?",
                Integer.class,
                String.valueOf(event.get("id"))))
        .isEqualTo(1);
  }

  @Test
  void aMoverChangesTheDetailsAndKeepsTheGroupProfiles() throws Exception {
    String userId = userId();
    String windowsId = "BDO\\m" + userId;
    joiner(userId, windowsId);
    jdbc.update(
        "insert into sec_user_role (user_id, role_id) select u.id, r.id from sec_user u, sec_role r"
            + " where u.username = ? and r.code = 'AUDITOR'",
        userId);
    mvc.perform(
            uidm(put(USERS + "/" + userId))
                .contentType(SCIM)
                .content(
                    json.writeValueAsString(
                        scimUser(userId, windowsId, "moved" + userId + "@bdo.test"))))
        .andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject(
                "select email from sec_user where username = ?", String.class, userId))
        .isEqualTo("moved" + userId + "@bdo.test");
    assertThat(roles(userId)).containsExactly("AUDITOR");
  }

  @Test
  void aLeaverEndsTheOpenSessionAndARehireRestoresTheGroupProfiles() throws Exception {
    String userId = userId();
    String windowsId = "BDO\\l" + userId;
    as.run(
        "admin",
        () ->
            admin.createUser(
                new UserRequest(userId, "Leaver Tester", null, null, null, Set.of("AUDITOR"), true),
                PASSWORD));
    jdbc.update("update sec_user set windows_id = ? where username = ?", windowsId, userId);
    String login =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(Map.of("username", userId, "password", PASSWORD))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(login, "$.accessToken");

    mvc.perform(uidm(delete(USERS + "/" + userId))).andExpect(status().isNoContent());
    TestSecurityContextHolder.clearContext();
    mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
    assertThat(enabled(userId)).isFalse();

    mvc.perform(
            uidm(patch(USERS + "/" + userId))
                .contentType(SCIM)
                .content(
                    "{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                        + "\"Operations\":[{\"op\":\"replace\",\"path\":\"active\",\"value\":true}]}"))
        .andExpect(status().isOk());
    assertThat(enabled(userId)).isTrue();
    assertThat(roles(userId)).containsExactly("AUDITOR");

    mvc.perform(
            uidm(patch(USERS + "/" + userId))
                .contentType(SCIM)
                .content(
                    "{\"Operations\":[{\"op\":\"replace\",\"path\":"
                        + "\"urn:bibs:params:scim:schemas:extension:bdo:2.0:User:adStatus\","
                        + "\"value\":\"LOCKED\"}]}"))
        .andExpect(status().isOk());
    assertThat(enabled(userId)).isFalse();
  }

  @Test
  void aRefusedEventIsReprocessedOnceTheDataIsCorrected() throws Exception {
    String userId = userId();
    String taken = "BDO\\r" + userId;
    String holder = userId();
    joiner(holder, taken);
    mvc.perform(
            uidm(post(USERS))
                .contentType(SCIM)
                .content(json.writeValueAsString(scimUser(userId, taken, userId + "@bdo.test"))))
        .andExpect(status().isConflict());
    Long id =
        jdbc.queryForObject(
            "select id from idn_identity_event where user_id = ? and status = 'REFUSED'",
            Long.class,
            userId);
    api.doGet("admin", "/api/v1/admin/identity/events?status=REFUSED&windowsId=" + userId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].reprocessable").value(true));
    jdbc.update("update sec_user set windows_id = ? where username = ?", taken + ".old", holder);
    api.doPost("admin", "/api/v1/admin/identity/events/" + id + "/reprocess", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPLIED"))
        .andExpect(jsonPath("$.attempts").value(2))
        .andExpect(jsonPath("$.processedBy").value("admin"));
  }

  @Test
  void aUserIsCreatedFromAnActiveEnterpriseSsoAccountAndSynchronisedOnDemand() throws Exception {
    jdbc.update(
        "update idn_sim_directory_account set user_id = ?, windows_id = ? where windows_id = ?",
        "a013000301",
        "BDO\\bsantos",
        "BDO\\bsantos");
    api.doGet("admin", "/api/v1/admin/identity/directory?windowsId=BDO\\bsantos")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Bea Santos"));
    if (jdbc.queryForList("select 1 from sec_user where username = 'a013000301'").isEmpty()) {
      api.doPost(
              "admin",
              "/api/v1/admin/identity/users/from-directory",
              Map.of("windowsId", "BDO\\bsantos"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("APPLIED"))
          .andExpect(jsonPath("$.username").value("a013000301"));
    }
    api.doGet("admin", "/api/v1/admin/identity/users/a013000301/profile")
        .andExpect(jsonPath("$.department").value("Marketing"))
        .andExpect(jsonPath("$.teamLeaderName").value("Teresa Lopez"));
    api.doPost(
            "admin",
            "/api/v1/admin/identity/users/from-directory",
            Map.of("windowsId", "BDO\\dlim"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.detail")
                .value("The user cannot be created from this Enterprise SSO account"));
    api.doPost("admin", "/api/v1/admin/identity/users/a013000101/sync", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source").value("ON_DEMAND"));
  }

  @Test
  void theExtractListsTheAccountsWithTheirGroupProfilesAndTheGroupsWithTheirPermissions()
      throws Exception {
    mvc.perform(uidm(get(USERS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalResults").isNumber());
    mvc.perform(uidm(get("/integration/v1/scim/v2/Groups")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.Resources[?(@.id == 'AUDITOR')].permissions").isNotEmpty());
    mvc.perform(get(USERS)).andExpect(status().isUnauthorized());
  }

  @Test
  void withProvisioningOffTheEventIsRefused() throws Exception {
    as.run("admin", () -> parameters.update("UAM_SSO_PROVISIONING", "false"));
    try {
      String userId = userId();
      mvc.perform(
              uidm(post(USERS))
                  .contentType(SCIM)
                  .content(
                      json.writeValueAsString(
                          scimUser(userId, "BDO\\off" + userId, userId + "@bdo.test"))))
          .andExpect(status().isBadRequest());
      assertThat(jdbc.queryForList("select 1 from sec_user where username = ?", userId)).isEmpty();
    } finally {
      as.run("admin", () -> parameters.update("UAM_SSO_PROVISIONING", "true"));
    }
  }

  private boolean enabled(String userId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select enabled from sec_user where username = ?", Boolean.class, userId));
  }

  private List<String> roles(String userId) {
    return jdbc.queryForList(
        "select r.code from sec_user_role ur join sec_user u on u.id = ur.user_id"
            + " join sec_role r on r.id = ur.role_id where u.username = ? order by r.code",
        String.class,
        userId);
  }

  private int notices() {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from msg_notification where title like 'Identity event % needs review'",
            Integer.class);
    return n == null ? 0 : n;
  }
}
