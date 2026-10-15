package com.iortatechnxt.brokerverse.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleKind;
import com.iortatechnxt.brokerverse.nbadmin.service.SodRuleService;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * UIDM-ISC orchestrates the membership of the group profiles (decision option A): {@code PATCH
 * /Groups/{id}} adds, removes and replaces the members of a group profile, and the {@code groups}
 * of a User sent on a create or a replace become the user's group profiles; each change applies at
 * once, is written to the access change log with its from and to values, the UIDM request number
 * and UIDM-ISC as the actor, and to the identity events with its outcome. A combination a
 * separation-of-duties rule forbids is refused (400 invalidValue) and alerted to the System
 * Administrators; an unknown group profile answers 404; the simulator sends the same events.
 */
@IntegrationTest
class ScimGroupMembershipIT {

  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 100_000_000L);
  private static final String SCIM_BASE = "/integration/v1/scim/v2";
  private static final String USERS = SCIM_BASE + "/Users";
  private static final String GROUPS = SCIM_BASE + "/Groups/";
  private static final String SCIM = "application/scim+json";
  private static final String PATCH_OP = "urn:ietf:params:scim:api:messages:2.0:PatchOp";
  private static final String BIBS = "urn:bibs:params:scim:schemas:extension:bdo:2.0:User";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Api api;
  @Autowired private AsUser as;
  @Autowired private UserAdminService admin;
  @Autowired private SodRuleService sodRules;

  private static String userId() {
    return "a" + String.format("%09d", 800_000_000L + IDS.incrementAndGet() % 99_999_999L);
  }

  private static MockHttpServletRequestBuilder uidm(MockHttpServletRequestBuilder request) {
    return request
        .contentType(SCIM)
        .with(
            jwt()
                .jwt(j -> j.subject("uidm-isc"))
                .authorities(new SimpleGrantedAuthority("SCOPE_identity.provision")));
  }

  private String profile() {
    String code = "GM_" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
    as.run(
        "admin",
        () ->
            admin.createRole(
                new RoleRequest(code, "Profile " + code, Set.of(Permission.REPORT_VIEW))));
    return code;
  }

  private Map<String, Object> scimUser(String userId, String requestNo, List<String> groups) {
    Map<String, Object> user = new HashMap<>();
    user.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:User"));
    user.put("userName", userId);
    user.put("name", Map.of("givenName", "Maria", "familyName", "Reyes"));
    user.put("emails", List.of(Map.of("value", userId + "@bdo.test", "primary", true)));
    user.put("active", true);
    user.put(BIBS, Map.of("windowsId", "BDO\\g" + userId, "uidmRequestNo", requestNo));
    if (groups != null) {
      user.put("groups", groups.stream().map(g -> Map.of("value", g)).toList());
    }
    return user;
  }

  private String joiner() throws Exception {
    String userId = userId();
    mvc.perform(
            uidm(post(USERS))
                .content(json.writeValueAsString(scimUser(userId, "UIDM-J-" + userId, null))))
        .andExpect(status().isCreated());
    return userId;
  }

  private ResultActions patchGroup(String group, String requestNo, Map<String, Object> op)
      throws Exception {
    MockHttpServletRequestBuilder request =
        uidm(patch(GROUPS + group))
            .content(
                json.writeValueAsString(
                    Map.of("schemas", List.of(PATCH_OP), "Operations", List.of(op))));
    if (requestNo != null) {
      request.header("X-UIDM-Request-No", requestNo);
    }
    return mvc.perform(request);
  }

  private static Map<String, Object> op(String op, String path, String... members) {
    Map<String, Object> o = new HashMap<>();
    o.put("op", op);
    o.put("path", path);
    if (members.length > 0) {
      o.put("value", List.of(members).stream().map(m -> Map.of("value", m)).toList());
    }
    return o;
  }

  @Test
  void membersAreAddedRemovedAndReplacedAtOnceAndLogged() throws Exception {
    String group = profile();
    String first = joiner();
    String second = joiner();

    patchGroup(group, "UIDM-2026-0001", op("add", "members", first, second))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(group))
        .andExpect(jsonPath("$.members[*].value").value(hasItems(first, second)));
    assertThat(roles(first)).containsExactly(group);
    assertThat(roles(second)).containsExactly(group);
    Map<String, Object> change =
        jdbc.queryForMap(
            "select from_value, to_value, done_by, request_no, approved_by"
                + " from sec_access_change_log where subject = ? and activity = 'ROLES_CHANGED'",
            first);
    assertThat(change.get("from_value")).isNull();
    assertThat(change.get("to_value")).isEqualTo(group);
    assertThat(change.get("done_by")).isEqualTo("UIDM-ISC");
    assertThat(change.get("request_no")).isEqualTo("UIDM-2026-0001");
    assertThat(change.get("approved_by")).isNull();
    Map<String, Object> event =
        jdbc.queryForMap(
            "select id, source, status, uidm_request_no, message from idn_identity_event"
                + " where user_id = ? and event_type = 'MEMBERSHIP'",
            first);
    assertThat(event.get("source")).isEqualTo("UIDM_ISC");
    assertThat(event.get("status")).isEqualTo("APPLIED");
    assertThat(event.get("uidm_request_no")).isEqualTo("UIDM-2026-0001");
    assertThat((String) event.get("message")).contains("changed from (none) to " + group);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_log where entity_type = 'IdentityEvent'"
                    + " and entity_id = ? and username = 'UIDM-ISC'",
                Integer.class,
                String.valueOf(event.get("id"))))
        .isEqualTo(1);

    // the same change again changes nothing and records no event
    patchGroup(group, null, op("add", "members", first)).andExpect(status().isOk());
    assertThat(events(first)).isEqualTo(1);

    patchGroup(group, "UIDM-2026-0002", op("remove", "members[value eq \"" + first + "\"]"))
        .andExpect(status().isOk());
    assertThat(roles(first)).isEmpty();
    assertThat(roles(second)).containsExactly(group);
    assertThat(
            jdbc.queryForObject(
                "select from_value from sec_access_change_log where subject = ?"
                    + " and activity = 'ROLES_CHANGED' and request_no = 'UIDM-2026-0002'",
                String.class,
                first))
        .isEqualTo(group);

    patchGroup(group, "UIDM-2026-0003", op("replace", "members", first))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members.length()").value(1))
        .andExpect(jsonPath("$.members[0].value").value(first));
    assertThat(roles(first)).containsExactly(group);
    assertThat(roles(second)).isEmpty();

    patchGroup(group, null, op("remove", "members")).andExpect(status().isOk());
    assertThat(roles(first)).isEmpty();
    mvc.perform(uidm(get(GROUPS + group)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members").isEmpty());
  }

  @Test
  void theGroupsOfAUserReplaceItsGroupProfilesOnCreateAndReplace() throws Exception {
    String a = profile();
    String b = profile();
    String userId = userId();
    mvc.perform(
            uidm(post(USERS))
                .content(json.writeValueAsString(scimUser(userId, "UIDM-C-" + userId, List.of(a)))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.groups[0].value").value(a));
    assertThat(roles(userId)).containsExactly(a);

    mvc.perform(
            uidm(put(USERS + "/" + userId))
                .content(
                    json.writeValueAsString(scimUser(userId, "UIDM-P-" + userId, List.of(a, b)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groups.length()").value(2));
    assertThat(roles(userId)).containsExactlyInAnyOrder(a, b);
    assertThat(
            jdbc.queryForObject(
                "select done_by from sec_access_change_log where subject = ?"
                    + " and activity = 'ROLES_CHANGED' and request_no = ?",
                String.class,
                userId,
                "UIDM-P-" + userId))
        .isEqualTo("UIDM-ISC");

    // without groups the group profiles stay
    mvc.perform(
            uidm(put(USERS + "/" + userId))
                .content(json.writeValueAsString(scimUser(userId, "UIDM-Q-" + userId, null))))
        .andExpect(status().isOk());
    assertThat(roles(userId)).containsExactlyInAnyOrder(a, b);

    mvc.perform(
            uidm(put(USERS + "/" + userId))
                .content(json.writeValueAsString(scimUser(userId, "UIDM-R-" + userId, List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groups").isEmpty());
    assertThat(roles(userId)).isEmpty();
  }

  @Test
  void aSeparationOfDutiesConflictIsRefusedAlertedAndChangesNobody() throws Exception {
    String a = profile();
    String b = profile();
    SodRule rule = as.run("badmin", () -> sodRules.create(a, b, "Maker and checker"));
    as.run("infosec", () -> sodRules.authorize(rule.getId()));
    try {
      String userId = joiner();
      String other = joiner();
      patchGroup(a, "UIDM-S-1", op("add", "members", userId)).andExpect(status().isOk());

      patchGroup(b, "UIDM-S-2", op("add", "members", other, userId))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.scimType").value("invalidValue"))
          .andExpect(jsonPath("$.detail").value(containsString(rule.getRuleCode())));
      assertThat(roles(userId)).containsExactly(a);
      assertThat(roles(other)).isEmpty();
      Map<String, Object> event =
          jdbc.queryForMap(
              "select id, status, uidm_request_no from idn_identity_event"
                  + " where user_id = ? and event_type = 'MEMBERSHIP' and status = 'REFUSED'",
              userId);
      assertThat(event.get("uidm_request_no")).isEqualTo("UIDM-S-2");
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from alt_alert where exception_code = 'UAM_IDENTITY_REFUSED'"
                      + " and entity_id = ?",
                  Integer.class,
                  String.valueOf(event.get("id"))))
          .isEqualTo(1);

      mvc.perform(
              uidm(put(USERS + "/" + other))
                  .content(json.writeValueAsString(scimUser(other, "UIDM-S-3", List.of(a, b)))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.scimType").value("invalidValue"));
      assertThat(roles(other)).isEmpty();
    } finally {
      as.run("badmin", () -> sodRules.requestDeactivation(rule.getId()));
      as.run("infosec", () -> sodRules.authorize(rule.getId()));
    }
  }

  @Test
  void conflictingPermissionsThroughTwoGroupProfilesAreRefused() throws Exception {
    String requester = "GM_R" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    String approver = "GM_A" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    as.run(
        "admin",
        () -> {
          admin.createRole(new RoleRequest(requester, "Requester", Set.of(Permission.UAM_ENROLL)));
          return admin.createRole(
              new RoleRequest(approver, "Approver", Set.of(Permission.UAM_SOD_AUTHORIZE)));
        });
    SodRule rule =
        as.run(
            "badmin",
            () ->
                sodRules.create(
                    SodRuleKind.PERMISSIONS,
                    "UAM_ENROLL",
                    "UAM_SOD_AUTHORIZE",
                    "Enrol or authorise"));
    as.run("infosec", () -> sodRules.authorize(rule.getId()));
    try {
      String userId = joiner();
      mvc.perform(
              uidm(put(USERS + "/" + userId))
                  .content(
                      json.writeValueAsString(
                          scimUser(userId, "UIDM-X-" + userId, List.of(requester, approver)))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.scimType").value("invalidValue"));
      assertThat(roles(userId)).isEmpty();
    } finally {
      as.run("badmin", () -> sodRules.requestDeactivation(rule.getId()));
      as.run("infosec", () -> sodRules.authorize(rule.getId()));
    }
  }

  @Test
  void anUnknownGroupAnswersNotFoundAndAnUnknownMemberIsRefused() throws Exception {
    String userId = joiner();
    patchGroup("NO_SUCH_GROUP", null, op("add", "members", userId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.schemas[0]").value("urn:ietf:params:scim:api:messages:2.0:Error"));
    mvc.perform(uidm(get(GROUPS + "NO_SUCH_GROUP"))).andExpect(status().isNotFound());
    mvc.perform(
            uidm(put(USERS + "/" + userId))
                .content(
                    json.writeValueAsString(
                        scimUser(userId, "UIDM-U-" + userId, List.of("NO_SUCH_GROUP")))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value(containsString("NO_SUCH_GROUP")));
    assertThat(roles(userId)).isEmpty();

    String group = profile();
    patchGroup(group, null, op("add", "members", "z999999999"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.scimType").value("invalidValue"));
    patchGroup(group, null, op("replace", "displayName"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.scimType").value("invalidPath"));
    // the integration security rule: only a caller with the provisioning scope changes members
    mvc.perform(
            patch(GROUPS + group)
                .with(jwt().jwt(j -> j.subject("other-system")))
                .contentType(SCIM)
                .content(json.writeValueAsString(Map.of("Operations", List.of()))))
        .andExpect(status().isForbidden());
  }

  @Test
  void theSimulatorSendsTheGroupProfilesOfAnAccount() throws Exception {
    String group = profile();
    String userId = userId();
    String windowsId = "BDO\\sim" + userId;
    String simulator = "/api/v1/admin/identity/simulator/accounts";
    api.doPost(
            "admin",
            simulator,
            Json.of(
                "windowsId",
                windowsId,
                "userId",
                userId,
                "email",
                userId + "@bdo.test",
                "firstName",
                "Sim",
                "lastName",
                "Member",
                "status",
                "ACTIVE"))
        .andExpect(status().isCreated());
    api.doPost("admin", simulator + "/events/JOINER?windowsId=" + windowsId, null)
        .andExpect(jsonPath("$.status").value("APPLIED"));
    api.doPost(
            "admin",
            simulator + "/events/MEMBERSHIP?windowsId=" + windowsId,
            Json.of("groups", List.of(group), "uidmRequestNo", "UIDM-SIM-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventType").value("MEMBERSHIP"))
        .andExpect(jsonPath("$.status").value("APPLIED"))
        .andExpect(jsonPath("$.uidmRequestNo").value("UIDM-SIM-1"));
    assertThat(roles(userId)).containsExactly(group);
    api.doPost(
            "admin",
            simulator + "/events/MEMBERSHIP?windowsId=" + windowsId,
            Json.of("groups", List.of("NO_SUCH_GROUP")))
        .andExpect(jsonPath("$.status").value("REFUSED"));
    assertThat(roles(userId)).containsExactly(group);
  }

  private int events(String userId) {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from idn_identity_event where user_id = ? and event_type = 'MEMBERSHIP'",
            Integer.class,
            userId);
    return n == null ? 0 : n;
  }

  private List<String> roles(String userId) {
    return jdbc.queryForList(
        "select r.code from sec_user_role ur join sec_user u on u.id = ur.user_id"
            + " join sec_role r on r.id = ur.role_id where u.username = ? order by r.code",
        String.class,
        userId);
  }
}
