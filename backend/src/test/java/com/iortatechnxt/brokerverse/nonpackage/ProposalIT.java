package com.iortatechnxt.brokerverse.nonpackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseTerms;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import com.iortatechnxt.brokerverse.nonpackage.service.ComparativeTable;
import com.iortatechnxt.brokerverse.nonpackage.service.InsurerResponseService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalAcceptanceService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalClientRecords;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalDocuments;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalDraft;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalQueryService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalRetentionProvider;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalSlipService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalSlipService.ClientEmail;
import com.iortatechnxt.brokerverse.nonpackage.service.QuotationSlipService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@IntegrationTest
class ProposalIT {

  private static final LocalDate FROM = LocalDate.of(2026, 12, 1);
  private static final LocalDate TO = LocalDate.of(2027, 12, 1);

  @Autowired private ProposalService proposals;
  @Autowired private ProposalQueryService queries;
  @Autowired private QuotationSlipService quotationSlips;
  @Autowired private InsurerResponseService responses;
  @Autowired private ProposalSlipService proposalSlips;
  @Autowired private ProposalAcceptanceService acceptance;
  @Autowired private ProposalDocuments documentsBuilder;
  @Autowired private ProposalClientRecords clientRecords;
  @Autowired private ProposalRetentionProvider retention;
  @Autowired private AccountQueryService accounts;
  @Autowired private ClientService clients;
  @Autowired private DocumentService documents;
  @Autowired private MessageService messages;
  @Autowired private WorkflowService workflow;
  @Autowired private TestData data;
  @Autowired private AsUser as;

  private Long company() {
    return data.company().getId();
  }

  private ProposalDraft engineering(Long clientId, String... insurers) {
    return new ProposalDraft(
        clientId,
        "CAR00",
        "CORBANK",
        "EMAIL",
        null,
        FROM,
        TO,
        new RiskDetails(
            List.of(
                new RiskDetails.Section("Project", "Warehouse extension, 2 storeys"),
                new RiskDetails.Section(" ", " ")),
            List.of(
                new RiskDetails.Item(
                    1,
                    RiskItemData.generic(
                        "Civil works", new BigDecimal("25000000"), new BigDecimal("0.35"))))),
        List.of(insurers));
  }

  private void attach(Long id, String type) {
    as.run(
        "ao",
        () ->
            documents.upload(
                new AttachmentTarget("ProposalRequest", String.valueOf(id)),
                List.of(
                    new UploadedFile("doc.pdf", "%PDF-1.4 x".getBytes(StandardCharsets.US_ASCII))),
                new UploadOptions(type, false, null, null)));
  }

  private InsurerResponse response(Long id, String insurer) {
    return responses.responses(id).stream()
        .filter(r -> r.getInsurerCode().equals(insurer))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void aPrfGoesThroughTsuInsurersComparativeTableProposalSlipAndAccounts() {
    Long client = clients.requireByCode(company(), "CL-2026-900003").getId();
    ProposalRequest p =
        as.run("ao", () -> proposals.create(company(), engineering(client, "INS-MGIC")));
    Long id = p.getId();
    assertThat(p.getPrfNo()).matches("PRF-\\d{4}-\\d{6}");
    assertThat(p.getArn()).matches("ARN-\\d{4}-\\d{6}");
    assertThat(p.getTsuRule()).isEqualTo("NON_PACKAGE");
    assertThat(p.getTotalSumInsured()).isEqualByComparingTo("25000000");
    assertThat(queries.details(queries.get(id)).sections()).hasSize(1);
    assertThat(proposals.checklist(id)).isEmpty();

    as.run("ao", () -> proposals.submit(id, "for approval"));
    assertThatThrownBy(() -> as.run("ao", () -> proposals.approve(id, null)))
        .extracting("code")
        .isEqualTo("PRF_FOUR_EYES");
    as.run("mkttl", () -> proposals.approve(id, "go"));
    assertThat(queries.get(id).getStatus()).isEqualTo(ProposalStatus.WITH_TSU);
    assertThatThrownBy(() -> as.run("ao", () -> proposals.update(id, engineering(client))))
        .extracting("code")
        .isEqualTo("PRF_NOT_EDITABLE");

    as.run(
        "tsu",
        () -> workflow.transition("ProposalRequest", String.valueOf(id), "prepare_qs", null));
    as.run("tsu", () -> proposals.update(id, engineering(client, "INS-MGIC")));
    as.run(
        "tsu", () -> quotationSlips.selectInsurers(id, List.of("INS-MGIC", "INS-LAC", "INS-VMI")));
    ProposalRequest qs = as.run("tsu", () -> quotationSlips.submit(id, null, "slip ready"));
    assertThat(qs.getQsNo()).matches("QS-\\d{4}-\\d{6}");
    assertThat(qs.getQsTemplate()).isEqualTo("QUOTATION_SLIP v1");
    assertThatThrownBy(() -> as.run("tsu", () -> quotationSlips.approve(id, null)))
        .extracting("code")
        .isEqualTo("QS_FOUR_EYES");
    as.run("tsulead", () -> quotationSlips.approve(id, "send"));
    assertThat(queries.get(id).getStatus()).isEqualTo(ProposalStatus.QS_SENT);
    assertThat(messages.forRecord("ProposalRequest", String.valueOf(id))).hasSize(6);
    assertThat(responses.responses(id))
        .hasSize(3)
        .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(ResponseStatus.PENDING));

    Long mgic = response(id, "INS-MGIC").getId();
    Long lac = response(id, "INS-LAC").getId();
    as.run(
        "tsu",
        () ->
            responses.record(
                id,
                mgic,
                new ResponseTerms(
                    ResponseStatus.RECEIVED,
                    new BigDecimal("120000"),
                    new BigDecimal("0.40"),
                    "PHP 50,000 each loss",
                    "Standard CAR wording",
                    TO,
                    null)));
    assertThatThrownBy(
            () ->
                as.run(
                    "tsu",
                    () ->
                        responses.record(
                            id,
                            lac,
                            new ResponseTerms(
                                ResponseStatus.RECEIVED, null, null, null, null, null, null))))
        .extracting("code")
        .isEqualTo("RESPONSE_PREMIUM_REQUIRED");
    as.run(
        "tsu",
        () ->
            responses.record(
                id,
                lac,
                new ResponseTerms(
                    ResponseStatus.RECEIVED,
                    new BigDecimal("110000"),
                    new BigDecimal("0.30"),
                    "PHP 75,000 each loss",
                    "Standard CAR wording",
                    TO,
                    "Best terms")));
    as.run(
        "tsu",
        () ->
            responses.attach(
                id,
                mgic,
                new UploadedFile("mgic.pdf", "%PDF-1.4 r".getBytes(StandardCharsets.US_ASCII))));
    assertThatThrownBy(() -> as.run("tsu", () -> responses.termsComplete(id, false, null)))
        .extracting("code")
        .isEqualTo("TERMS_PENDING");
    as.run("tsu", () -> responses.recommend(id, lac));
    ComparativeTable table = responses.comparative(id);
    assertThat(table.rows()).hasSize(3);
    assertThat(table.rows().get(0).insurerCode()).isEqualTo("INS-LAC");
    assertThat(table.rows().get(0).lowest()).isTrue();
    assertThat(table.recommendedInsurer()).isEqualTo("INS-LAC");
    assertThat(responses.history(id)).hasSizeGreaterThanOrEqualTo(4);
    assertThat(response(id, "INS-MGIC").getDocumentId()).isNotNull();
    as.run("tsu", () -> responses.termsComplete(id, true, null));
    assertThat(queries.get(id).isTermsClosed()).isTrue();
    assertThat(documentsBuilder.comparativeXlsx(queries.get(id), responses.responses(id)).content())
        .isNotEmpty();

    ProposalRequest ps = as.run("tsu", () -> proposalSlips.submit(id, null, null));
    assertThat(ps.getPsNo()).matches("PS-\\d{4}-\\d{6}");
    assertThat(ps.getChosenInsurer()).isEqualTo("INS-LAC");
    assertThat(
            documents.documentTypesOf(new AttachmentTarget("ProposalRequest", String.valueOf(id))))
        .contains("PROPOSAL_SLIP", "INSURER_RESPONSE");
    assertThatThrownBy(() -> as.run("tsu", () -> proposalSlips.approve(id, null)))
        .extracting("code")
        .isEqualTo("PS_FOUR_EYES");
    as.run("tsulead", () -> proposalSlips.approve(id, null));
    assertThat(queries.get(id).getStatus()).isEqualTo(ProposalStatus.PS_RELEASED);
    assertThat(quotationSlips.pdf(id).content()).isNotEmpty();
    assertThat(proposalSlips.pdf(id).fileName()).endsWith("_v1.pdf");

    as.run(
        "ao",
        () ->
            proposalSlips.sendToClient(
                id,
                new ClientEmail(
                    List.of("treasury@pacificharbor.example"),
                    null,
                    "Our proposal",
                    "Dear client",
                    null)));
    assertThat(queries.get(id).getStatus()).isEqualTo(ProposalStatus.SENT_TO_CLIENT);
    assertThatThrownBy(() -> as.run("ao", () -> acceptance.accept(id, null, null)))
        .extracting("code")
        .isEqualTo("ACCEPTANCE_EMAIL_REQUIRED");
    attach(id, "CLIENT_ACCEPTANCE");
    as.run("ao", () -> acceptance.accept(id, List.of(1), "accepted"));
    ProposalRequest converted = as.run("ao", () -> acceptance.createAccounts(id, null));
    assertThat(converted.getAccountArns()).containsExactly(p.getArn());
    Account account = accounts.requireByArn(p.getArn());
    assertThat(account.getProposalRef()).isEqualTo(p.getPrfNo());
    assertThat(account.getInsurerCode()).isEqualTo("INS-LAC");
    assertThat(account.getItems().get(0).getRate()).isEqualByComparingTo("0.30");
    assertThat(queries.get(id).getStatus()).isEqualTo(ProposalStatus.CONVERTED);
    assertThat(clientRecords.recordsOf(client)).extracting("reference").contains(p.getPrfNo());
    assertThat(
            queries
                .search(
                    new ProposalQueryService.Search(
                        company(), p.getPrfNo(), List.of(ProposalStatus.CONVERTED), "ao", client),
                    PageRequest.of(0, 5))
                .getContent())
        .hasSize(1);
  }

  @Test
  void packageRisksNeedDocumentsAndATsuReasonAndVoidedPrfsAreRetentionCandidates() {
    Long client = clients.requireByCode(company(), "CL-2026-900001").getId();
    String id = Long.toString(System.nanoTime(), 36).toUpperCase();
    ProposalDraft motor =
        new ProposalDraft(
            client,
            "MTR10",
            "CBG",
            null,
            null,
            FROM,
            TO,
            new RiskDetails(
                List.of(),
                List.of(
                    new RiskDetails.Item(
                        1,
                        new RiskItemData(
                            null,
                            new BigDecimal("900000"),
                            null,
                            null,
                            null,
                            new Vehicle(
                                "PR" + id, null, "PE" + id, "PC" + id, "Honda", "City", 2025, null,
                                null, null),
                            null,
                            null)))),
            List.of());
    ProposalRequest p = as.run("ao", () -> proposals.create(company(), motor));
    assertThat(proposals.checklist(p.getId())).extracting("documentType").containsExactly("IDF");
    assertThatThrownBy(() -> as.run("ao", () -> proposals.submit(p.getId(), null)))
        .extracting("code")
        .isEqualTo("MISSING_DOCUMENTS");
    attach(p.getId(), "IDF");
    assertThatThrownBy(() -> as.run("ao", () -> proposals.submit(p.getId(), null)))
        .extracting("code")
        .isEqualTo("PRF_NOT_NEEDED");
    as.run(
        "ao",
        () ->
            workflow.transition(
                "ProposalRequest",
                String.valueOf(p.getId()),
                "void",
                new TransitionNote("CLIENT_WITHDREW", null)));
    assertThat(queries.get(p.getId()).getStatus()).isEqualTo(ProposalStatus.VOIDED);
    RetentionCriteria criteria =
        new RetentionCriteria(Set.of("VOIDED", "BOGUS"), LocalDate.now().plusDays(1));
    assertThat(retention.countEligible(criteria)).isPositive();
    assertThat(retention.eligible(criteria, 500)).extracting("reference").contains(p.getPrfNo());
    assertThat(retention.countEligible(new RetentionCriteria(Set.of("BOGUS"), LocalDate.now())))
        .isZero();
  }
}
