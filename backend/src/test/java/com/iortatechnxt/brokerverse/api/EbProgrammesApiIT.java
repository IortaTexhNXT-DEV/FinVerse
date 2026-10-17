package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HTTP contract of the Employee Benefits waves E1-B and E1-C: programmes, cycles and their steps,
 * feedback, documents, BOR, accounts, activity, EB Home, Send RA and Pending Items, with the
 * permissions of the EB roles.
 */
@IntegrationTest
class EbProgrammesApiIT {

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ClientService clients;
  @Autowired private TestData data;

  private String c() {
    return "companyId=" + data.company().getId();
  }

  private Map<String, Object> programme(String name, boolean eligible) {
    Long clientId = clients.requireByCode(data.company().getId(), "CL-2026-000004").getId();
    return Map.of(
        "clientId",
        clientId,
        "profile",
        Map.of("name", name, "teamCode", "SM", "funding", "EMPLOYER", "renewalEligible", eligible),
        "lines",
        List.of(
            Map.of(
                "benefitLine",
                "HMO",
                "incumbentInsurer",
                "INS-VMI",
                "currentPolicyNo",
                "HMO-API-1",
                "periodFrom",
                LocalDate.now().minusDays(300).toString(),
                "periodTo",
                LocalDate.now().plusDays(65).toString(),
                "headcount",
                90)),
        "contacts",
        List.of(
            Map.of(
                "name",
                "Api Hr",
                "email",
                "api.hr@client.example",
                "role",
                "HR_HEAD",
                "receivesRa",
                true)));
  }

  private ResultActions multipartAs(
      String user, String url, MockMultipartFile file, Map<String, String> params)
      throws Exception {
    var request = multipart(url).file(file);
    params.forEach(request::param);
    return mvc.perform(request.with(user(users.loadUserByUsername(user))).with(csrf()));
  }

  private static MockMultipartFile pdf(String field, String name) {
    return new MockMultipartFile(
        field,
        name,
        "application/pdf",
        ("%PDF-1.4 " + System.nanoTime()).getBytes(StandardCharsets.US_ASCII));
  }

  @Test
  void aProgrammeIsCreatedReadAndMaintainedOverHttp() throws Exception {
    String name = "Api programme " + System.nanoTime();
    JsonNode created =
        api.read(
            api.doPost("ebao", "/api/v1/eb/programmes?" + c(), programme(name, true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programmeNo").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.lines[0].benefitLine").value("HMO")));
    long id = created.get("id").asLong();
    String base = "/api/v1/eb/programmes/" + id + "?" + c();
    api.doGet("ebtl", base).andExpect(status().isOk()).andExpect(jsonPath("$.name").value(name));
    api.doGet(
            "ebcoll",
            "/api/v1/eb/programmes?"
                + c()
                + "&tab=RENEWAL_DUE&q="
                + created.get("programmeNo").asText())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(id));
    api.doPost("ebcoll", "/api/v1/eb/programmes?" + c(), programme("No right", true))
        .andExpect(status().isForbidden());
    api.doPost("ebao", "/api/v1/eb/programmes?" + c(), Map.of("profile", Map.of("name", "x")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("EB_CLIENT_REQUIRED"));

    api.doPut(
            "ebao",
            base,
            Map.of(
                "name",
                name + " (renamed)",
                "teamCode",
                "SM",
                "funding",
                "EMPLOYER",
                "renewalEligible",
                true))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(name + " (renamed)"));
    api.doPost(
            "ebao",
            "/api/v1/eb/programmes/" + id + "/lines?" + c(),
            Map.of("benefitLine", "GLI", "headcount", 90))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lines.length()").value(2));
    api.doPut(
            "ebao",
            "/api/v1/eb/programmes/" + id + "/lines/2?" + c(),
            Map.of("benefitLine", "GLI", "incumbentInsurer", "INS-LAC", "headcount", 95))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lines[1].headcount").value(95));
    api.doDelete("ebao", "/api/v1/eb/programmes/" + id + "/lines/2?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lines[1].active").value(false));
    JsonNode withContact =
        api.read(
            api.doPost(
                    "ebao",
                    "/api/v1/eb/programmes/" + id + "/contacts?" + c(),
                    Map.of(
                        "name",
                        "Fin",
                        "email",
                        "fin@client.example",
                        "role",
                        "FINANCE",
                        "receivesSoa",
                        true))
                .andExpect(status().isOk()));
    long contactId = withContact.get("contacts").get(1).get("id").asLong();
    api.doPut(
            "ebao",
            "/api/v1/eb/programmes/" + id + "/contacts/" + contactId + "?" + c(),
            Map.of("name", "Finance", "email", "fin@client.example", "role", "FINANCE"))
        .andExpect(status().isOk());
    api.doDelete("ebao", "/api/v1/eb/programmes/" + id + "/contacts/" + contactId + "?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contacts[1].active").value(false));

    JsonNode sent =
        api.read(
            api.doPost(
                    "ebao",
                    "/api/v1/eb/programmes/send-ra?" + c(),
                    Map.of("programmeIds", List.of(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sent").value(true)));
    assertThat(sent.get(0).get("cycleNo").asText()).startsWith("EBC-");
    JsonNode page = api.read(api.doGet("ebao", base));
    long cycleId = page.get("currentCycleId").asLong();
    assertThat(page.get("cycles").get(0).get("stage").asText()).isEqualTo("RA_SENT");
    assertThat(page.get("cycles").get(0).get("renewalAdvice").get("recipients").get(0).asText())
        .isEqualTo("api.hr@client.example");

    multipartAs(
            "ebao",
            "/api/v1/eb/cycles/" + cycleId + "/feedback?" + c(),
            pdf("files", "feedback.pdf"),
            Map.of(
                "channel",
                "EMAIL",
                "receivedOn",
                LocalDate.now().minusDays(1).toString(),
                "text",
                "Renew"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fileCount").value(1));
    api.doGet("ebao", "/api/v1/eb/programmes/" + id + "/feedback?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].channel").value("EMAIL"));
    api.doPost("ebao", "/api/v1/eb/cycles/" + cycleId + "/stay-with-incumbent?" + c(), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycles[0].stage").value("INCUMBENT_TERMS"));
    api.doGet("ebao", "/api/v1/eb/programmes/" + id + "/activity?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].activity").value("RENEWAL_ADVICE"));
    api.doGet("ebao", "/api/v1/eb/programmes/" + id + "/accounts?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void documentsBorAndCyclesOfANewBusinessOverHttp() throws Exception {
    JsonNode created =
        api.read(
            api.doPost(
                    "ebao2",
                    "/api/v1/eb/programmes?" + c(),
                    programme("Api NB " + System.nanoTime(), false))
                .andExpect(status().isCreated()));
    long id = created.get("id").asLong();
    JsonNode opened =
        api.read(
            api.doPost(
                    "ebao2",
                    "/api/v1/eb/programmes/" + id + "/cycles?" + c(),
                    Map.of("businessType", "NEW_BUSINESS", "policyYear", 2031))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cycles[0].businessType").value("NEW_BUSINESS")));
    long cycleId = opened.get("currentCycleId").asLong();
    api.doPost("ebao2", "/api/v1/eb/cycles/" + cycleId + "/start?" + c(), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycles[0].stage").value("REQUIREMENTS"));
    api.doPost("ebao2", "/api/v1/eb/cycles/" + cycleId + "/remarket?" + c(), null)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("EB_BOR_REQUIRED"));

    multipartAs(
            "ebao2",
            "/api/v1/eb/cycles/" + cycleId + "/documents?" + c(),
            pdf("files", "census.pdf"),
            Map.of(
                "documentType",
                "EB_MASTERLIST_UNNAMED",
                "processType",
                "PROPOSAL",
                "source",
                "INSURER"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$[0].documentType").value("EB_MASTERLIST_UNNAMED"))
        .andExpect(jsonPath("$[0].source").value("INSURER"));
    multipartAs(
            "ebao2",
            "/api/v1/eb/cycles/" + cycleId + "/documents?" + c(),
            pdf("files", "census.pdf"),
            Map.of("documentType", "EB_TOR"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("EB_PROCESS_REQUIRED"));
    api.doGet("ebtl", "/api/v1/eb/programmes/" + id + "/documents?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    JsonNode bor =
        api.read(
            multipartAs(
                    "ebao2",
                    "/api/v1/eb/cycles/" + cycleId + "/bor?" + c(),
                    pdf("file", "bor.pdf"),
                    Map.of())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UPLOADED")));
    api.doPost(
            "ebao2",
            "/api/v1/eb/bor/" + bor.get("id").asLong() + "/validate?" + c(),
            Map.of(
                "signedBySignatory",
                true,
                "notBlank",
                true,
                "clientNameMatches",
                true,
                "validFrom",
                "2026-01-01",
                "validTo",
                "2099-01-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("VALIDATED"));
    api.doGet("ebao2", "/api/v1/eb/programmes/" + id + "/bor?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].versionNo").value(1));
    api.doPost("ebao2", "/api/v1/eb/cycles/" + cycleId + "/remarket?" + c(), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycles[0].stage").value("FRANCHISE"));
    JsonNode second =
        api.read(
            api.doPost(
                    "ebao2",
                    "/api/v1/eb/programmes/" + id + "/cycles?" + c(),
                    Map.of("businessType", "NEW_BUSINESS", "policyYear", 2032))
                .andExpect(status().isCreated()));
    long other = second.get("cycles").get(0).get("id").asLong();
    JsonNode rejected =
        api.read(
            multipartAs(
                    "ebao2",
                    "/api/v1/eb/cycles/" + other + "/bor?" + c(),
                    pdf("file", "bor.pdf"),
                    Map.of())
                .andExpect(status().isCreated()));
    api.doPost(
            "ebproc",
            "/api/v1/eb/bor/" + rejected.get("id").asLong() + "/reject?" + c(),
            Map.of("reason", "Blank page"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rejectReason").value("Blank page"));
  }

  @Test
  void homeAndPendingItemsOverHttp() throws Exception {
    api.doGet("ebao", "/api/v1/eb/home?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.raDue").isNumber())
        .andExpect(jsonPath("$.pendingItemsOverdue").isNumber());
    JsonNode created =
        api.read(
            api.doPost(
                    "ebao",
                    "/api/v1/eb/programmes?" + c(),
                    programme("Api items " + System.nanoTime(), true))
                .andExpect(status().isCreated()));
    long id = created.get("id").asLong();
    String member = "EMP-API-" + System.nanoTime();
    JsonNode item =
        api.read(
            api.doPost(
                    "ebao",
                    "/api/v1/eb/pending-items?" + c(),
                    Map.of(
                        "programmeId",
                        id,
                        "itemType",
                        "HMO_CARD",
                        "subject",
                        "Card of " + member,
                        "memberRef",
                        member,
                        "responsible",
                        "INSURER",
                        "partyCode",
                        "INS-VMI",
                        "dueDate",
                        LocalDate.now().plusDays(10).toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")));
    long itemId = item.get("id").asLong();
    api.doPost("ebao", "/api/v1/eb/pending-items?" + c(), Map.of("itemType", "HMO_CARD"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("EB_PROGRAMME_REQUIRED"));
    api.doPut(
            "ebproc",
            "/api/v1/eb/pending-items/" + itemId + "?" + c(),
            Map.of(
                "subject",
                "Card of " + member,
                "memberRef",
                member,
                "responsible",
                "INSURER",
                "dueDate",
                LocalDate.now().plusDays(12).toString()))
        .andExpect(status().isOk());
    api.doPost(
            "ebproc",
            "/api/v1/eb/pending-items/" + itemId + "/status?" + c(),
            Map.of("action", "RECEIVE", "date", LocalDate.now().minusDays(1).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RECEIVED"));
    api.doPost("ebproc", "/api/v1/eb/pending-items/" + itemId + "/status?" + c(), Map.of())
        .andExpect(status().isUnprocessableEntity());
    api.doGet("ebcoll", "/api/v1/eb/pending-items?" + c() + "&member=" + member)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(itemId));
    api.doPost("ebcoll", "/api/v1/eb/pending-items?" + c(), Map.of("programmeId", id))
        .andExpect(status().isForbidden());
  }
}
