package com.iortatechnxt.brokerverse.security.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import com.iortatechnxt.brokerverse.support.TestData;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The functions deferred to Phase 2 by the client decisions of 17-Oct-2026 (reinsurance, partner
 * portal) and the Data Migration console: the super user Brokerverse keeps them, the BDOI group
 * profiles do not hold them, and the server refuses the reinsurance functions to a user without
 * {@link Permission#RI_BROKING_TRANSACT}.
 */
@IntegrationTest
class PhaseTwoFunctionsAccessIT {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private Api api;
  @Autowired private TestData data;

  private Set<String> permissionsOf(String username) throws Exception {
    JsonNode me = api.read(api.doGet(username, "/api/v1/auth/me"));
    Set<String> permissions = new TreeSet<>();
    me.get("permissions").forEach(n -> permissions.add(n.asText()));
    return permissions;
  }

  @Test
  void theSuperUserSeesReinsuranceAndDataMigration() throws Exception {
    assertThat(permissionsOf(SeedSuperUser.USERNAME))
        .contains(
            Permission.RI_BROKING_TRANSACT.name(),
            Permission.MIG_VIEW.name(),
            Permission.LEGACY_INQUIRY_VIEW.name());
  }

  @Test
  void noBdoiGroupProfileHoldsReinsuranceDataMigrationOrThePortal() {
    Set<String> held =
        new TreeSet<>(
            jdbc.queryForList(
                "select distinct p.permission from sec_role_permission p"
                    + " join sec_role r on r.id = p.role_id"
                    + " where r.code like 'BDOI\\_%' and r.code <> ?",
                String.class, SeedSuperUser.PROFILE));
    assertThat(held).isNotEmpty().doesNotContain(Permission.RI_BROKING_TRANSACT.name());
    assertThat(held).noneMatch(p -> p.startsWith("MIG_") || p.startsWith("PORTAL"));
  }

  @Test
  void reinsurancePartiesAreRefusedWithoutThePermission() throws Exception {
    assertThat(permissionsOf("accountant"))
        .contains(Permission.MASTER_MAINTAIN.name())
        .doesNotContain(Permission.RI_BROKING_TRANSACT.name());
    String code = "RI-" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    api.doPost("accountant", "/api/v1/parties", party(code, "REINSURER"))
        .andExpect(status().isForbidden());
    api.doPost("accountant", "/api/v1/parties", party(code, "SUPPLIER"))
        .andExpect(status().isCreated());
    api.doPost(SeedSuperUser.USERNAME, "/api/v1/parties", party(code + "R", "RI_BROKER"))
        .andExpect(status().isCreated());
  }

  private Map<String, Object> party(String code, String type) {
    return Json.of(
        "companyId",
        data.company().getId(),
        "code",
        code,
        "name",
        "Phase 2 check " + code,
        "partyType",
        type,
        "defaultCurrency",
        "PHP",
        "creditDays",
        30);
  }
}
