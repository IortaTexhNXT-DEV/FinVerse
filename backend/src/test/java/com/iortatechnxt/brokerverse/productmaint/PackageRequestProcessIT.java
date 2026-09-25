package com.iortatechnxt.brokerverse.productmaint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionReleased;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionReturned;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.productmaint.domain.Advisory;
import com.iortatechnxt.brokerverse.productmaint.domain.ComparativeOutput;
import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRound;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageInsurerResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.CoverageTerm;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Dates;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.InsurerLine;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Scheme;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestScope;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import com.iortatechnxt.brokerverse.productmaint.domain.RoundStatus;
import com.iortatechnxt.brokerverse.productmaint.service.AdvisoryService;
import com.iortatechnxt.brokerverse.productmaint.service.ComparativeService;
import com.iortatechnxt.brokerverse.productmaint.service.ComparativeTable.Selection;
import com.iortatechnxt.brokerverse.productmaint.service.NegotiationService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageRequestService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageRequests;
import com.iortatechnxt.brokerverse.productmaint.service.PackageResponseService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageResponseService.ResponseTerms;
import com.iortatechnxt.brokerverse.productmaint.service.PackageSetupHandoff;
import com.iortatechnxt.brokerverse.productmaint.service.PackageSetupHandoff.SetupInput;
import com.iortatechnxt.brokerverse.productmaint.service.RequestDraft;
import com.iortatechnxt.brokerverse.productmaint.service.RequirementsService;
import com.iortatechnxt.brokerverse.productmaint.service.TermsService;
import com.iortatechnxt.brokerverse.productmaint.service.TermsService.InsurerChoice;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * The package request process end to end on the catalog contracts (BRPM.008-016, PMADD03/04/06):
 * request, approvals, two negotiation rounds, terms final with the comparative master and a client
 * view, requirements, ManCom sign-off, MBS set-up, validator return and release (catalog events
 * published by the test, as the stub never releases), advisory. Codes are unique per run.
 */
@IntegrationTest
class PackageRequestProcessIT {

  private static final AtomicInteger SEQ = new AtomicInteger();
  private static final LocalDate START = LocalDate.now().plusMonths(2).withDayOfMonth(1);

  @Autowired private PackageRequestService requests;
  @Autowired private PackageRequests reader;
  @Autowired private NegotiationService negotiation;
  @Autowired private PackageResponseService responses;
  @Autowired private TermsService terms;
  @Autowired private ComparativeService comparatives;
  @Autowired private RequirementsService requirements;
  @Autowired private PackageSetupHandoff setup;
  @Autowired private AdvisoryService advisories;
  @Autowired private DocumentService documents;
  @Autowired private ClientService clients;
  @Autowired private MessageService messages;
  @Autowired private WorkflowService workflow;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private TestData data;
  @Autowired private AsUser as;

  private Long company() {
    return data.company().getId();
  }

  private static PackageTerms requested(String... insurers) {
    return new PackageTerms(
        List.of(new PackageTerms.Section("Target market", "Corporate fleet owners")),
        List.of(
            coverage("OD_THEFT", "2500000", "PHP 5,000 each loss"), coverage("PD", "500000", null)),
        new Scheme(new BigDecimal("0.35"), new BigDecimal("15000"), BigDecimal.TEN, null, null),
        new Dates(START, START, START.plusYears(1).minusDays(1), null),
        Arrays.stream(insurers).map(InsurerLine::target).toList());
  }

  private static CoverageTerm coverage(String code, String limit, String deductible) {
    return new CoverageTerm(
        code, true, false, new BigDecimal(limit), null, null, null, deductible, List.of(), null);
  }

  private RequestDraft draft(RequestType type, String product, Boolean negotiate, String... ins) {
    boolean isNew = type == RequestType.NEW;
    return new RequestDraft(
        type,
        RequestScope.GENERIC,
        "Test package " + SEQ.incrementAndGet(),
        null,
        "MOTOR",
        "COMPREHENSIVE",
        product,
        null,
        List.of(),
        isNew ? "NEW_PROGRAMME" : "PACKAGE_EXPIRY",
        null,
        negotiate,
        requested(ins));
  }

  private void attach(Long id, String type) {
    as.run(
        "tsu",
        () ->
            documents.upload(
                new AttachmentTarget(PackageRequests.ENTITY, String.valueOf(id)),
                List.of(
                    new UploadedFile("doc.pdf", "%PDF-1.4 x".getBytes(StandardCharsets.US_ASCII))),
                new UploadOptions(type, false, null, null)));
  }

  private PackageInsurerResponse response(NegotiationRound r, String insurer) {
    return responses.ofRound(r).stream()
        .filter(x -> x.getInsurerCode().equals(insurer))
        .findFirst()
        .orElseThrow();
  }

  private void key(Long id, NegotiationRound r, String insurer, String outcome, String rate) {
    as.run(
        "tsu",
        () ->
            responses.record(
                id,
                response(r, insurer).getId(),
                new ResponseTerms(
                    outcome,
                    rate == null ? null : new BigDecimal(rate),
                    new BigDecimal("15000"),
                    List.of(coverage("OD_THEFT", "2500000", "PHP 7,500 each loss")),
                    "Standard CAR wording",
                    START,
                    null)));
  }

  private NegotiationRound sendRound(Long id, int roundNo) {
    as.run("tsu", () -> negotiation.submitSlip(id, roundNo, null));
    return as.run("tsulead", () -> negotiation.approveSlip(id, roundNo));
  }

  private Long throughTsuApproval(RequestDraft draft) {
    PackageRequest p = as.run("ao", () -> requests.create(company(), draft));
    Long id = p.getId();
    as.run("ao", () -> requests.submit(id, "please approve"));
    as.run("mkttl", () -> requests.approve(id, null));
    as.run("tsulead", () -> requests.recommend(id, "Recommend: approach the panel"));
    as.run("tsuhead", () -> requests.approveTsu(id, null));
    return id;
  }

  @Test
  void aNewPackageRunsFromDraftToReleasedWithTwoRoundsAndAnAdvisory() {
    PackageRequest created =
        as.run(
            "ao",
            () ->
                requests.create(
                    company(), draft(RequestType.NEW, null, null, "INS-MGIC", "INS-LAC")));
    Long id = created.getId();
    assertThat(created.getRequestNo()).matches("PKR-\\d{4}-\\d{6}");
    assertThat(created.getStatus()).isEqualTo(RequestStage.DRAFT);

    as.run("ao", () -> requests.submit(id, "for approval"));
    assertThatThrownBy(() -> as.run("ao", () -> requests.approve(id, null)))
        .extracting("code")
        .isEqualTo("PKG_FOUR_EYES");
    as.run("mkttl", () -> requests.approve(id, "go"));
    assertThatThrownBy(() -> as.run("tsulead", () -> requests.recommend(id, " ")))
        .extracting("code")
        .isEqualTo("PKG_RECOMMENDATION_REQUIRED");
    as.run("tsulead", () -> requests.recommend(id, "Recommend: two panel insurers"));
    as.run("tsuhead", () -> requests.approveTsu(id, null));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.NEGOTIATION);
    assertThat(reader.get(id).getRecommendation()).isEqualTo("Recommend: two panel insurers");

    NegotiationRound one = negotiation.latest(id);
    assertThat(one.getRoundNo()).isEqualTo(1);
    assertThat(one.getInsurers()).containsExactly("INS-MGIC", "INS-LAC");
    as.run("tsu", () -> negotiation.submitSlip(id, 1, null));
    assertThat(negotiation.round(id, 1).getQsNo()).matches("PQS-\\d{4}-\\d{6}");
    assertThatThrownBy(() -> as.run("tsu", () -> negotiation.approveSlip(id, 1)))
        .extracting("code")
        .isEqualTo("QS_FOUR_EYES");
    one = as.run("tsulead", () -> negotiation.approveSlip(id, 1));
    assertThat(one.getStatus()).isEqualTo(RoundStatus.SENT);
    assertThat(messages.forRecord(PackageRequests.ENTITY, String.valueOf(id))).hasSize(4);
    NegotiationRound first = one;
    as.run("tsu", () -> negotiation.resend(id, 1, "INS-LAC"));
    assertThat(response(first, "INS-LAC").getSends()).isEqualTo(2);

    assertThatThrownBy(
            () ->
                as.run(
                    "tsu",
                    () ->
                        terms.termsFinal(
                            id, List.of(new InsurerChoice("INS-MGIC", null, null)), null)))
        .extracting("code")
        .isEqualTo("NEGOTIATION_INCOMPLETE");
    assertThatThrownBy(() -> key(id, first, "INS-MGIC", "APPROVED_WITH_CHANGES", null))
        .extracting("code")
        .isEqualTo("PKG_RESPONSE_RATE_REQUIRED");
    key(id, first, "INS-MGIC", "APPROVED_WITH_CHANGES", "0.40");
    key(id, first, "INS-LAC", "COUNTER_PROPOSAL", "0.33");
    assertThat(responses.comparative(first).rows().get(0).insurerCode()).isEqualTo("INS-LAC");

    NegotiationRound two =
        as.run("tsu", () -> negotiation.revise(id, List.of(), "Match the lower rate"));
    assertThat(two.getRoundNo()).isEqualTo(2);
    assertThat(negotiation.round(id, 1).getStatus()).isEqualTo(RoundStatus.CLOSED);
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.NEGOTIATION);
    NegotiationRound second = sendRound(id, 2);
    key(id, second, "INS-MGIC", "ACCEPTED_AS_REQUESTED", "0.34");
    key(id, second, "INS-LAC", "DECLINED", null);
    assertThat(responses.history(id)).hasSizeGreaterThanOrEqualTo(4);
    assertThatThrownBy(
            () ->
                as.run(
                    "tsu",
                    () ->
                        terms.termsFinal(
                            id, List.of(new InsurerChoice("INS-LAC", null, null)), null)))
        .extracting("code")
        .isEqualTo("PKG_INSURER_NOT_OFFERED");
    as.run(
        "tsu",
        () -> terms.termsFinal(id, List.of(new InsurerChoice("INS-MGIC", "panel", null)), "final"));
    PackageRequest afterTerms = reader.get(id);
    assertThat(afterTerms.getStatus()).isEqualTo(RequestStage.TERMS_REVIEW);
    assertThat(afterTerms.getChosenInsurerList()).containsExactly("INS-MGIC");

    List<ComparativeOutput> outputs = comparatives.outputs(id);
    assertThat(outputs).hasSize(1);
    ComparativeOutput master = outputs.get(0);
    assertThat(master.getKind()).isEqualTo(ComparativeOutput.Kind.MASTER);
    assertThat(master.getSha256()).hasSize(64);
    ComparativeOutput view =
        as.run(
            "tsu",
            () ->
                comparatives.clientView(
                    id, "Client view", new Selection(List.of("RATE"), List.of("INS-MGIC"))));
    assertThat(view.getParentOutputId()).isEqualTo(master.getId());
    assertThat(view.getFieldList()).containsExactly("RATE");
    assertThat(comparatives.file(id, view.getId(), true).fileName()).endsWith(".xlsx");
    ComparativeOutput recompiled = as.run("tsu", () -> comparatives.compileMaster(id));
    assertThat(comparatives.outputs(id))
        .filteredOn(o -> o.getKind() == ComparativeOutput.Kind.MASTER && o.isCurrent())
        .extracting(ComparativeOutput::getId)
        .containsExactly(recompiled.getId());

    assertThatThrownBy(() -> as.run("tsuhead", () -> terms.releaseToMarketing(id, null)))
        .extracting("code")
        .isEqualTo("PKG_SCOPE_GENERIC");
    as.run("tsuhead", () -> terms.skipMarketingReview(id, null));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.REQUIREMENTS_PREP);

    assertThatThrownBy(() -> as.run("tsu", () -> requirements.submitRequirements(id, null)))
        .extracting("code")
        .isEqualTo("REQUIREMENTS_INCOMPLETE");
    as.run(
        "tsu",
        () ->
            requirements.updateRequirements(
                id,
                new Scheme(
                    new BigDecimal("0.34"),
                    new BigDecimal("15000"),
                    BigDecimal.TEN,
                    new BigDecimal("50000000"),
                    "Contract value x 0.34%"),
                new Dates(START, START, START.plusYears(1).minusDays(1), null)));
    assertThat(requirements.missing(id))
        .containsExactly("the signed package slip (document PKG_SLIP_SIGNED)");
    assertThat(requirements.packageSlip(id).content()).isNotEmpty();
    attach(id, RequirementsService.SIGNED_SLIP);
    as.run("tsu", () -> requirements.submitRequirements(id, "pack complete"));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.FOR_MANCOM);

    as.run("mancom", () -> requirements.signoff(id, "signed"));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.WITH_MBS);
    assertThat(negotiation.rounds(id)).allMatch(NegotiationRound::isLocked);
    assertThat(requirements.signoffs(id).get(0).getSignedSheetAttachmentId()).isNotNull();

    String code = "PMT" + (100000 + SEQ.incrementAndGet() * 7 + System.nanoTime() % 90000);
    as.run("mbs", () -> setup.setUp(id, new SetupInput(code, null, null, "set up")));
    PackageRequest setUp = reader.get(id);
    assertThat(setUp.getStatus()).isEqualTo(RequestStage.FOR_VALIDATION);
    assertThat(setUp.getTargetProductCode()).isEqualTo(code);
    int version = setUp.getResultingVersionNo();
    assertThat(version).isPositive();

    events.publishEvent(
        new ProductVersionReturned(
            code, version, "Deductible missing", setUp.getRequestNo(), "badmin"));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.WITH_MBS);
    as.run("mbs", () -> setup.setUp(id, new SetupInput(code, null, "Deductible added", null)));
    assertThat(reader.get(id).getResultingVersionNo()).isEqualTo(version);

    events.publishEvent(
        new ProductVersionReleased(code, version, START, setUp.getRequestNo(), "badmin"));
    PackageRequest released = reader.get(id);
    assertThat(released.getStatus()).isEqualTo(RequestStage.RELEASED);
    assertThat(released.getReleasedAt()).isNotNull();

    List<Advisory> drafted = advisories.ofRequest(id);
    assertThat(drafted).hasSize(1);
    Advisory advisory = drafted.get(0);
    assertThat(advisory.getAdvisoryType()).isEqualTo(Advisory.Type.PACKAGE_READY);
    assertThat(advisories.checklist(advisory)).allMatch(AdvisoryService.DocumentCheck::attached);
    Advisory sent = as.run("tsu", () -> advisories.send(advisory.getId()));
    assertThat(sent.getStatus()).isEqualTo(Advisory.Status.SENT);
    assertThat(sent.getDocumentIdList()).hasSize(3);
  }

  @Test
  void anAdvisoryIsBlockedWithoutItsDocumentsAndARenewalNeedsNoNegotiation() {
    Long id = throughTsuApproval(draft(RequestType.RENEW, "MTR31", false, "INS-MGIC"));
    PackageRequest p = reader.get(id);
    assertThat(p.getStatus()).isEqualTo(RequestStage.FOR_MANCOM);
    assertThat(p.getChosenInsurerList()).containsExactly("INS-MGIC");
    as.run("mancom", () -> requirements.signoff(id, null));
    as.run("mbs", () -> setup.setUp(id, new SetupInput(null, null, null, null)));
    PackageRequest setUp = reader.get(id);
    assertThat(setUp.getTargetProductCode()).isEqualTo("MTR31");
    events.publishEvent(
        new ProductVersionReleased(
            "MTR31", setUp.getResultingVersionNo(), START, setUp.getRequestNo(), "tsuhead"));
    Advisory advisory = advisories.ofRequest(id).get(0);
    assertThat(advisory.getAdvisoryType()).isEqualTo(Advisory.Type.RENEWAL);

    assertThatThrownBy(() -> as.run("tsu", () -> advisories.send(advisory.getId())))
        .extracting("code")
        .isEqualTo("ADVISORY_DOCUMENTS_MISSING");
    as.run(
        "tsu",
        () ->
            advisories.update(
                advisory.getId(),
                new Advisory.Content(
                    List.of("MARKETING", "OPERATIONS"),
                    List.of("operations@brokerverse-demo.ph"),
                    "MTR31 renewed",
                    "The MTR31 package is renewed.",
                    null)));
    attach(id, RequirementsService.SIGNED_SLIP);
    Advisory sent = as.run("tsu", () -> advisories.send(advisory.getId()));
    assertThat(sent.getStatus()).isEqualTo(Advisory.Status.SENT);
    assertThat(sent.getMessageIdList()).hasSize(1);
    assertThatThrownBy(() -> as.run("tsu", () -> advisories.send(advisory.getId())))
        .extracting("code")
        .isEqualTo("ADVISORY_ALREADY_SENT");
  }

  @Test
  void aRetireRequestRetiresThePackageWithAnAdvisoryDraft() {
    Long id = throughTsuApproval(draft(RequestType.RETIRE, "MTR32", null));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.FOR_MANCOM);
    as.run("mancom", () -> requirements.signoff(id, null));
    assertThatThrownBy(
            () -> as.run("mbs", () -> setup.setUp(id, new SetupInput(null, null, null, null))))
        .extracting("code")
        .isEqualTo("PKG_REQUEST_TYPE_MISMATCH");
    as.run("mbs", () -> setup.retire(id, "retired"));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.RETIRED);
    Advisory advisory = advisories.ofRequest(id).get(0);
    assertThat(advisory.getAdvisoryType()).isEqualTo(Advisory.Type.RETIREMENT);
    assertThat(advisories.checklist(advisory)).hasSize(1).allMatch(c -> c.attached());
  }

  @Test
  void aClientSpecificPackageGoesThroughMarketingReviewAndReturns() {
    RequestDraft base = draft(RequestType.AMEND, "MTR34", null, "INS-VMI");
    RequestDraft noClient = withClient(base, null);
    assertThatThrownBy(() -> as.run("ao", () -> requests.create(company(), noClient)))
        .extracting("code")
        .isEqualTo("PKG_REQUEST_INCOMPLETE");
    Long client = clients.requireByCode(company(), "CL-2026-000003").getId();
    PackageRequest p = as.run("ao", () -> requests.create(company(), withClient(base, client)));
    Long id = p.getId();
    assertThat(p.getClientCode()).isEqualTo("CL-2026-000003");
    as.run("ao", () -> requests.submit(id, null));
    as.run(
        "mkttl",
        () ->
            workflow.transition(
                PackageRequests.ENTITY,
                String.valueOf(id),
                "return",
                new TransitionNote("INCOMPLETE_DETAILS", "add the loss history")));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.DRAFT);
    assertThatThrownBy(() -> as.run("mbs", () -> requests.update(id, withClient(base, client))))
        .extracting("code")
        .isEqualTo("PKG_REQUEST_NOT_EDITABLE");
    as.run("ao", () -> requests.update(id, withClient(base, client)));
    as.run("ao", () -> requests.submit(id, null));
    as.run("mkttl", () -> requests.approve(id, null));
    as.run("tsulead", () -> requests.recommend(id, "Approach Visayas Mutual"));
    assertThatThrownBy(() -> as.run("tsulead", () -> requests.approveTsu(id, null)))
        .extracting("code")
        .isEqualTo("PKG_FOUR_EYES");
    as.run("tsuhead", () -> requests.approveTsu(id, null));
    NegotiationRound round = sendRound(id, 1);
    key(id, round, "INS-VMI", "APPROVED_WITH_CHANGES", "1.40");
    List<InsurerChoice> vmi = List.of(new InsurerChoice("INS-VMI", null, null));
    as.run("tsu", () -> terms.termsFinal(id, vmi, null));
    assertThatThrownBy(() -> as.run("tsuhead", () -> terms.skipMarketingReview(id, null)))
        .extracting("code")
        .isEqualTo("PKG_SCOPE_CLIENT");
    as.run("tsuhead", () -> terms.releaseToMarketing(id, "for the client"));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.FOR_MKT_REVIEW);
    as.run(
        "ao",
        () ->
            workflow.transition(
                PackageRequests.ENTITY,
                String.valueOf(id),
                "request_changes",
                new TransitionNote("INSURER_REQUIREMENTS", "client wants a lower rate")));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.NEGOTIATION);
    as.run("tsu", () -> terms.termsFinal(id, vmi, "same terms, client informed"));
    as.run("tsuhead", () -> terms.releaseToMarketing(id, null));
    as.run("ao", () -> terms.acceptTerms(id, "client agrees"));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.REQUIREMENTS_PREP);
  }

  private static RequestDraft withClient(RequestDraft d, Long clientId) {
    return new RequestDraft(
        d.type(),
        RequestScope.CLIENT_SPECIFIC,
        d.title(),
        clientId,
        d.lineCode(),
        d.coverTypeCode(),
        d.productCode(),
        null,
        List.of(),
        d.reason(),
        null,
        null,
        d.terms());
  }
}
