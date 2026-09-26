package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PersonaMenus;
import com.iortatechnxt.brokerverse.support.PersonaMenus.Persona;
import com.iortatechnxt.brokerverse.support.TestData;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Persona menus of BRD-10 and BRD-11 (client requirement 14) on the server side of the shared file
 * {@code frontend/src/navigation/personaMenus.json}: each role holds exactly the permissions the
 * web client's menu is computed from (V1050, V1060, V1062, V1063), its demo user holds only that
 * role, and every screen the role sees answers its demo user (no menu entry opens on a refusal).
 */
@IntegrationTest
class PersonaMenusIT {

  /** The read the screen makes when it opens ({c} = the demo company). */
  private static final Map<String, String> SCREEN_READS =
      Map.ofEntries(
          Map.entry("/screening", "/api/v1/screening/cases/tiles?companyId={c}"),
          Map.entry("/screening/cases", "/api/v1/screening/cases?companyId={c}"),
          Map.entry("/screening/matches", "/api/v1/screening/matches?companyId={c}"),
          Map.entry("/screening/runs", "/api/v1/screening/runs?companyId={c}"),
          Map.entry("/screening/high-risk", "/api/v1/screening/high-risk-clients?companyId={c}"),
          Map.entry("/screening/str", "/api/v1/screening/str?companyId={c}"),
          Map.entry(
              "/screening-setup/config",
              "/api/v1/screening/config/versions?companyId={c}&type=MATCH_CRITERIA"),
          Map.entry(
              "/screening-setup/templates",
              "/api/v1/screening/config/versions?companyId={c}&type=TEMPLATE"),
          Map.entry("/screening-setup/watchlist", "/api/v1/screening/watchlist/entries"),
          Map.entry("/screening-setup/sources", "/api/v1/screening/watchlist/sources"),
          Map.entry("/user-access/requests", "/api/v1/nbadmin/access-requests"),
          Map.entry(
              "/user-access/group-profiles", "/api/v1/nbadmin/access-requests?groupProfiles=true"),
          Map.entry("/user-access/bulk", "/api/v1/nbadmin/access-batches"),
          Map.entry("/user-access/matrix", "/api/v1/nbadmin/access-matrix"),
          Map.entry("/user-access/reports", "/api/v1/reports"));

  @Autowired private Api api;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TestData data;

  private Set<String> granted(String role) {
    return new TreeSet<>(
        jdbc.queryForList(
            "select p.permission from sec_role_permission p join sec_role r on r.id = p.role_id"
                + " where r.code = ? and r.active",
            String.class,
            role));
  }

  @Test
  void everyRoleHoldsExactlyThePermissionsTheMenuIsBuiltFrom() throws Exception {
    Map<String, Persona> personas = PersonaMenus.load();
    assertThat(personas).hasSize(9);
    for (Persona p : personas.values()) {
      assertThat(granted(p.role())).as(p.role()).isEqualTo(p.permissions());
      JsonNode me = api.read(api.doGet(p.demoUser(), "/api/v1/auth/me"));
      assertThat(me.get("roles").size()).as(p.demoUser()).isEqualTo(1);
      assertThat(me.get("roles").get(0).asText()).isEqualTo(p.role());
      Set<String> permissions = new TreeSet<>();
      me.get("permissions").forEach(n -> permissions.add(n.asText()));
      assertThat(permissions).as(p.demoUser()).isEqualTo(p.permissions());
    }
  }

  @Test
  void everyScreenOfAPersonaOpensForItsDemoUser() throws Exception {
    String company = data.company().getId().toString();
    for (Persona p : PersonaMenus.load().values()) {
      for (String screen : p.screens()) {
        assertThat(SCREEN_READS).as(screen).containsKey(screen);
        api.doGet(p.demoUser(), SCREEN_READS.get(screen).replace("{c}", company))
            .andExpect(status().isOk());
      }
    }
  }

  @Test
  void theUserAccessMatrixIsNotOfferedToRolesItRefuses() throws Exception {
    // The matrix used to be in the menu of every UAM_VIEW holder, but its endpoint refuses the
    // Requestor and the Second Approver: the entry is now shown to the endpoint's permissions only.
    for (String user : new String[] {"requestor", "secapprover"}) {
      api.doGet(user, "/api/v1/nbadmin/access-matrix").andExpect(status().isForbidden());
    }
    api.doGet("uamapprover", "/api/v1/nbadmin/access-matrix").andExpect(status().isOk());
  }
}
