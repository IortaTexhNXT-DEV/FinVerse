package com.iortatechnxt.finverse.journal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.finverse.support.Api;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.Json;
import com.iortatechnxt.finverse.support.TestData;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class JournalApiIT {

  private static final String JOURNALS = "/api/v1/journals/";

  @Autowired private Api api;
  @Autowired private TestData data;

  private Map<String, Object> voucher(String amount, String costCenter) {
    return Json.of(
        "companyId",
        data.company().getId(),
        "branchId",
        data.branch("CEB").getId(),
        "journalType",
        "MANUAL",
        "valueDate",
        LocalDate.now().toString(),
        "currency",
        "PHP",
        "narration",
        "Telephone bills",
        "reference",
        "TEL-09",
        "lines",
        List.of(
            Json.of(
                "accountCode", "5604", "side", "DEBIT", "amount", amount, "costCenter", costCenter),
            Json.of("accountCode", "1111", "side", "CREDIT", "amount", amount)));
  }

  @Test
  void rejectCorrectResubmitApproveCopyAndReverse() throws Exception {
    long id =
        api.read(api.doPost("accountant", "/api/v1/journals", voucher("850.00", "FIN")))
            .get("id")
            .asLong();
    api.doPost("accountant", JOURNALS + id + "/submit", null)
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    api.doPost("checker", JOURNALS + id + "/reject", Json.of("reason", "Wrong amount"))
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.rejectionReason").value("Wrong amount"));

    api.doPut("fmanager", JOURNALS + id, voucher("900.00", "FIN"))
        .andExpect(status().isForbidden());
    api.doPut("accountant", JOURNALS + id, voucher("900.00", "FIN"))
        .andExpect(jsonPath("$.totalDebit").value(900.0));
    api.doPost("accountant", JOURNALS + id + "/submit", null).andExpect(status().isOk());
    api.doPost("checker", JOURNALS + id + "/approve", null)
        .andExpect(jsonPath("$.status").value("POSTED"))
        .andExpect(jsonPath("$.lines.length()").value(2));

    api.doGet("auditor", JOURNALS + id).andExpect(jsonPath("$.authorizedBy").value("checker"));
    api.doGet(
            "auditor",
            "/api/v1/journals?companyId="
                + data.company().getId()
                + "&batchNo=JV-CEB&authorizer=checker&minAmount=100&fromDate=2026-01-01")
        .andExpect(status().isOk());

    long copyId =
        api.read(
                api.doPost("accountant", JOURNALS + id + "/copy?valueDate=" + LocalDate.now(), null)
                    .andExpect(jsonPath("$.status").value("DRAFT")))
            .get("id")
            .asLong();
    api.doPost("accountant", JOURNALS + copyId + "/cancel", null)
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    long reversalId =
        api.read(
                api.doPost(
                        "fmanager",
                        JOURNALS + id + "/reverse",
                        Json.of("reversalDate", LocalDate.now().toString(), "reason", "Duplicate"))
                    .andExpect(jsonPath("$.journalType").value("REVERSAL")))
            .get("id")
            .asLong();
    api.doPost(
            "fmanager",
            JOURNALS + id + "/reverse",
            Json.of("reversalDate", LocalDate.now().toString(), "reason", "Again"))
        .andExpect(jsonPath("$.code").value("REVERSAL_PENDING"));
    api.doPost("checker", JOURNALS + reversalId + "/approve", null)
        .andExpect(jsonPath("$.status").value("POSTED"));
    api.doGet("auditor", JOURNALS + id).andExpect(jsonPath("$.status").value("REVERSED"));

    long accountId =
        api.read(
                api.doGet(
                    "auditor",
                    "/api/v1/coa/accounts?companyId=" + data.company().getId() + "&q=5604"))
            .get(0)
            .get("id")
            .asLong();
    api.doGet(
            "auditor",
            "/api/v1/ledger/accounts/"
                + accountId
                + "/statement?from=2026-01-01&to="
                + LocalDate.now())
        .andExpect(jsonPath("$.accountCode").value("5604"))
        .andExpect(jsonPath("$.lines.length()").isNumber());
  }

  @Test
  void invalidDimensionAndSystemJournalTypeAreRejected() throws Exception {
    long id =
        api.read(api.doPost("accountant", "/api/v1/journals", voucher("10.00", "NOPE")))
            .get("id")
            .asLong();
    api.doPost("accountant", JOURNALS + id + "/submit", null)
        .andExpect(jsonPath("$.code").value("INVALID_DIMENSION"));
    var system = voucher("10.00", "FIN");
    system.put("journalType", "PREMIUM");
    api.doPost("accountant", "/api/v1/journals", system)
        .andExpect(jsonPath("$.code").value("INVALID_JOURNAL_TYPE"));
    var unknown = voucher("10.00", "FIN");
    unknown.put("lines", List.of(Json.of("accountCode", "0000", "side", "DEBIT", "amount", 1)));
    api.doPost("accountant", "/api/v1/journals", unknown)
        .andExpect(jsonPath("$.code").value("UNKNOWN_ACCOUNT"));
  }
}
