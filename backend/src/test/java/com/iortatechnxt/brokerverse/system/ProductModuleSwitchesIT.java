package com.iortatechnxt.brokerverse.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.common.exception.ModuleNotInUseException;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.domain.ProductModuleSwitchRepository;
import com.iortatechnxt.brokerverse.system.service.JobRegistry;
import com.iortatechnxt.brokerverse.system.service.JobRegistry.JobStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Product module switches (V1160): a switched-off module has no menu (its permissions grant nothing
 * and it is listed as switched off), its APIs refuse in business words, its jobs do not run and its
 * reports are not listed; a switch changes only on the approval of another user.
 */
@IntegrationTest
class ProductModuleSwitchesIT {

  private static final String UNDERWRITING = "UNDERWRITING";

  @Autowired private Api api;
  @Autowired private ProductModuleSwitchRepository switches;
  @Autowired private JobRegistry jobs;
  @Autowired private UserDetailsService users;
  @Autowired private PlatformTransactionManager transactions;

  private void force(String code, boolean on) {
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            s -> {
              var sw = switches.findByCode(code).orElseThrow();
              sw.forceState(on);
              switches.saveAndFlush(sw);
            });
  }

  @AfterEach
  void restore() {
    force(UNDERWRITING, true);
    force("BUDGET", true);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            s ->
                switches
                    .findByCode("BUDGET")
                    .filter(sw -> sw.getPendingEnabled() != null)
                    .ifPresent(
                        sw -> {
                          sw.rejectChange();
                          switches.saveAndFlush(sw);
                        }));
  }

  private List<String> reportCodes(String user) throws Exception {
    List<String> codes = new ArrayList<>();
    api.read(api.doGet(user, "/api/v1/reports").andExpect(status().isOk()))
        .forEach(r -> codes.add(r.get("code").asText()));
    return codes;
  }

  @Test
  void switchedOffModuleHasNoMenuApiJobOrReport() throws Exception {
    String policies = "/api/v1/underwriting/policies?companyId=1";
    api.doGet("fmanager", policies).andExpect(status().isOk());
    assertThat(reportCodes("fmanager")).contains("PGIBR003");

    force(UNDERWRITING, false);

    // Its APIs refuse in business words, before any permission check.
    api.doGet("fmanager", policies)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MODULE_NOT_IN_USE"))
        .andExpect(
            jsonPath("$.detail")
                .value("The Underwriting (insurer) module is not in use in this deployment"));
    // Its reports are not listed and cannot be run.
    assertThat(reportCodes("fmanager")).doesNotContain("PGIBR003").contains("GL-TB");
    api.doPost("fmanager", "/api/v1/reports/PGIBR003/run", Map.of())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MODULE_NOT_IN_USE"));
    // Its job is not shown, does not run on its schedule and is refused when started by hand.
    assertThat(jobs.statuses())
        .extracting(JobStatus::job)
        .noneMatch(j -> "QUOTATION_EXPIRY".equals(j.name()));
    assertThat(jobs.runScheduled("QUOTATION_EXPIRY")).isEmpty();
    assertThatThrownBy(() -> jobs.run("QUOTATION_EXPIRY", JobTrigger.MANUAL))
        .isInstanceOf(ModuleNotInUseException.class);
    // Its permissions grant nothing, so its menus are not shown; the web client also reads the
    // switched-off modules.
    assertThat(users.loadUserByUsername("uw").getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .doesNotContain("POLICY_VIEW", "POLICY_MAINTAIN");
    JsonNode inUse =
        api.read(api.doGet("cashier", "/api/v1/system/modules").andExpect(status().isOk()));
    List<String> off = new ArrayList<>();
    inUse.get("switchedOff").forEach(n -> off.add(n.asText()));
    List<String> inactive = new ArrayList<>();
    inUse.get("inactivePermissions").forEach(n -> inactive.add(n.asText()));
    assertThat(off).contains(UNDERWRITING);
    assertThat(inactive).contains("POLICY_VIEW").doesNotContain("JOURNAL_VIEW", "MASTER_VIEW");
    // Platform functions stay available.
    api.doGet("fmanager", "/api/v1/system/about").andExpect(status().isOk());
  }

  @Test
  void switchChangesOnlyOnApprovalOfAnotherUser() throws Exception {
    String budgets = "/api/v1/budgets?companyId=1";
    api.doGet("fmanager", budgets).andExpect(status().isOk());

    api.doPost("admin", "/api/v1/admin/modules/BUDGET/change", Map.of("enabled", false))
        .andExpect(status().isBadRequest());
    api.doPost(
            "admin",
            "/api/v1/admin/modules/BUDGET/change",
            Map.of("enabled", false, "reason", "Budgets are kept outside the system"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.pendingEnabled").value(false));
    // Nothing changes until the approval.
    api.doGet("fmanager", budgets).andExpect(status().isOk());
    api.doPost("admin", "/api/v1/admin/modules/BUDGET/approve", Map.of())
        .andExpect(status().isForbidden());
    api.doPost("infosec", "/api/v1/admin/modules/BUDGET/approve", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.changedReason").value("Budgets are kept outside the system"))
        .andExpect(jsonPath("$.approvedBy").value("infosec"));
    api.doGet("fmanager", budgets)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MODULE_NOT_IN_USE"));
    // The administration of the switches is a platform function and stays available.
    JsonNode list =
        api.read(api.doGet("admin", "/api/v1/admin/modules").andExpect(status().isOk()));
    assertThat(list.findValuesAsText("code")).contains("BUDGET", UNDERWRITING);
  }

  @Test
  void moduleNeededByAnotherCannotBeSwitchedOff() throws Exception {
    api.doPost(
            "admin",
            "/api/v1/admin/modules/UNDERWRITING/change",
            Map.of("enabled", false, "reason", "Insurer suite not used"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("MODULE_NEEDED"));
    JsonNode profiles =
        api.read(api.doGet("admin", "/api/v1/admin/modules/profiles").andExpect(status().isOk()));
    assertThat(profiles.findValuesAsText("code")).contains("INSURANCE_BROKER", "COMPLETE_SUITE");
  }
}
