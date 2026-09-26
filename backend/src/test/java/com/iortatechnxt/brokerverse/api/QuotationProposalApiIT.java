package com.iortatechnxt.brokerverse.api;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/** Quotation, quotation request and proposal endpoints through the full HTTP stack. */
@IntegrationTest
class QuotationProposalApiIT {

  @Autowired private MockMvc mvc;
  @Autowired private Api api;
  @Autowired private TestData data;
  @Autowired private UserDetailsService users;
  @Autowired private JdbcTemplate jdbc;

  private String company() {
    return data.company().getId().toString();
  }

  private Long id(String table, String column, String value) {
    return jdbc.queryForObject(
        "select id from " + table + " where " + column + " = ?", Long.class, value);
  }

  @ParameterizedTest
  @CsvSource({
    "ao, /api/v1/quotations?companyId={c}",
    "ao, /api/v1/quotations?companyId={c}&status=DRAFT&status=FOR_REVIEW&mine=true&text=QT&product=MTR10",
    "ao, /api/v1/quotations?companyId={c}&expiring=true&page=0&size=5",
    "mkttl, /api/v1/quotations/by-arn/ARN-2026-930004",
    "proc, /api/v1/quotations/by-arn/ARN-2026-930006",
    "ao, /api/v1/quotation-requests?companyId={c}",
    "ao, /api/v1/quotation-requests?companyId={c}&status=NEW&text=dizon",
    "ao, /api/v1/proposals?companyId={c}",
    "tsu, /api/v1/proposals?companyId={c}&status=WITH_TSU&status=QS_SENT&text=PRF",
    "mkttl, /api/v1/proposals?companyId={c}&mine=true&clientId=1",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    mvc.perform(get(url.replace("{c}", company())).with(user(users.loadUserByUsername(username))))
        .andExpect(status().isOk());
  }

  @Test
  void seedQuotationDetailVersionsDiffAndDocuments() throws Exception {
    Long revised = id("quo_quotation", "quotation_no", "QT-2026-900008");
    String base = "/api/v1/quotations/" + revised;
    api.doGet("ao", base)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentVersion").value(2))
        .andExpect(jsonPath("$.content.items[0].label").value("YAQ5521"))
        .andExpect(jsonPath("$.content.premium.grossPremium").value(25037.75));
    api.doGet("ao", base + "/versions")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].frozen").value(true))
        .andExpect(jsonPath("$[1].frozen").value(false));
    api.doGet("ao", base + "/versions/1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.validUntil").value("2026-10-05"));
    api.doGet("ao", base + "/diff?from=1&to=2")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fields[*].field", Matchers.hasItem("Valid until")))
        .andExpect(jsonPath("$.grossDelta").value(0.0));
    api.doGet("ao", base + "/document.pdf")
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"));
    api.doGet("ao", base + "/document.xlsx").andExpect(status().isOk());
    Long request = id("quo_request", "request_no", "REQ-2026-900002");
    api.doGet("ao", "/api/v1/quotation-requests/" + request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.prospectName").value("Dizon, Paolo Miguel"));
  }

  @Test
  void quotationIsCreatedPricedAndSubmittedOverHttp() throws Exception {
    Map<String, Object> body =
        Map.of(
            "companyId",
            data.company().getId(),
            "clientId",
            id("crm_client", "client_code", "CL-2026-000001"),
            "productCode",
            "PAR01",
            "marketSegment",
            "CBG",
            "insurerCode",
            "INS-MGIC",
            "insurerBranch",
            "MKT",
            "periodFrom",
            "2026-12-01",
            "periodTo",
            "2027-12-01",
            "items",
            List.of(
                Map.of(
                    "riskGroup",
                    1,
                    "item",
                    Map.of(
                        "location",
                        Map.of(
                            "address",
                            "7 Api Street " + System.nanoTime(),
                            "city",
                            "Makati",
                            "insuredItems",
                            List.of(Map.of("description", "Building", "sumInsured", 3000000)))))));
    api.doPost("ao", "/api/v1/quotations/preview", body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.rated").value(true));
    JsonNode created =
        api.read(api.doPost("ao", "/api/v1/quotations", body).andExpect(status().isCreated()));
    String url = "/api/v1/quotations/" + created.get("id").asText();
    api.doPut("ao", url, body).andExpect(status().isOk());
    api.doPost("ao", url + "/submit", Map.of("comment", "ok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FOR_REVIEW"));
    api.doPost("ao", url + "/approve", Map.of()).andExpect(status().isForbidden());
    api.doPost("mkttl", url + "/approve", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    api.doPost("ao", url + "/revise", Map.of()).andExpect(jsonPath("$.status").value("DRAFT"));
    api.doPost("ao", url + "/submit", Map.of()).andExpect(status().isOk());
    api.doPost("approver", url + "/approve", Map.of()).andExpect(status().isOk());
    api.doPost(
            "ao",
            url + "/send",
            Map.of("to", List.of("client@example.ph"), "subject", "Quotation", "body", "Hello"))
        .andExpect(jsonPath("$.status").value("SENT_TO_CLIENT"));
    api.doPost("ao", url + "/accept", Map.of())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ACCEPTANCE_EMAIL_REQUIRED"));
    api.doPost("ao", url + "/decline", Map.of("comment", "no"))
        .andExpect(jsonPath("$.status").value("NOT_PROCEEDED"));
    api.doPost("ao", "/api/v1/quotations/batch-send", Map.of("ids", List.of(created.get("id"))))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void requestsAreCapturedAndClosedOverHttp() throws Exception {
    JsonNode request =
        api.read(
            api.doPost(
                    "ao",
                    "/api/v1/quotation-requests",
                    Map.of(
                        "companyId",
                        data.company().getId(),
                        "channel",
                        "EMAIL",
                        "prospectName",
                        "Httpson, Mara",
                        "requestedCover",
                        "Motor cover"))
                .andExpect(status().isCreated()));
    String url = "/api/v1/quotation-requests/" + request.get("id").asText();
    api.doPost("ao", url + "/close", Map.of("reason", "Duplicate of an e-mail"))
        .andExpect(jsonPath("$.status").value("CLOSED"));
    api.doPost("ao", url + "/prospect", Map.of()).andExpect(status().isUnprocessableEntity());
  }

  @Test
  void seedProposalReadsAndTsuEndpoints() throws Exception {
    Long terms = id("npk_proposal", "prf_no", "PRF-2026-900004");
    String base = "/api/v1/proposals/" + terms;
    api.doGet("ao2", base)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("TERMS_RECEIVED"))
        .andExpect(jsonPath("$.slips.qsNo").value("QS-2026-900001"))
        .andExpect(jsonPath("$.sections.length()").value(2));
    api.doGet("tsu", base + "/checklist").andExpect(status().isOk());
    api.doGet("tsu", base + "/responses")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
    api.doGet("tsu", base + "/responses/history").andExpect(status().isOk());
    api.doGet("tsu", base + "/comparative")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recommendedInsurer").value("INS-LAC"))
        .andExpect(jsonPath("$.rows[0].insurerCode").value("INS-LAC"));
    api.doGet("tsu", base + "/comparative.pdf").andExpect(status().isOk());
    api.doGet("tsu", base + "/comparative.xlsx").andExpect(status().isOk());
    api.doGet("tsu", base + "/quotation-slip.pdf").andExpect(status().isOk());
    Long accepted = id("npk_proposal", "prf_no", "PRF-2026-900005");
    api.doGet("ao2", "/api/v1/proposals/" + accepted + "/proposal-slip.pdf")
        .andExpect(status().isOk());
    api.doGet("ao2", "/api/v1/proposals/" + accepted)
        .andExpect(jsonPath("$.status").value("ACCEPTED"))
        .andExpect(jsonPath("$.slips.chosenInsurer").value("INS-MPI"));
    api.doGet("epol", base).andExpect(status().isForbidden());

    Long response =
        jdbc.queryForObject(
            "select id from npk_insurer_response where proposal_id = ? and insurer_code = 'INS-MGIC'",
            Long.class,
            terms);
    api.doPut(
            "tsu",
            base + "/responses/" + response,
            Map.of("status", "RECEIVED", "premium", 126000, "rate", 0.1575))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.premium").value(126000));
    mvc.perform(
            multipart(base + "/responses/" + response + "/document")
                .file(
                    new MockMultipartFile(
                        "file",
                        "terms.pdf",
                        "application/pdf",
                        "%PDF-1.4 t".getBytes(StandardCharsets.US_ASCII)))
                .with(user(users.loadUserByUsername("tsu"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentId").isNumber());
    api.doPost("tsu", base + "/responses/" + response + "/recommend", Map.of())
        .andExpect(jsonPath("$.recommended").value(true));
  }

  @Test
  void proposalIsCreatedAndWalkedToTsuOverHttp() throws Exception {
    Map<String, Object> body =
        Map.of(
            "companyId",
            data.company().getId(),
            "clientId",
            id("crm_client", "client_code", "CL-2026-000004"),
            "productCode",
            "EEI01",
            "marketSegment",
            "COMBANK",
            "sections",
            List.of(Map.of("heading", "Equipment", "text", "Servers")),
            "items",
            List.of(
                Map.of(
                    "riskGroup",
                    1,
                    "risk",
                    Map.of("description", "Data centre servers", "sumInsured", 9000000))),
            "insurers",
            List.of("INS-MGIC"));
    JsonNode created =
        api.read(api.doPost("ao", "/api/v1/proposals", body).andExpect(status().isCreated()));
    String url = "/api/v1/proposals/" + created.get("id").asText();
    api.doPut("ao", url, body).andExpect(status().isOk());
    api.doPost("ao", url + "/submit", Map.of())
        .andExpect(jsonPath("$.status").value("FOR_MKT_APPROVAL"));
    api.doPost("mkttl", url + "/approve", Map.of())
        .andExpect(jsonPath("$.status").value("WITH_TSU"));
    api.doPut("tsu", url + "/insurers", Map.of("insurers", List.of("INS-MGIC", "INS-MPI")))
        .andExpect(jsonPath("$.insurers.length()").value(2));
    api.doPost("tsu", url + "/quotation-slip/submit", Map.of())
        .andExpect(status().isUnprocessableEntity());
  }
}
