package com.iortatechnxt.brokerverse.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.dashboard.domain.HomeWidget;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Role Home Set-up (FR-CR-010, V2820): the System Administrator requests the widgets of a role,
 * another user approves them, and the home page of the role's users shows them in that order; a
 * role without its own set shows the delivered widgets.
 */
@IntegrationTest
class RoleHomeSetupIT {

  private static final String BASE = "/api/v1/role-homes";

  @Autowired private Api api;
  @Autowired private JdbcTemplate jdbc;

  @AfterEach
  void restore() {
    jdbc.update("delete from sys_role_home");
  }

  private String roleOf(String user) {
    return jdbc.queryForObject(
        "select min(r.code) from sec_role r join sec_user_role ur on ur.role_id = r.id"
            + " join sec_user u on u.id = ur.user_id where u.username = ?",
        String.class,
        user);
  }

  private int audited(String role) {
    Integer count =
        jdbc.queryForObject(
            "select count(*) from audit_log where entity_type = 'RoleHome' and entity_id = ?"
                + " and (summary like 'Requested the home-page widgets%'"
                + " or summary like 'Home-page widgets of%')",
            Integer.class, role);
    return count == null ? 0 : count;
  }

  private List<String> mine(String user) throws Exception {
    List<String> codes = new ArrayList<>();
    api.read(api.doGet(user, BASE + "/mine").andExpect(status().isOk()))
        .forEach(n -> codes.add(n.asText()));
    return codes;
  }

  @Test
  void widgetsOfARoleChangeOnlyOnApproval() throws Exception {
    String role = roleOf("cashier");
    int audited = audited(role);
    assertThat(mine("cashier")).isEqualTo(HomeWidget.defaults());

    // Only the System Administrator requests; the requester cannot approve.
    api.doPost(
            "cashier",
            BASE + "/" + role + "/change",
            Map.of("widgets", List.of("CASH"), "reason", "Counter"))
        .andExpect(status().isForbidden());
    api.doPost(
            "admin",
            BASE + "/" + role + "/change",
            Map.of("widgets", List.of("CASH", "WORKLOAD"), "reason", "Cash counter first"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendingWidgets[0]").value("CASH"))
        .andExpect(jsonPath("$.delivered").value(true));
    api.doPost(
            "admin",
            BASE + "/" + role + "/change",
            Map.of("widgets", List.of("KPIS"), "reason", "Again"))
        .andExpect(status().isUnprocessableEntity());
    assertThat(mine("cashier")).isEqualTo(HomeWidget.defaults());
    api.doPost("admin", BASE + "/" + role + "/approve", Map.of()).andExpect(status().isForbidden());

    api.doPost("infosec", BASE + "/" + role + "/approve", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approvedBy").value("infosec"))
        .andExpect(jsonPath("$.delivered").value(false));
    assertThat(mine("cashier")).containsExactly("CASH", "WORKLOAD");

    JsonNode list = api.read(api.doGet("infosec", BASE).andExpect(status().isOk()));
    assertThat(list.findValuesAsText("roleCode")).contains(role, "SYSADMIN");
    assertThat(audited(role) - audited).isEqualTo(2);
  }

  @Test
  void unknownOrDuplicateWidgetsAndRejection() throws Exception {
    String role = roleOf("cashier");
    String change = BASE + "/" + role + "/change";
    api.doPost("admin", change, Map.of("widgets", List.of("NOPE"), "reason", "x"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ROLE_HOME_UNKNOWN_WIDGET"));
    api.doPost("admin", change, Map.of("widgets", List.of("CASH", "CASH"), "reason", "x"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ROLE_HOME_DUPLICATE_WIDGET"));
    api.doPost("admin", change, Map.of("widgets", List.of("CASH"), "reason", "x"))
        .andExpect(status().isOk());
    api.doPost("infosec", BASE + "/" + role + "/reject", Map.of())
        .andExpect(status().isUnprocessableEntity());
    api.doPost("infosec", BASE + "/" + role + "/reject", Map.of("reason", "Keep the figures"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendingWidgets").doesNotExist());
    assertThat(mine("cashier")).isEqualTo(HomeWidget.defaults());
  }
}
