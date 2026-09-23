package com.iortatechnxt.finverse.period;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.finverse.support.Api;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.Json;
import com.iortatechnxt.finverse.support.TestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class PeriodApiIT {

  @Autowired private Api api;
  @Autowired private TestData data;

  @Test
  void periodLifecycleWithCloseGuardAndReopen() throws Exception {
    Long companyId = data.company().getId();
    api.doPost(
            "fmanager", "/api/v1/periods/years", Json.of("companyId", companyId, "yearCode", 2027))
        .andExpect(status().isCreated());
    api.doPost(
            "fmanager", "/api/v1/periods/years", Json.of("companyId", companyId, "yearCode", 2027))
        .andExpect(status().isConflict());
    JsonNode years = api.read(api.doGet("auditor", "/api/v1/periods/years?companyId=" + companyId));
    long yearId = years.get(0).get("id").asLong();
    JsonNode periods =
        api.read(api.doGet("auditor", "/api/v1/periods/years/" + yearId + "/periods"));
    long march = periods.get(2).get("id").asLong();

    api.doPost("fmanager", "/api/v1/periods/" + march + "/close", null)
        .andExpect(jsonPath("$.code").value("INVALID_PERIOD_TRANSITION"));
    api.doPost("fmanager", "/api/v1/periods/" + march + "/open", null)
        .andExpect(jsonPath("$.status").value("OPEN"));
    api.doPost("fmanager", "/api/v1/periods/" + march + "/start-closing", null)
        .andExpect(jsonPath("$.status").value("CLOSING"));
    api.doPost("fmanager", "/api/v1/periods/" + march + "/open", null)
        .andExpect(jsonPath("$.status").value("OPEN"));

    var draft =
        Json.of(
            "companyId",
            companyId,
            "branchId",
            data.branch("HO").getId(),
            "journalType",
            "ACCRUAL",
            "valueDate",
            "2027-03-15",
            "currency",
            "PHP",
            "narration",
            "Accrual draft",
            "lines",
            List.of(
                Json.of(
                    "accountCode", "5605", "side", "DEBIT", "amount", 100, "costCenter", "FIN")));
    long journalId =
        api.read(api.doPost("accountant", "/api/v1/journals", draft)).get("id").asLong();
    api.doPost("fmanager", "/api/v1/periods/" + march + "/close", null)
        .andExpect(jsonPath("$.code").value("PERIOD_CLOSE_BLOCKED"));
    api.doPost("accountant", "/api/v1/journals/" + journalId + "/cancel", null)
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    api.doPost("fmanager", "/api/v1/periods/" + march + "/close", null)
        .andExpect(jsonPath("$.status").value("CLOSED"));
    api.doPost(
            "fmanager",
            "/api/v1/periods/" + march + "/reopen",
            Json.of("reason", "Audit adjustment"))
        .andExpect(jsonPath("$.status").value("REOPENED"))
        .andExpect(jsonPath("$.statusReason").value("Audit adjustment"));
    api.doPost("fmanager", "/api/v1/periods/" + march + "/close", null)
        .andExpect(jsonPath("$.status").value("CLOSED"));
    api.doPost("accountant", "/api/v1/periods/" + march + "/open", null)
        .andExpect(status().isForbidden());
  }
}
