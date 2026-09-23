package com.iortatechnxt.finverse.coa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.finverse.support.Api;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.Json;
import com.iortatechnxt.finverse.support.TestData;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ChartOfAccountsApiIT {

  private static final String ACCOUNTS = "/api/v1/coa/accounts";

  @Autowired private Api api;
  @Autowired private TestData data;

  private Map<String, Object> account(String code, String cls, String level, String parent) {
    return Json.of(
        "companyId",
        data.company().getId(),
        "code",
        code,
        "name",
        "Test account " + code,
        "accountClass",
        cls,
        "level",
        level,
        "parentCode",
        parent,
        "categoryCode",
        "OPEX",
        "allowManualPosting",
        true,
        "costCenterRequired",
        true,
        "openedOn",
        "2020-01-01",
        "allowedCurrencies",
        List.of("PHP"));
  }

  private String uniqueCode(String prefix) {
    return prefix + ThreadLocalRandom.current().nextInt(100, 999);
  }

  /**
   * A new, authorized postable expense account under 5600 used as the parent of the micro accounts
   * created here. Adding a child turns the parent into a heading, so the tests must never attach
   * children to shared demo accounts that other test classes post to.
   */
  private String testParent() throws Exception {
    String code = uniqueCode("56T");
    long id =
        api.read(api.doPost("accountant", ACCOUNTS, account(code, "EXPENSE", "SUB", "5600")))
            .get("id")
            .asLong();
    api.doPost("checker", ACCOUNTS + "/" + id + "/authorize", null).andExpect(status().isOk());
    return code;
  }

  @Test
  void accountLifecycleWithMakerCheckerFreezeAndClose() throws Exception {
    String parent = testParent();
    String code = uniqueCode(parent + "-");
    var created =
        api.read(
            api.doPost("accountant", ACCOUNTS, account(code, "EXPENSE", "MICRO", parent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION")));
    long id = created.get("id").asLong();

    api.doPost("accountant", ACCOUNTS + "/" + id + "/authorize", null)
        .andExpect(status().isForbidden());
    api.doPost("checker", ACCOUNTS + "/" + id + "/authorize", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.authorizedBy").value("checker"));

    var update = account(code, "EXPENSE", "MICRO", parent);
    update.put("name", "Renamed " + code);
    api.doPut("accountant", ACCOUNTS + "/" + id, update)
        .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"))
        .andExpect(jsonPath("$.name").value("Renamed " + code));
    api.doPost("checker", ACCOUNTS + "/" + id + "/authorize", null).andExpect(status().isOk());

    api.doPost("checker", ACCOUNTS + "/" + id + "/freeze", Json.of("reason", "Audit hold"))
        .andExpect(jsonPath("$.frozen").value(true))
        .andExpect(jsonPath("$.freezeReason").value("Audit hold"));
    api.doPost("checker", ACCOUNTS + "/" + id + "/unfreeze", null)
        .andExpect(jsonPath("$.frozen").value(false));
    api.doPost("checker", ACCOUNTS + "/" + id + "/close", null)
        .andExpect(jsonPath("$.closedOn").value(LocalDate.now().toString()));
    api.doGet("auditor", ACCOUNTS + "/" + id).andExpect(jsonPath("$.code").value(code));
    api.doGet("auditor", ACCOUNTS + "?companyId=" + data.company().getId() + "&q=" + code)
        .andExpect(jsonPath("$[0].code").value(code));
  }

  @Test
  void makerCannotAuthorizeOwnAccount() throws Exception {
    String parent = testParent();
    long id =
        api.read(
                api.doPost(
                    "fmanager",
                    ACCOUNTS,
                    account(uniqueCode(parent + "-"), "EXPENSE", "MICRO", parent)))
            .get("id")
            .asLong();
    api.doPost("fmanager", ACCOUNTS + "/" + id + "/authorize", null)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("MAKER_CHECKER_VIOLATION"));
  }

  @Test
  void hierarchyRulesAreEnforced() throws Exception {
    api.doPost("accountant", ACCOUNTS, account("5601", "EXPENSE", "SUB", "5600"))
        .andExpect(status().isConflict());
    api.doPost("accountant", ACCOUNTS, account(uniqueCode("9-"), "EXPENSE", "MICRO", "5000"))
        .andExpect(jsonPath("$.code").value("INVALID_TIER"));
    api.doPost("accountant", ACCOUNTS, account(uniqueCode("9-"), "ASSET", "MICRO", testParent()))
        .andExpect(jsonPath("$.code").value("CLASS_MISMATCH"));
    api.doPost("accountant", ACCOUNTS, account(uniqueCode("9-"), "EXPENSE", "SUB", null))
        .andExpect(jsonPath("$.code").value("PARENT_REQUIRED"));
  }

  @Test
  void accountWithPostingsCannotBecomeHeading() throws Exception {
    var journal =
        Json.of(
            "companyId",
            data.company().getId(),
            "branchId",
            data.branch("HO").getId(),
            "journalType",
            "MANUAL",
            "valueDate",
            LocalDate.now().toString(),
            "currency",
            "PHP",
            "narration",
            "Creditable withholding tax",
            "lines",
            List.of(
                Json.of("accountCode", "1602", "side", "DEBIT", "amount", 10),
                Json.of("accountCode", "1111", "side", "CREDIT", "amount", 10)));
    long jid = api.read(api.doPost("accountant", "/api/v1/journals", journal)).get("id").asLong();
    api.doPost("accountant", "/api/v1/journals/" + jid + "/submit", null)
        .andExpect(status().isOk());
    api.doPost("checker", "/api/v1/journals/" + jid + "/approve", null)
        .andExpect(jsonPath("$.status").value("POSTED"));

    var child = account(uniqueCode("1602-"), "ASSET", "MICRO", "1602");
    child.put("categoryCode", "RECV");
    api.doPost("accountant", ACCOUNTS, child)
        .andExpect(jsonPath("$.code").value("PARENT_HAS_POSTINGS"));
  }

  @Test
  void categoriesCanBeListedAndCreatedOnce() throws Exception {
    String code = uniqueCode("T");
    var body =
        Json.of(
            "code",
            code,
            "name",
            "Test category",
            "accountClass",
            "EXPENSE",
            "bankCategory",
            false);
    api.doPost("accountant", "/api/v1/coa/categories", body).andExpect(status().isCreated());
    api.doPost("accountant", "/api/v1/coa/categories", body).andExpect(status().isConflict());
    assertThat(api.read(api.doGet("auditor", "/api/v1/coa/categories")).size()).isGreaterThan(10);
  }
}
