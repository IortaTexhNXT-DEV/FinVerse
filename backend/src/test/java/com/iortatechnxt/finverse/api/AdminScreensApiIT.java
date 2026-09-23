package com.iortatechnxt.finverse.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Exercises, through HTTP, the platform APIs behind the admin screens (parties, accounting rules,
 * event register, sub-ledger inquiry) and the finance report endpoints, as the UI calls them.
 */
@IntegrationTest
class AdminScreensApiIT {

  @Autowired private MockMvc mvc;
  @Autowired private TestData data;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;

  private RequestPostProcessor as(String username) {
    RequestPostProcessor principal = user(users.loadUserByUsername(username));
    return request -> csrf().postProcessRequest(principal.postProcessRequest(request));
  }

  private String partyJson(String code, String name) {
    return """
        {"companyId": %d, "code": "%s", "name": "%s", "partyType": "SUPPLIER",
         "defaultCurrency": "PHP", "creditDays": 30, "email": "ap@example.ph",
         "withholdingTaxRate": 2.0}
        """
        .formatted(data.company().getId(), code, name);
  }

  @Test
  void partyIsCreatedEditedAndAuthorizedByDifferentUsers() throws Exception {
    String code = "T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    String body =
        mvc.perform(
                post("/api/v1/parties")
                    .with(as("accountant"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(partyJson(code, "Test Supplier")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = json.readTree(body).get("id").asLong();
    mvc.perform(
            put("/api/v1/parties/" + id)
                .with(as("accountant"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(partyJson(code, "Test Supplier Renamed")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Supplier Renamed"));
    mvc.perform(post("/api/v1/parties/" + id + "/authorize").with(as("checker")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    mvc.perform(get("/api/v1/parties/" + id).with(as("checker")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(code));
    mvc.perform(
            post("/api/v1/parties")
                .with(as("accountant"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(partyJson(code, "Duplicate")))
        .andExpect(status().isConflict());
  }

  private String ruleJson(String name, String bankRole) {
    return """
        {"companyId": %d, "eventType": "MISC_RECEIPT", "name": "%s", "priority": 50,
         "currency": "", "effectiveFrom": "2026-01-01",
         "lines": [
           {"side": "DEBIT", "accountCode": "%s", "amountComponent": "AMOUNT", "partyLine": false},
           {"side": "CREDIT", "accountCode": "4700", "amountComponent": "AMOUNT",
            "partyLine": false, "narration": "Other income"}]}
        """
        .formatted(data.company().getId(), name, bankRole);
  }

  @Test
  void accountingRuleLifecycleAndSimulation() throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/accounting/rules")
                    .with(as("fmanager"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ruleJson("Misc receipt test rule", "@BANK")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode rule = json.readTree(body);
    long id = rule.get("id").asLong();
    mvc.perform(
            put("/api/v1/accounting/rules/" + id)
                .with(as("fmanager"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleJson("Misc receipt test rule v2", "@BANK")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lines.length()").value(2));
    String threeLines =
        ruleJson("Misc receipt test rule v3", "@BANK")
            .replace(
                "\"Other income\"}]",
                "\"Other income\"}, {\"side\": \"CREDIT\", \"accountCode\": \"4700\","
                    + " \"amountComponent\": \"AMOUNT\", \"partyLine\": false}]");
    for (String content : new String[] {threeLines, ruleJson("v4", "@BANK")}) {
      mvc.perform(
              put("/api/v1/accounting/rules/" + id)
                  .with(as("fmanager"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(content))
          .andExpect(status().isOk());
    }
    mvc.perform(post("/api/v1/accounting/rules/" + id + "/authorize").with(as("checker")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    String simulation =
        """
        {"companyId": %d, "branchId": %d, "eventType": "MISC_RECEIPT", "valueDate": "%s",
         "currency": "PHP", "amounts": {"AMOUNT": 1500.00}, "accounts": {"BANK": "1111"}}
        """
            .formatted(data.company().getId(), data.branch("HO").getId(), LocalDate.now());
    mvc.perform(
            post("/api/v1/accounting/simulate")
                .with(as("fmanager"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(simulation))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lines.length()").value(2))
        .andExpect(jsonPath("$.lines[0].accountCode").value("1111"));
    mvc.perform(
            post("/api/v1/accounting/rules")
                .with(as("fmanager"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleJson("x", "@BANK").replace("MISC_RECEIPT", "NO_SUCH_EVENT")))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void eventRegisterFiltersByStatus() throws Exception {
    mvc.perform(
            get("/api/v1/accounting/events")
                .with(as("fmanager"))
                .param("companyId", data.company().getId().toString())
                .param("status", "POSTED")
                .param("eventType", "")
                .param("from", "2026-01-01")
                .param("to", "2026-12-31")
                .param("size", "500"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(200));
  }

  @Test
  void partyAgeingAndStatementRespond() throws Exception {
    mvc.perform(
            get("/api/v1/subledger/ageing")
                .with(as("accountant"))
                .param("companyId", data.company().getId().toString())
                .param("asOf", LocalDate.now().toString())
                .param("partyCode", "C-000201"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.buckets.length()").value(7));
  }

  @Test
  void financeReportRunsAndExportsOverHttp() throws Exception {
    String params =
        """
        {"companyId": "%d", "month": "%d", "year": "%d"}
        """
            .formatted(
                data.company().getId(), LocalDate.now().getMonthValue(), LocalDate.now().getYear());
    mvc.perform(
            post("/api/v1/reports/FIN-TB-MAIN/run")
                .with(as("accountant"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(params))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("FIN-TB-MAIN"));
    mvc.perform(
            post("/api/v1/reports/FIN-TB-MAIN/export")
                .with(as("accountant"))
                .param("format", "XLSX")
                .contentType(MediaType.APPLICATION_JSON)
                .content(params))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", containsString("FIN-TB-MAIN")));
    mvc.perform(
            post("/api/v1/reports/FIN-TB-MAIN/run")
                .with(as("uw"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(params))
        .andExpect(status().isForbidden());
  }

  @Test
  void reportParametersAreValidatedOverHttp() throws Exception {
    String reversed =
        """
        {"companyId": "%d", "fromDate": "2026-09-01", "toDate": "2026-01-31"}
        """
            .formatted(data.company().getId());
    mvc.perform(
            post("/api/v1/reports/GL-PL/run")
                .with(as("accountant"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reversed))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_REPORT_PARAMETERS"))
        .andExpect(jsonPath("$.detail").value("To Date must not be before From Date"));
    String blankAsOf =
        """
        {"companyId": "%d", "asOfDate": ""}
        """
            .formatted(data.company().getId());
    mvc.perform(
            post("/api/v1/reports/GL-TB/run")
                .with(as("accountant"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(blankAsOf))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.detail").value("As of Date is required"));
  }
}
