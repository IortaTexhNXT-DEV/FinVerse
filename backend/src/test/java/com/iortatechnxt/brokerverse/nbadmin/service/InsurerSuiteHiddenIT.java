package com.iortatechnxt.brokerverse.nbadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.JobRegistry;
import com.iortatechnxt.brokerverse.system.service.JobRegistry.JobStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The insurer suite is hidden from BDOI (V1064, CODEBASE_RELEVANCE_AUDIT.md R1 and step 1): no BDOI
 * role holds an insurer-only permission, the insurer roles are inactive, the insurer jobs are
 * disabled, the report catalogue has no insurer report, and the User Access screens list neither
 * the insurer permissions nor the roles holding them. The seed-only roles SIT_INS_* keep the
 * insurer stories of the SIT/UAT users until the insurer modules are removed (seed V1961).
 */
@IntegrationTest
class InsurerSuiteHiddenIT {

  private static final List<String> INSURER_ROLES =
      List.of("UNDERWRITER", "CLAIMS_OFFICER", "RI_OFFICER");

  private static final List<String> INSURER_REPORTS =
      List.of(
          "RSV-SUMMARY",
          "RSV-TRIANGLE",
          "RSV-UPR-MOVE",
          "GL-CON-TB",
          "GL-CON-BS",
          "GL-CON-PL",
          "GL-CON-ELIM",
          "GL-ICREC",
          "TAX-PREMTAX",
          "TAX-DST-2000",
          "IC-PREM-LOB",
          "IC-LOSS-LOB",
          "IC-COMM-LOB",
          "IC-RESERVES",
          "IC-RBC",
          "IC-NETWORTH",
          "IC-INVEST");

  @Autowired private JdbcTemplate jdbc;
  @Autowired private JobRegistry jobs;
  @Autowired private Api api;

  private static List<String> insurerPermissions() {
    return Arrays.stream(Permission.values())
        .filter(Permission::isInsurerOnly)
        .map(Enum::name)
        .toList();
  }

  @Test
  void noBdoiRoleHoldsAnInsurerPermission() {
    List<String> held = new ArrayList<>();
    jdbc.query(
        """
        select r.code, p.permission from sec_role r join sec_role_permission p on p.role_id = r.id
        where r.code not in ('UNDERWRITER', 'CLAIMS_OFFICER', 'RI_OFFICER')
          and r.code not like 'SIT\\_INS\\_%'
        """,
        rs -> {
          if (insurerPermissions().contains(rs.getString(2))) {
            held.add(rs.getString(1) + ":" + rs.getString(2));
          }
        });
    assertThat(held).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_role where code in ('FIN_MANAGER', 'AUDITOR',"
                    + " 'FRBS_HEAD', 'DISB_APPROVER')",
                Long.class))
        .isEqualTo(4L);
  }

  @Test
  void insurerRolesAreInactive() {
    List<Boolean> active =
        jdbc.queryForList(
            "select active from sec_role where code in ('UNDERWRITER', 'CLAIMS_OFFICER',"
                + " 'RI_OFFICER')",
            Boolean.class);
    assertThat(active).hasSize(3).containsOnly(false);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_access_change_log where activity = 'DEACTIVATE_ROLE'"
                    + " and subject in ('UNDERWRITER', 'CLAIMS_OFFICER', 'RI_OFFICER')",
                Long.class))
        .isEqualTo(3L);
    // The seed-only copies carry the insurer stories of the SIT/UAT users.
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_role where code like 'SIT\\_INS\\_%' and active",
                Long.class))
        .isPositive();
  }

  @Test
  void insurerJobsAreDisabled() {
    List<JobStatus> insurer =
        jobs.statuses().stream()
            .filter(
                s ->
                    Set.of("QUOTATION_EXPIRY", "RI_ALLOCATION", "RESERVE_VALUATION")
                        .contains(s.job().name()))
            .toList();
    assertThat(insurer).hasSize(3);
    assertThat(insurer)
        .allSatisfy(
            s -> {
              assertThat(s.job().cron()).isEqualTo(JobRegistry.DISABLED);
              assertThat(s.nextRun()).isNull();
            });
  }

  @Test
  void reportCatalogueOfBdoiRolesHasNoInsurerReport() throws Exception {
    for (String user : List.of("glhead", "gltl", "glofficer")) {
      JsonNode catalogue = api.read(api.doGet(user, "/api/v1/reports").andExpect(status().isOk()));
      List<String> codes = new ArrayList<>();
      catalogue.forEach(r -> codes.add(r.get("code").asText()));
      assertThat(codes).as(user).isNotEmpty().doesNotContainAnyElementsOf(INSURER_REPORTS);
    }
    api.doGet("glhead", "/api/v1/reserves/summary?companyId=1&asOf=2026-03-31")
        .andExpect(status().isForbidden());
    api.doGet(
            "glhead",
            "/api/v1/tax/worksheets/PREMIUM_TAX?companyId=1&from=2026-01-01&to=2026-03-31")
        .andExpect(status().isForbidden());
    api.doGet(
            "glhead", "/api/v1/tax/ic/schedules/PREMIUMS?companyId=1&from=2026-01-01&to=2026-03-31")
        .andExpect(status().isForbidden());
  }

  @Test
  void userAccessScreensListNoInsurerPermissionOrRole() throws Exception {
    List<String> offered = new ArrayList<>();
    api.read(api.doGet("admin", "/api/v1/admin/permissions").andExpect(status().isOk()))
        .forEach(p -> offered.add(p.asText()));
    assertThat(offered).isNotEmpty().doesNotContainAnyElementsOf(insurerPermissions());

    for (String url : List.of("/api/v1/admin/roles", "/api/v1/nbadmin/roles")) {
      List<String> roles = new ArrayList<>();
      api.read(api.doGet("admin", url).andExpect(status().isOk()))
          .forEach(r -> roles.add(r.get("code").asText()));
      assertThat(roles).as(url).contains("FIN_MANAGER").doesNotContainAnyElementsOf(INSURER_ROLES);
      assertThat(roles).as(url).noneMatch(r -> r.startsWith("SIT_INS_"));
    }

    JsonNode matrix =
        api.read(api.doGet("admin", "/api/v1/nbadmin/access-matrix").andExpect(status().isOk()));
    List<String> rows = new ArrayList<>();
    matrix.get("permissions").forEach(r -> rows.add(r.get("permission").asText()));
    assertThat(rows).isNotEmpty().doesNotContainAnyElementsOf(insurerPermissions());

    assertThatThrownBy(() -> RolePermissionChangeValidator.requireKnown(Set.of("POLICY_VIEW")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("POLICY_VIEW");
  }
}
