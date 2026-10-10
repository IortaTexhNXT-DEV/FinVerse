package com.iortatechnxt.brokerverse.productmaint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseTerms;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import com.iortatechnxt.brokerverse.nonpackage.service.InsurerResponseService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalDraft;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalQueryService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalSlipService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalSlipService.ClientEmail;
import com.iortatechnxt.brokerverse.nonpackage.service.QuotationSlipService;
import com.iortatechnxt.brokerverse.productmaint.service.PmStatusNames;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The comparative table with the Final Terms for Proposal, the insurer selection, the proposal
 * slips per insurer and the client response of BDOI's FRS (FRPM.006.02, FRPM.008.01, FRPM.009.01,
 * FRPM.009.02, FRPM.010.01, FRPM.012.02, FRPM.013.01) and the quotation slip named per insurer.
 */
@IntegrationTest
class TermsProposalIT {

  private static final String BASE = "/api/v1/product-maintenance/terms/";
  private static final LocalDate FROM = LocalDate.of(2026, 12, 1);
  private static final LocalDate TO = LocalDate.of(2027, 12, 1);
  private static final DateTimeFormatter MMDDYYYY = DateTimeFormatter.ofPattern("MMddyyyy");

  @Autowired private ProposalService proposals;
  @Autowired private ProposalQueryService queries;
  @Autowired private QuotationSlipService quotationSlips;
  @Autowired private InsurerResponseService responses;
  @Autowired private ProposalSlipService proposalSlips;
  @Autowired private ClientService clients;
  @Autowired private DocumentService documents;
  @Autowired private WorkflowService workflow;
  @Autowired private TestData data;
  @Autowired private AsUser as;
  @Autowired private Api api;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Clock clock;

  private Long quotationAtQsSent() {
    Long company = data.company().getId();
    Long client = clients.requireByCode(company, "CL-2026-900003").getId();
    ProposalDraft draft =
        new ProposalDraft(
            client,
            "CAR00",
            "CORBANK",
            "EMAIL",
            null,
            FROM,
            TO,
            new RiskDetails(
                List.of(new RiskDetails.Section("Project", "Cold storage, 1 storey")),
                List.of(
                    new RiskDetails.Item(
                        1,
                        RiskItemData.generic(
                            "Civil works", new BigDecimal("18000000"), new BigDecimal("0.35"))))),
            List.of("INS-MGIC"));
    Long id = as.run("ao", () -> proposals.create(company, draft)).getId();
    as.run("ao", () -> proposals.submit(id, "for approval"));
    as.run("mkttl", () -> proposals.approve(id, "go"));
    as.run(
        "tsu",
        () -> workflow.transition("ProposalRequest", String.valueOf(id), "prepare_qs", null));
    as.run(
        "tsu", () -> quotationSlips.selectInsurers(id, List.of("INS-MGIC", "INS-LAC", "INS-VMI")));
    as.run("tsu", () -> quotationSlips.submit(id, null, "slip ready"));
    as.run("tsulead", () -> quotationSlips.approve(id, "send"));
    return id;
  }

  private void quote(Long id, String insurer, String premium) {
    InsurerResponse r =
        responses.responses(id).stream()
            .filter(x -> x.getInsurerCode().equals(insurer))
            .findFirst()
            .orElseThrow();
    as.run(
        "tsu",
        () ->
            responses.record(
                id,
                r.getId(),
                new ResponseTerms(
                    ResponseStatus.RECEIVED,
                    new BigDecimal(premium),
                    new BigDecimal("0.35"),
                    "50,000 each loss",
                    "Standard CAR wording",
                    TO,
                    null)));
  }

  private int notices(String user, String titlePart) {
    return jdbc.queryForObject(
        "select count(*) from msg_notification where recipient = ? and title like ?",
        Integer.class,
        user,
        "%" + titlePart + "%");
  }

  @Test
  void theRequestorSelectsInsurersTsuGeneratesProposalSlipsAndTheClientReturnsAndAccepts()
      throws Exception {
    Long id = quotationAtQsSent();
    ProposalRequest p = queries.get(id);
    String today = MMDDYYYY.format(BusinessClock.today(clock));
    // FRPM.006.02: one quotation slip per insurer, named after the insurer.
    List<String> slips =
        jdbc.queryForList(
            "select a.file_name from msg_outbound_attachment a join msg_outbound m on m.id ="
                + " a.message_id where m.entity_id = ? and a.file_name like 'QS_%'",
            String.class, String.valueOf(id));
    assertThat(slips)
        .contains(
            "QS_" + p.getArn() + "_MabuhayGeneralInsuranceCorp_" + today + ".pdf",
            "QS_" + p.getArn() + "_LuzonAssuranceCo_" + today + ".pdf");
    quote(id, "INS-MGIC", "120000");
    quote(id, "INS-LAC", "110000");
    String url = BASE + "quotation/" + id;

    // FRPM.006.02 / FRPM.012.02: QS value column, several options per insurer, Others wording.
    JsonNode table = api.read(api.doGet("tsu", url).andExpect(status().isOk())).path("table");
    assertThat(table.path("qsValues").path("SUM_INSURED").asText()).isEqualTo("18,000,000.00");
    assertThat(table.path("columns")).hasSize(3);
    api.doPut(
            "tsu",
            url + "/options/INS-LAC",
            Map.of(
                "optionNo",
                2,
                "answer",
                "OTHERS",
                "otherAnswer",
                "Subject to survey",
                "values",
                Map.of("PREMIUM", "105,000.00", "DEDUCTIBLES", "75,000 each loss")))
        .andExpect(status().isOk());
    api.doPut(
            "tsu",
            url + "/fields",
            Map.of(
                "shown", List.of("SUM_INSURED", "PREMIUM", "RATE", "DEDUCTIBLES"),
                "client", List.of("PREMIUM", "DEDUCTIBLES")))
        .andExpect(status().isOk());
    table = api.read(api.doGet("ao", url)).path("table");
    assertThat(table.path("shown")).hasSize(4);
    assertThat(table.path("columns")).hasSize(4);

    // FRPM.008.01: only insurers with terms; the requestor proceeds; Terms Agreed; TSU is told.
    api.doPost("ao", url + "/proceed", Map.of("insurers", List.of("INS-VMI")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PM_PROPOSAL_NO_TERMS"));
    api.doPost("ao", url + "/proceed", Map.of("insurers", List.of("INS-LAC", "INS-MGIC")))
        .andExpect(status().isOk());
    ProposalRequest agreed = queries.get(id);
    assertThat(agreed.getStatus()).isEqualTo(ProposalStatus.TERMS_RECEIVED);
    assertThat(PmStatusNames.quotation("TERMS_RECEIVED", null, agreed.isTermsClosed()))
        .isEqualTo("Terms Agreed");
    assertThat(notices("tsu", "Terms Agreed")).isPositive();

    // FRPM.009.01: the Final Terms show the selected insurer's terms; an edit is kept.
    table = api.read(api.doGet("ao", url)).path("table");
    assertThat(table.path("finalTerms").path("PREMIUM").asText()).isEqualTo("110,000.00");
    api.doPut("ao", url + "/final-terms", Map.of("PREMIUM", "108,000.00"))
        .andExpect(status().isOk());
    JsonNode history = api.read(api.doGet("ao", url + "/final-terms/history"));
    assertThat(history).hasSize(1);
    assertThat(history.get(0).path("field").asText()).isEqualTo("Premium");
    assertThat(history.get(0).path("oldValue").asText()).isEqualTo("110,000.00");
    assertThat(history.get(0).path("newValue").asText()).isEqualTo("108,000.00");

    // FRPM.009.02: one proposal slip per selected insurer, BDOI's file names, for approval.
    api.doPost("ao", url + "/proposals", Map.of()).andExpect(status().isForbidden());
    JsonNode files = api.read(api.doPost("tsu", url + "/proposals", Map.of("comment", "ready")));
    assertThat(files).hasSize(2);
    assertThat(files.findValuesAsText("fileName"))
        .containsExactlyInAnyOrder(
            "ProposalSlip_" + p.getArn() + "_LuzonAssuranceCo_1_" + today + ".pdf",
            "ProposalSlip_" + p.getArn() + "_MabuhayGeneralInsuranceCorp_1_" + today + ".pdf");
    assertThat(queries.get(id).getStatus()).isEqualTo(ProposalStatus.PS_FOR_APPROVAL);
    as.run("tsulead", () -> proposalSlips.approve(id, "fine"));
    assertThat(notices("tsu", "proposal approved")).isPositive();
    assertThat(notices("ao", "Proposal Ready")).isPositive();
    api.download("ao", url + "/export?format=xlsx")
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.containsString(
                        "ComparativeTable_" + p.getArn() + "_" + today + ".xlsx")));
    sendToClient(id);

    // FRPM.010.01: return for revision needs remarks; the request goes back to the terms.
    api.doPost("ao", url + "/client-responses", Map.of("response", "RETURNED"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PM_CLIENT_REMARKS"));
    api.doPost(
            "ao",
            url + "/client-responses",
            Map.of("response", "RETURNED", "remarks", "Lower deductible please"))
        .andExpect(status().isOk());
    assertThat(queries.get(id).getStatus()).isEqualTo(ProposalStatus.TERMS_RECEIVED);
    assertThat(PmStatusNames.quotation("TERMS_RECEIVED", "client_return", true))
        .isEqualTo(PmStatusNames.RETURNED);
    assertThat(notices("tsu", "Return for Revision")).isPositive();
    files = api.read(api.doPost("tsu", url + "/proposals", Map.of("comment", "revised")));
    assertThat(files.findValuesAsText("versionNo")).containsOnly("2");
    as.run("tsulead", () -> proposalSlips.approve(id, null));
    sendToClient(id);
    as.run(
        "ao",
        () ->
            documents.upload(
                new AttachmentTarget("ProposalRequest", String.valueOf(id)),
                List.of(
                    new UploadedFile("ok.pdf", "%PDF-1.4 x".getBytes(StandardCharsets.US_ASCII))),
                new UploadOptions("CLIENT_ACCEPTANCE", false, null, null)));
    api.doPost("ao", url + "/client-responses", Map.of("response", "ACCEPTED"))
        .andExpect(status().isOk());
    assertThat(queries.get(id).getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
    JsonNode recorded = api.read(api.doGet("tsu", url + "/client-responses"));
    assertThat(recorded.findValuesAsText("responseName"))
        .containsExactly("Accepted", "Return for Revision");
    assertThat(api.read(api.doGet("tsu", url + "/proposals"))).hasSize(4);
  }

  private void sendToClient(Long id) {
    as.run(
        "ao",
        () ->
            proposalSlips.sendToClient(
                id,
                new ClientEmail(
                    List.of("treasury@pacificharbor.example"), null, "Proposal", "Dear", null)));
  }

  @Test
  void aPackageRequestGetsProposalSlipsOfTheInsurersSelectedFromTheLatestRound() throws Exception {
    Long id =
        jdbc.queryForObject(
            "select id from pm_request where request_no = 'PKR-2026-900003'", Long.class);
    String url = BASE + "package/" + id;
    JsonNode table = api.read(api.doGet("tsu", url).andExpect(status().isOk())).path("table");
    assertThat(table.findValuesAsText("insurerCode")).contains("INS-MGIC", "INS-LAC");
    api.doPost("tsu", url + "/proceed", Map.of("insurers", List.of("INS-LAC")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PM_PROPOSAL_NO_TERMS"));
    api.doPost("tsu", url + "/client-responses", Map.of("response", "ACCEPTED"))
        .andExpect(status().isForbidden());
    api.doPost("tsu", url + "/proceed", Map.of("insurers", List.of("INS-MGIC")))
        .andExpect(status().isOk());
    JsonNode files = api.read(api.doPost("tsu", url + "/proposals", Map.of()));
    String today = MMDDYYYY.format(BusinessClock.today(clock));
    assertThat(files.findValuesAsText("fileName"))
        .containsExactly(
            "ProposalSlip_PKR-2026-900003_MabuhayGeneralInsuranceCorp_1_" + today + ".pdf");
    assertThat(
            documents.documentTypesOf(new AttachmentTarget("PackageRequest", String.valueOf(id))))
        .contains("PROPOSAL_SLIP");
  }
}
