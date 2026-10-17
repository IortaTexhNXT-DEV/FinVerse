package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.EbFixtures;
import com.iortatechnxt.brokerverse.eb.bor.service.BorService;
import com.iortatechnxt.brokerverse.eb.cycle.service.CycleService;
import com.iortatechnxt.brokerverse.eb.domain.EbBor;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.Clock;
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
 * HTTP contract of the Employee Benefits marketing and servicing steps: franchise, TOR, insurer
 * requests, proposals (multipart with the insurer's document), comparative, confirmation and
 * Trigger Placement, submissions, EB Setup with maker-checker, roster, member changes and SOA, with
 * the permissions of the EB roles.
 */
@IntegrationTest
class EbMarketApiIT {

  private static final String AO = "ebao";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private EbFixtures fx;
  @Autowired private CycleService cycles;
  @Autowired private BorService bor;
  @Autowired private AsUser as;
  @Autowired private ObjectMapper json;
  @Autowired private Clock clock;

  private String c() {
    return "companyId=" + fx.company();
  }

  private ResultActions multipartAs(
      String user, String url, List<MockMultipartFile> files, Map<String, String> params)
      throws Exception {
    var request = multipart(url);
    files.forEach(request::file);
    params.forEach(request::param);
    return mvc.perform(request.with(user(users.loadUserByUsername(user))).with(csrf()));
  }

  private static MockMultipartFile pdf(String field, String name) {
    return new MockMultipartFile(
        field, name, "application/pdf", EbFixtures.realPdf(name).content());
  }

  private EbCycle franchiseCycle(EbProgramme p) {
    EbCycle cycle = fx.cycle(p, BusinessType.NEW_BUSINESS, 2027);
    as.run(AO, () -> cycles.start(fx.company(), cycle.getId()));
    EbBor version =
        as.run(AO, () -> bor.upload(fx.company(), cycle.getId(), EbFixtures.realPdf("bor.pdf")));
    LocalDate today = BusinessClock.today(clock);
    as.run(
        "ebtl",
        () ->
            bor.validate(
                fx.company(),
                version.getId(),
                new EbBor.Checklist(true, true, true, today.minusDays(1), today.plusYears(1))));
    return cycle;
  }

  @Test
  void aNewBusinessIsMarketedComparedConfirmedAndPlacedOverHttp() throws Exception {
    EbProgramme p =
        fx.programme(
            false,
            List.of(new EbProgrammeLine.Data("HMO", null, null, null, null, null, null, 50)));
    EbCycle cycle = franchiseCycle(p);
    String cy = "/api/v1/eb/cycles/" + cycle.getId();
    api.doPost(AO, cy + "/remarket?" + c(), Map.of()).andExpect(status().isOk());
    api.doPost("ebcoll", cy + "/franchise?" + c(), Map.of("insurerCodes", List.of("INS-MGIC")))
        .andExpect(status().isForbidden());
    JsonNode franchise =
        api.read(
            api.doPost(AO, cy + "/franchise?" + c(), Map.of("insurerCodes", List.of("INS-MGIC")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$[0].insurerName").value("Mabuhay General Insurance Corp.")));
    multipartAs(
            AO,
            "/api/v1/eb/franchise/" + franchise.get(0).get("id").asLong() + "/decision?" + c(),
            List.of(pdf("file", "reply.pdf")),
            Map.of("approve", "true", "remarks", "Approved"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    api.doGet("ebtl", "/api/v1/eb/programmes/" + p.getId() + "/franchise?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].decision").value("APPROVED"));

    api.doPut(
            AO,
            cy + "/tor?" + c(),
            Map.of(
                "items",
                List.of(
                    Map.of(
                        "benefitLine",
                        "HMO",
                        "description",
                        "Annual limit",
                        "requirement",
                        "PHP 150,000"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT"));
    JsonNode tor =
        api.read(api.doPost(AO, cy + "/tor/release?" + c(), Map.of()).andExpect(status().isOk()));
    long torItem = tor.get("items").get(0).get("id").asLong();
    api.doPost(AO, cy + "/requests?" + c(), Map.of("insurerCodes", List.of("INS-MGIC")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$[0].status").value("OPEN"));

    String proposal =
        json.writeValueAsString(
            Map.of(
                "insurerCode", "INS-MGIC",
                "lines",
                    List.of(
                        Map.of(
                            "benefitLine",
                            "HMO",
                            "planCode",
                            "PLAN-A",
                            "annualPremium",
                            750000,
                            "members",
                            50)),
                "items",
                    List.of(
                        Map.of(
                            "torItemId",
                            torItem,
                            "offeredValue",
                            "PHP 150,000",
                            "deviation",
                            false)),
                "factors",
                    List.of(
                        Map.of("factorCode", "TECHNOLOGY", "value", "Mobile app", "rating", 5))));
    JsonNode recorded =
        api.read(
            multipartAs(
                    AO,
                    cy + "/proposals?" + c(),
                    List.of(pdf("file", "proposal.pdf")),
                    Map.of("proposal", proposal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("PROPOSAL"))
                .andExpect(jsonPath("$.totalPremium").value(750000.0)));
    long proposalId = recorded.get("id").asLong();
    api.doPost(AO, "/api/v1/eb/proposals/" + proposalId + "/validate?" + c(), Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("VALIDATED"));

    JsonNode comparative =
        api.read(
            api.doPost(AO, cy + "/comparatives?" + c(), Map.of())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matrix.proposals[0].insurerCode").value("INS-MGIC"))
                .andExpect(jsonPath("$.lines[0].recommendedProposalId").value(proposalId)));
    String cp = "/api/v1/eb/comparatives/" + comparative.get("comparative").get("id").asLong();
    api.doPost(AO, cp + "/submit?" + c(), Map.of()).andExpect(status().isOk());
    api.doPost(AO, cp + "/sign-off?" + c(), Map.of()).andExpect(status().isForbidden());
    api.doPost("ebtl", cp + "/sign-off?" + c(), Map.of("remarks", "Agreed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comparative.status").value("APPROVED"))
        .andExpect(jsonPath("$.decisions[0].signatory").value("ebtl"));
    api.download(AO, cp + "/export?format=xlsx&" + c()).andExpect(status().isOk());
    api.doPost(AO, cp + "/present?" + c(), Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycleStage").value("WITH_CLIENT"));
    api.doPost(AO, cp + "/comments?" + c(), Map.of("text", "Client agrees", "client", true))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.authorKind").value("CLIENT"));

    String confirmation =
        json.writeValueAsString(
            Map.of(
                "channel",
                "SIGNED_DOCUMENT",
                "choices",
                List.of(Map.of("lineNo", 1, "proposalId", proposalId))));
    multipartAs(
            AO,
            cy + "/confirmation?" + c(),
            List.of(pdf("file", "signed.pdf")),
            Map.of("confirmation", confirmation))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    api.doPost(AO, cy + "/trigger-placement?" + c(), Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("SUBMITTED"))
        .andExpect(jsonPath("$[0].productCode").value("EBHMO01"));
    api.doGet(AO, cy + "/confirmations?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].lines[0].accountArn").exists());

    api.doGet(
            AO,
            "/api/v1/eb/submissions/checklist?"
                + c()
                + "&programmeId="
                + p.getId()
                + "&cycleId="
                + cycle.getId()
                + "&processType=NB_PLACEMENT")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].documentType").value("EB_CLIENT_CONFIRMATION"))
        .andExpect(jsonPath("$[0].present").value(true));
    api.doPost(
            AO,
            "/api/v1/eb/submissions?" + c(),
            Map.of(
                "programmeId", p.getId(),
                "cycleId", cycle.getId(),
                "processType", "NB_PLACEMENT",
                "insurerCode", "INS-MGIC",
                "remarks", "Placement documents"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.documents.length()").value(2));
    api.doGet("ebproc", "/api/v1/eb/programmes/" + p.getId() + "/submissions?" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].processType").value("NB_PLACEMENT"));
  }

  @Test
  void setUpChangesNeedAnotherUsersAuthorisation() throws Exception {
    JsonNode rule =
        api.read(
            api.doPost(
                    "badmin",
                    "/api/v1/eb/setup/threshold-rules?" + c(),
                    Map.of(
                        "benefitLine", "GLI",
                        "measure", "TSI",
                        "amount", 900000000,
                        "effectiveFrom", "2026-01-01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"))
                .andExpect(jsonPath("$.approverPermission").value("EB_THRESHOLD_APPROVE")));
    api.doPost(
            "badmin",
            "/api/v1/eb/setup/threshold-rules/" + rule.get("id").asLong() + "/authorize?" + c(),
            Map.of())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("MAKER_CHECKER_VIOLATION"));
    api.doPost(
            "badmin",
            "/api/v1/eb/setup/threshold-rules?" + c(),
            Map.of("measure", "TSI", "amount", 0, "effectiveFrom", "2026-01-01"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("EB_THRESHOLD_AMOUNT"));
    api.doPost(
            AO,
            "/api/v1/eb/setup/required-documents?" + c(),
            Map.of("processType", "PROPOSAL", "documentType", "EB_UTILIZATION", "mandatory", true))
        .andExpect(status().isForbidden());
    api.doPost(
            "badmin",
            "/api/v1/eb/setup/required-documents?" + c(),
            Map.of(
                "processType",
                "PROPOSAL",
                "benefitLine",
                "GPA",
                "documentType",
                "EB_UTILIZATION",
                "mandatory",
                true))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"));
  }

  @Test
  void anSoaIsRegisteredValidatedAndReleasedOverHttp() throws Exception {
    EbProgramme p = fx.programme(true, List.of(EbFixtures.hmo(LocalDate.of(2026, 12, 31), null)));
    JsonNode soa =
        api.read(
            multipartAs(
                    "ebproc",
                    "/api/v1/eb/programmes/" + p.getId() + "/soa?" + c(),
                    List.of(pdf("file", "soa.pdf")),
                    Map.of(
                        "insurerCode", "INS-MGIC",
                        "insurerSoaNo", "SOA-" + EbFixtures.token(),
                        "periodFrom", "2026-01-01",
                        "periodTo", "2026-03-31",
                        "amount", "98000.50"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED")));
    String s = "/api/v1/eb/soa/" + soa.get("id").asLong();
    api.doPost(AO, s + "/validate?" + c(), Map.of()).andExpect(status().isForbidden());
    api.doPost("ebproc", s + "/validate?" + c(), Map.of()).andExpect(status().isOk());
    api.doPost("ebproc", s + "/release?" + c(), Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RELEASED"));
    api.doGet("ebcoll", s + "?" + c()).andExpect(status().isOk());
    api.doGet("ebcoll", "/api/v1/eb/programmes/" + p.getId() + "/invoices?" + c())
        .andExpect(status().isOk());
    api.doGet(AO, "/api/v1/eb/programmes/" + p.getId() + "/roster?" + c())
        .andExpect(status().isOk());
    assertThat(soa.get("soaNo").asText()).startsWith("EBS-");
  }
}
