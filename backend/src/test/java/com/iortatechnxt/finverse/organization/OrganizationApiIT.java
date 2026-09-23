package com.iortatechnxt.finverse.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.support.Api;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.Json;
import com.iortatechnxt.finverse.support.TestData;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class OrganizationApiIT {

  private static final String ORG = "/api/v1/organization";

  @Autowired private Api api;
  @Autowired private TestData data;
  @Autowired private OrganizationService organization;

  private static String code(String prefix) {
    return prefix + ThreadLocalRandom.current().nextInt(1000, 9999);
  }

  private Map<String, Object> company(String code) {
    return Json.of(
        "code",
        code,
        "name",
        "Subsidiary " + code,
        "baseCurrency",
        "PHP",
        "taxId",
        "999-000",
        "address",
        "Makati",
        "fiscalYearStartMonth",
        1,
        "backValueDays",
        30,
        "forwardValueDays",
        5,
        "retainedEarningsAccount",
        "3500");
  }

  private Map<String, Object> branch(Long companyId, String code) {
    return Json.of(
        "companyId",
        companyId,
        "code",
        code,
        "name",
        "Branch " + code,
        "region",
        "NCR",
        "openingDate",
        "2026-01-01",
        "headOffice",
        false,
        "forexAuthorized",
        true,
        "contactEmail",
        "branch@example.ph",
        "weeklyHolidays",
        "6,7");
  }

  @Test
  void companyAndBranchMakerCheckerLifecycle() throws Exception {
    String co = code("C");
    long companyId =
        api.read(
                api.doPost("fmanager", ORG + "/companies", company(co))
                    .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    api.doPost("fmanager", ORG + "/companies", company(co)).andExpect(status().isConflict());
    api.doPost("checker", ORG + "/companies/" + companyId + "/authorize", null)
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    var update = company(co);
    update.put("name", "Renamed " + co);
    api.doPut("fmanager", ORG + "/companies/" + companyId, update)
        .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"));

    String br = code("B");
    long branchId =
        api.read(api.doPost("accountant", ORG + "/branches", branch(companyId, br)))
            .get("id")
            .asLong();
    api.doPost("accountant", ORG + "/branches", branch(companyId, br))
        .andExpect(status().isConflict());
    api.doPost("checker", ORG + "/branches/" + branchId + "/authorize", null)
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    var branchUpdate = branch(companyId, br);
    branchUpdate.put("managerName", "New Manager");
    api.doPut("accountant", ORG + "/branches/" + branchId, branchUpdate)
        .andExpect(jsonPath("$.managerName").value("New Manager"));
    api.doPost("checker", ORG + "/branches/" + branchId + "/deactivate", null)
        .andExpect(jsonPath("$.recordStatus").value("INACTIVE"));
    api.doGet("auditor", ORG + "/branches?companyId=" + companyId)
        .andExpect(jsonPath("$[0].code").value(br));
  }

  @Test
  void invalidBranchIsRejectedWithFieldErrors() throws Exception {
    var bad = branch(data.company().getId(), "lower case");
    bad.put("contactEmail", "not-an-email");
    api.doPost("accountant", ORG + "/branches", bad)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.code").exists())
        .andExpect(jsonPath("$.errors.contactEmail").exists());
  }

  @Test
  void holidaysAndWorkingDays() throws Exception {
    Long companyId = data.company().getId();
    api.doPost(
            "accountant",
            ORG + "/holidays",
            Json.of(
                "companyId",
                companyId,
                "holidayDate",
                "2026-11-02",
                "description",
                "All Souls Day"))
        .andExpect(status().isCreated());
    assertThat(
            api.read(api.doGet("auditor", ORG + "/holidays?companyId=" + companyId + "&year=2026"))
                .size())
        .isGreaterThan(9);
    var branch = data.branch("HO");
    assertThat(organization.isWorkingDay(branch, LocalDate.of(2026, 11, 2))).isFalse();
    assertThat(organization.isWorkingDay(branch, LocalDate.of(2026, 9, 26))).isFalse();
    assertThat(organization.isWorkingDay(branch, LocalDate.of(2026, 9, 23))).isTrue();
  }
}
