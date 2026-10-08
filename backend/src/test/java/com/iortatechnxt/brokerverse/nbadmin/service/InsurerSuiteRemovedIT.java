package com.iortatechnxt.brokerverse.nbadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.domain.ProductModule;
import com.iortatechnxt.brokerverse.system.service.JobRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The insurer suite is removed (V2500-V2502, seed V2510, CODEBASE_RELEVANCE_AUDIT.md steps 2-6):
 * its tables, roles, permissions, module switches, event types, exception codes, parameters, jobs,
 * reports and SIT/UAT users are gone, and the broker's tax outputs remain.
 */
@IntegrationTest
class InsurerSuiteRemovedIT {

  private static final List<String> INSURER_PERMISSIONS =
      List.of(
          "POLICY_VIEW",
          "POLICY_MAINTAIN",
          "POLICY_AUTHORIZE",
          "CLAIM_VIEW",
          "CLAIM_MAINTAIN",
          "CLAIM_AUTHORIZE",
          "REINSURANCE_VIEW",
          "REINSURANCE_MAINTAIN",
          "REINSURANCE_AUTHORIZE",
          "RESERVE_PREPARE",
          "RESERVE_VIEW",
          "RESERVE_APPROVE",
          "CONSOLIDATION_RUN",
          "INSURER_TAX_VIEW");

  private static final List<String> INSURER_REPORTS =
      List.of(
          "PGIBR003",
          "CLM-REGISTER",
          "RI-SOA",
          "RSV-SUMMARY",
          "GL-CON-TB",
          "GL-ICREC",
          "TAX-PREMTAX",
          "TAX-DST-2000",
          "IC-PREM-LOB",
          "IC-RBC");

  @Autowired private JdbcTemplate jdbc;
  @Autowired private JobRegistry jobs;
  @Autowired private Api api;

  private long count(String sql) {
    Long value = jdbc.queryForObject(sql, Long.class);
    return value == null ? 0 : value;
  }

  @Test
  void insurerTablesAreDropped() {
    assertThat(
            count(
                "select count(*) from information_schema.tables where table_schema = current_schema()"
                    + " and (table_name ~ '^(uw|clm|ri|rsv|con|ic)_' or table_name = 'tax_ic_line_item')"))
        .isZero();
    assertThat(
            count(
                "select count(*) from information_schema.tables where table_schema = current_schema()"
                    + " and table_name = 'bud_budget'"))
        .isEqualTo(1L);
  }

  @Test
  void insurerRolesPermissionsAndUsersAreRemoved() {
    assertThat(
            count(
                "select count(*) from sec_role where code in ('UNDERWRITER', 'CLAIMS_OFFICER',"
                    + " 'RI_OFFICER') or code like 'SIT\\_INS\\_%'"))
        .isZero();
    assertThat(
            jdbc.queryForList("select distinct permission from sec_role_permission", String.class))
        .doesNotContainAnyElementsOf(INSURER_PERMISSIONS);
    assertThat(
            jdbc.queryForList(
                "select distinct permission from sec_permission_action", String.class))
        .doesNotContainAnyElementsOf(INSURER_PERMISSIONS);
    assertThat(Arrays.stream(Permission.values()).map(Enum::name))
        .doesNotContainAnyElementsOf(INSURER_PERMISSIONS);
    assertThat(
            count("select count(*) from sec_user where username in ('uw', 'claims', 'reinsurer')"))
        .isZero();
    assertThat(count("select count(*) from sec_user where username = 'fmanager'")).isEqualTo(1L);
    assertThatThrownBy(() -> RolePermissionChangeValidator.requireKnown(Set.of("POLICY_VIEW")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("POLICY_VIEW");
  }

  @Test
  void insurerReferenceDataIsRemoved() {
    assertThat(
            count(
                "select count(*) from acc_event_type where code in ('CLAIM_RESERVE',"
                    + " 'RI_PREMIUM_CEDED', 'UPR_PROVISION', 'RI_SOA_ADJUSTMENT',"
                    + " 'PREMIUM_DEFICIENCY_PROVISION')"))
        .isZero();
    assertThat(count("select count(*) from acc_rule where event_type = 'IBNR_PROVISION'")).isZero();
    assertThat(count("select count(*) from acc_event_type where code = 'SUPPLIER_INVOICE'"))
        .isEqualTo(1L);
    assertThat(
            count(
                "select count(*) from alt_exception_code where code in ('LARGE_CLAIM_RESERVE',"
                    + " 'LATE_CLAIM_NOTIFICATION', 'RI_FAC_UNPLACED', 'RI_TREATY_CAPACITY')"))
        .isZero();
    assertThat(
            count(
                "select count(*) from sys_parameter where param_key in"
                    + " ('RESERVES_AUTO_RUN_COMPANIES', 'OPEN_COVER_TRANSIT_DAYS')"))
        .isZero();
    assertThat(count("select count(*) from tax_form where worksheet not in ('VAT', 'EWT', 'NONE')"))
        .isZero();
  }

  @Test
  void insurerModuleSwitchesJobsAndReportsAreGone() throws Exception {
    List<String> modules =
        jdbc.queryForList("select code from sys_product_module order by sort_order", String.class);
    assertThat(modules)
        .containsExactlyElementsOf(Arrays.stream(ProductModule.values()).map(Enum::name).toList())
        .doesNotContain("UNDERWRITING", "INSURER_CLAIMS", "REINSURANCE", "ACTUARIAL_RESERVES");
    assertThat(count("select count(*) from sys_module_profile where code = 'COMPLETE_SUITE'"))
        .isZero();
    assertThat(jobs.statuses())
        .extracting(s -> s.job().name())
        .doesNotContain("QUOTATION_EXPIRY", "RESERVE_VALUATION", "RI_ALLOCATION");

    List<String> codes = new ArrayList<>();
    api.read(api.doGet("fmanager", "/api/v1/reports").andExpect(status().isOk()))
        .forEach(r -> codes.add(r.get("code").asText()));
    assertThat(codes)
        .contains("TAX-VAT-2550Q", "TAX-2307-REG", "IC-BROKER-ASBO")
        .doesNotContainAnyElementsOf(INSURER_REPORTS);
  }
}
