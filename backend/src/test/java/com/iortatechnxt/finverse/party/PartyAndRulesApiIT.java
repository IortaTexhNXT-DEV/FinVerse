package com.iortatechnxt.finverse.party;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.support.Api;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.Json;
import com.iortatechnxt.finverse.support.TestData;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class PartyAndRulesApiIT {

  @Autowired private Api api;
  @Autowired private TestData data;
  @Autowired private PartyService parties;

  private Map<String, Object> party(String code) {
    return Json.of(
        "companyId",
        data.company().getId(),
        "code",
        code,
        "name",
        "Test Supplier " + code,
        "partyType",
        "SUPPLIER",
        "taxId",
        "123",
        "email",
        "s@example.ph",
        "defaultCurrency",
        "PHP",
        "creditDays",
        30,
        "withholdingTaxRate",
        2);
  }

  @Test
  void partyMaintenanceAndLookups() throws Exception {
    String code = "S-T" + ThreadLocalRandom.current().nextInt(1000, 9999);
    long id =
        api.read(
                api.doPost("accountant", "/api/v1/parties", party(code))
                    .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    api.doPost("accountant", "/api/v1/parties", party(code)).andExpect(status().isConflict());
    var update = party(code);
    update.put("creditDays", 45);
    api.doPut("accountant", "/api/v1/parties/" + id, update)
        .andExpect(jsonPath("$.creditDays").value(45));
    api.doPost("checker", "/api/v1/parties/" + id + "/authorize", null)
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    api.doGet("auditor", "/api/v1/parties/" + id).andExpect(jsonPath("$.code").value(code));

    Long companyId = data.company().getId();
    assertThat(parties.requireActive(companyId, code, Set.of(PartyType.SUPPLIER)).getName())
        .contains(code);
    assertThatThrownBy(() -> parties.requireActive(companyId, code, Set.of(PartyType.BROKER)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("expected");
    assertThat(parties.search(companyId, Set.of(), "luzon")).isNotEmpty();
  }

  @Test
  void accountingRuleLifecycleAndSimulation() throws Exception {
    Long companyId = data.company().getId();
    var rule =
        Json.of(
            "companyId",
            companyId,
            "eventType",
            "MISC_RECEIPT",
            "name",
            "Motor misc receipts",
            "businessLine",
            "MOTOR",
            "currency",
            "PHP",
            "priority",
            10,
            "effectiveFrom",
            "2026-01-01",
            "lines",
            List.of(
                Json.of(
                    "side",
                    "DEBIT",
                    "accountCode",
                    "@BANK",
                    "amountComponent",
                    "AMOUNT",
                    "partyLine",
                    false),
                Json.of(
                    "side",
                    "CREDIT",
                    "accountCode",
                    "4700",
                    "amountComponent",
                    "AMOUNT",
                    "partyLine",
                    false)));
    long id =
        api.read(
                api.doPost("fmanager", "/api/v1/accounting/rules", rule)
                    .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    rule.put("name", "Motor miscellaneous receipts");
    api.doPut("fmanager", "/api/v1/accounting/rules/" + id, rule)
        .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"));
    api.doPost("checker", "/api/v1/accounting/rules/" + id + "/authorize", null)
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));

    var simulation =
        Json.of(
            "companyId",
            companyId,
            "branchId",
            data.branch("HO").getId(),
            "eventType",
            "MISC_RECEIPT",
            "valueDate",
            "2026-06-30",
            "currency",
            "PHP",
            "businessLine",
            "MOTOR",
            "amounts",
            Map.of("AMOUNT", 1500),
            "accounts",
            Map.of("BANK", "1111"));
    api.doPost("auditor", "/api/v1/accounting/simulate", simulation)
        .andExpect(jsonPath("$.ruleId").value(id))
        .andExpect(jsonPath("$.lines[0].accountCode").value("1111"))
        .andExpect(jsonPath("$.lines[1].accountCode").value("4700"));

    rule.put("eventType", "NO_SUCH_EVENT");
    api.doPost("fmanager", "/api/v1/accounting/rules", rule)
        .andExpect(jsonPath("$.code").value("UNKNOWN_EVENT_TYPE"));
    api.doGet(
            "auditor",
            "/api/v1/accounting/events?companyId="
                + companyId
                + "&status=POSTED&eventType=POLICY_ISSUE&from=2026-01-01&to=2026-12-31")
        .andExpect(status().isOk());
  }
}
