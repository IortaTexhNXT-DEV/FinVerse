package com.iortatechnxt.brokerverse.api;

import static com.iortatechnxt.brokerverse.screening.ScreeningMatchingFixtures.word;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.screening.ScreeningMatchingFixtures;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sanction Screening (BRD-10) end to end through the HTTP API with the demo users (wave S2): the
 * Compliance Officer lists an invented name and the checker approves it; the account officer
 * registers a client with that name, which screens the client, records the match, tags the client
 * for watchlist review and opens the case; the UCC assigns it, the investigator completes the
 * review, uploads the KYC form and submits; the unit head concurs, Compliance escalates, the AML
 * Committee approves an STR by majority, Compliance prepares it, extracts it and files it with the
 * AMLC reference, which closes the case. The client's Screening tab and the compliance reports show
 * the outcome. A second case shows the SLA reminder and breach of the monitor job run from the job
 * monitor (SNSRP-201-204, 301-304, 401-405, 501-502, 601-602, 701-706, 802, 901).
 */
@IntegrationTest
class ScreeningEndToEndApiIT {

  private static final String BASE = "/api/v1/screening";
  private static final String MAKER = "compoff";
  private static final String CHECKER = "compchk";
  private static final String UCC = "ucc";
  private static final String INVESTIGATOR = "investigator";
  private static final String UNIT_HEAD = "scrapprover";
  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
  private static final AtomicInteger SEQ = new AtomicInteger();

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;

  private static String today() {
    return LocalDate.now(MANILA).toString();
  }

  private String caseUrl(long caseId) {
    return BASE + "/cases/" + caseId;
  }

  @Test
  void aListedClientRunsFromRegistrationToTheFiledStr() throws Exception {
    String name = word() + " " + word();
    listName(name);
    long clientId = register(name);

    // Screening on registration: the match, the risk profile and the watchlist-review tag.
    api.doGet(UCC, BASE + "/clients/" + clientId + "/matches")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].status").value("POTENTIAL"))
        .andExpect(jsonPath("$[0].listType").value("INTERNAL"));
    api.doGet(UCC, BASE + "/clients/" + clientId + "/risk-profile")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].source").value("RULE"));
    api.doGet(UCC, "/api/v1/crm/clients/" + clientId + "/instructions")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tags[*].code", hasItem("WATCHLIST_REVIEW")));
    api.doGet(UCC, "/api/v1/crm/clients/" + clientId)
        .andExpect(jsonPath("$.profile.riskRating").value("HIGH"));

    // The case is open in INVESTIGATION; the UCC assigns it to the investigator.
    JsonNode opened = api.read(api.doGet(UCC, BASE + "/clients/" + clientId + "/cases"));
    assertThat(opened.size()).isEqualTo(1);
    long caseId = opened.get(0).get("id").asLong();
    String caseNo = opened.get(0).get("caseNo").asText();
    assertThat(caseNo).matches("SCR-\\d{4}-\\d{6}");
    assertThat(opened.get(0).get("stage").asText()).isEqualTo("INVESTIGATION");
    toInvestigator(caseId);

    investigate(caseId);
    approveAndEscalate(caseId);
    committeeApproves(caseId);
    String strNo = prepareExtractAndFile(caseId, caseNo);

    // Client record, Screening tab: the case is closed; the reports show it.
    api.doGet(UCC, BASE + "/clients/" + clientId + "/cases")
        .andExpect(jsonPath("$[0].caseNo").value(caseNo))
        .andExpect(jsonPath("$[0].stage").value("CLOSED"))
        .andExpect(jsonPath("$[0].status").value("CLOSED"));
    api.doGet(UCC, caseUrl(caseId) + "/timeline")
        .andExpect(
            jsonPath(
                "$[*].event",
                org.hamcrest.Matchers.hasItems(
                    "CREATED", "SUBMITTED", "ESCALATED", "COMMITTEE_DECISION", "FILED")));
    assertThat(report("SCR-CASE-STATUS")).contains(caseNo);
    assertThat(report("SCR-STR-REGISTER")).contains(strNo);
    String clientCode =
        jdbc.queryForObject(
            "select coalesce(client_code, prospect_code) from crm_client where id = ?",
            String.class,
            clientId);
    assertThat(report("SCR-HIGH-RISK-CLIENTS")).contains(clientCode);
  }

  @Test
  void theSlaMonitorJobRemindsAndEscalatesABreach() throws Exception {
    String name = word() + " " + word();
    listName(name);
    long clientId = register(name);
    JsonNode opened = api.read(api.doGet(UCC, BASE + "/clients/" + clientId + "/cases"));
    long caseId = opened.get(0).get("id").asLong();
    String caseNo = opened.get(0).get("caseNo").asText();
    toInvestigator(caseId);

    jdbc.update(
        "update scr_case set due_at = now() + interval '2 hours',"
            + " remind_at = now() - interval '1 hour', reminded_at = null, breached = false"
            + " where id = ?",
        caseId);
    runSlaMonitor();
    api.doGet(UCC, caseUrl(caseId) + "/timeline")
        .andExpect(jsonPath("$[*].event", hasItem("REMINDER")));
    api.doGet(UCC, caseUrl(caseId)).andExpect(jsonPath("$.breached").value(false));

    jdbc.update("update scr_case set due_at = now() - interval '1 hour' where id = ?", caseId);
    runSlaMonitor();
    api.doGet(UCC, caseUrl(caseId))
        .andExpect(jsonPath("$.breached").value(true))
        .andExpect(jsonPath("$.row.slaState").value("BREACHED"));
    api.doGet(UCC, caseUrl(caseId) + "/timeline")
        .andExpect(jsonPath("$[*].event", hasItem("BREACH")));
    api.doGet(
            UCC,
            BASE
                + "/cases?companyId="
                + data.company().getId()
                + "&tab=ALL&sla=BREACHED&q="
                + caseNo)
        .andExpect(jsonPath("$.content[0].caseNo").value(caseNo));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where exception_code = 'SCR_SLA_BREACH'"
                    + " and entity_id = ?",
                Integer.class,
                caseNo))
        .isEqualTo(1);
    assertThat(report("SCR-SLA-BREACHES")).contains(caseNo);
  }

  private void runSlaMonitor() throws Exception {
    api.doPost("admin", "/api/v1/system/jobs/SCR_SLA_MONITOR/run", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jobName").value("SCR_SLA_MONITOR"))
        .andExpect(jsonPath("$.status").value("SUCCEEDED"));
  }

  /** The maker lists the name on the internal list; the checker approves it (SNSRP-203, 204). */
  private void listName(String name) throws Exception {
    JsonNode change =
        api.read(
            api.doPost(
                    MAKER,
                    BASE + "/watchlist/entries",
                    Json.of(
                        "listType", "INTERNAL",
                        "entityType", "INDIVIDUAL",
                        "primaryName", name,
                        "nationality", "FILIPINO",
                        "remarks", "Internal list: end-to-end test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")));
    long changeId = change.get("id").asLong();
    api.doPost(MAKER, BASE + "/watchlist/changes/" + changeId + "/approve", Map.of())
        .andExpect(status().isForbidden());
    api.doPost(CHECKER, BASE + "/watchlist/changes/" + changeId + "/approve", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
  }

  /** The account officer registers the prospect, which screens it (SNSRP-602). */
  private long register(String name) throws Exception {
    String[] parts = name.split(" ");
    int n = SEQ.incrementAndGet();
    long tag = System.nanoTime() % 1_000_000L;
    String tin = String.format("8%02d-%03d-%03d-000", n % 100, tag % 1000, (tag / 1000) % 1000);
    Map<String, Object> body =
        Json.of(
            "companyId", data.company().getId(),
            "clientType", "INDIVIDUAL",
            "firstName", parts[0],
            "lastName", parts[1],
            "birthDate", "1984-05-12",
            "tin", tin,
            "idType", "PASSPORT",
            "idNumber", "E2E" + tag + n,
            "email", "e2e" + tag + n + "@test-client.ph",
            "mobile", String.format("0997%07d", (tag * 10 + n) % 10_000_000),
            "addressLine", "1 Test Street",
            "city", "Makati",
            "province", "Metro Manila",
            "postalCode", "1200",
            "marketSegment", "CBG",
            "nationality", "FILIPINO",
            "civilStatus", "SINGLE",
            "occupation", "Tester",
            "sourceOfFunds", "SALARY");
    return api.read(api.doPost("ao", "/api/v1/crm/clients", body).andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  private void toInvestigator(long caseId) throws Exception {
    JsonNode detail = api.read(api.doGet(UCC, caseUrl(caseId)));
    if (!INVESTIGATOR.equalsIgnoreCase(detail.get("row").get("assignee").asText())) {
      api.doGet(UCC, caseUrl(caseId) + "/eligible-assignees")
          .andExpect(jsonPath("$", hasItem(INVESTIGATOR)));
      api.doPost(
              UCC,
              caseUrl(caseId) + "/reassign",
              Json.of("assignee", INVESTIGATOR, "reasonCode", "WORKLOAD", "comment", "E2E"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.screeningCase.row.assignee").value(INVESTIGATOR));
    }
  }

  /** Review, KYC form and submission to the unit head (SNSRP-501, 502, 601, 701). */
  private void investigate(long caseId) throws Exception {
    api.doPut(
            INVESTIGATOR,
            caseUrl(caseId) + "/review",
            Json.of(
                "values",
                Map.of(
                    "IDENTITY_VERIFIED", "true",
                    "SOURCE_OF_FUNDS", "Salary from employment",
                    "MATCH_ASSESSMENT", "Same name as the internal entry",
                    "PROPOSED_RATING", "HIGH",
                    "RECOMMENDATION", "Escalate for enhanced review")))
        .andExpect(status().isOk());
    Map<String, Object> submit =
        Json.of(
            "disposition", "TRUE_MATCH_REVIEW",
            "recommendation", "Same person; escalate",
            "strRequired", true);
    api.doPost(INVESTIGATOR, caseUrl(caseId) + "/submit", submit)
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(
            multipart(caseUrl(caseId) + "/documents")
                .file(
                    new MockMultipartFile(
                        "file", "kyc form.pdf", "application/pdf", ScreeningMatchingFixtures.PDF))
                .param("formType", "KYC_REVIEW")
                .param("documentType", "KYC_FORM")
                .param("dateReceived", LocalDate.now(MANILA).minusDays(1).toString())
                .param("source", "Branch")
                .with(user(users.loadUserByUsername(INVESTIGATOR)))
                .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nominatedName").value(org.hamcrest.Matchers.startsWith("KYC-")));
    api.doPost(INVESTIGATOR, caseUrl(caseId) + "/submit", submit)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.screeningCase.row.stage").value("UNIT_HEAD_APPROVAL"));
    api.doGet(UNIT_HEAD, "/api/v1/approvals/inbox")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].reference", hasItem(caseNoOf(caseId))));
  }

  /** The unit head concurs and Compliance escalates to the committee (SNSRP-702, 703). */
  private void approveAndEscalate(long caseId) throws Exception {
    api.doPost(INVESTIGATOR, caseUrl(caseId) + "/decision", Json.of("disposition", "CONCUR"))
        .andExpect(status().isForbidden());
    api.doPost(
            UNIT_HEAD,
            caseUrl(caseId) + "/decision",
            Json.of("disposition", "CONCUR", "remarks", "Agree with the investigator"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.screeningCase.row.stage").value("COMPLIANCE_REVIEW"));
    api.doPost(
            MAKER,
            caseUrl(caseId) + "/outcome",
            Json.of("disposition", "ESCALATE_COMMITTEE", "remarks", "For the committee"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.screeningCase.row.stage").value("AML_COMMITTEE"));
  }

  /** Three of the five members approve the STR: MAJORITY finalises (SNSRP-704). */
  private void committeeApproves(long caseId) throws Exception {
    for (String member : List.of("amlcom1", "amlcom2", "amlcom3")) {
      api.doPost(
              member,
              caseUrl(caseId) + "/votes",
              Json.of("decision", "APPROVE_STR", "remarks", "Supports the STR"))
          .andExpect(status().isOk());
    }
    api.doGet(UCC, caseUrl(caseId) + "/votes").andExpect(jsonPath("$.length()").value(3));
    api.doGet(UCC, caseUrl(caseId))
        .andExpect(jsonPath("$.row.stage").value("STR_PREPARATION"))
        .andExpect(jsonPath("$.committeeDecision").value("APPROVE_STR"));
  }

  /** STR prepared, marked ready, extracted and filed with the AMLC reference (SNSRP-705, 706). */
  private String prepareExtractAndFile(long caseId, String caseNo) throws Exception {
    JsonNode prepared =
        api.read(
            api.doPost(MAKER, BASE + "/cases/" + caseId + "/str", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.str.status").value("DRAFT")));
    long strId = prepared.get("str").get("id").asLong();
    String strNo = prepared.get("str").get("strNo").asText();
    api.doPut(
            MAKER,
            BASE + "/str/" + strId,
            Json.of(
                "values", Map.of("NARRATIVE", "Cash premium from a listed person"),
                "reasonCodes", List.of("DEMO01"),
                "transactions",
                    List.of(
                        Json.of(
                            "reference", "OR-E2E-" + caseId,
                            "date", today(),
                            "amount", "50000.00",
                            "currency", "PHP",
                            "type", "RECEIPT",
                            "description", "Cash"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gaps").isEmpty());
    api.doPost(MAKER, BASE + "/str/" + strId + "/ready", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.str.status").value("APPROVED"));
    api.doGet(UCC, caseUrl(caseId)).andExpect(jsonPath("$.row.stage").value("STR_EXTRACTION"));

    Long company = data.company().getId();
    long extractionId =
        api.read(
                api.doPost(
                        MAKER,
                        BASE + "/str/extractions",
                        Json.of("companyId", company, "from", today(), "to", today()))
                    .andExpect(status().isOk()))
            .get("id")
            .asLong();
    String file =
        api.doGet(MAKER, BASE + "/str/extractions/" + extractionId + "/file")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(file).contains(strNo);
    api.doGet(UCC, BASE + "/str/extractions/" + extractionId + "/file")
        .andExpect(status().isForbidden());
    api.doPost(
            MAKER,
            BASE + "/str/" + strId + "/filing",
            Json.of("reference", "AMLC-" + caseNo, "filedOn", today()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FILED"));
    api.doGet(MAKER, BASE + "/str?companyId=" + company + "&status=FILED")
        .andExpect(jsonPath("$.content[*].strNo", hasItem(strNo)));
    return strNo;
  }

  private String caseNoOf(long caseId) {
    return jdbc.queryForObject("select case_no from scr_case where id = ?", String.class, caseId);
  }

  /** Runs a compliance report as the Compliance Officer and returns the response body. */
  private String report(String code) throws Exception {
    return api.doPost(
            MAKER,
            "/api/v1/reports/" + code + "/run",
            Map.of(
                "companyId", String.valueOf(data.company().getId()),
                "fromDate", LocalDate.now(MANILA).minusDays(1).toString(),
                "toDate", LocalDate.now(MANILA).plusDays(1).toString(),
                "asOfDate", LocalDate.now(MANILA).plusDays(1).toString()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
  }
}
