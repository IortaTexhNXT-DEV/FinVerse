package com.iortatechnxt.brokerverse.quotation;

import static com.iortatechnxt.brokerverse.quotation.QuotationFixtures.motor;
import static com.iortatechnxt.brokerverse.quotation.QuotationFixtures.vehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.quotation.service.QuotationAcceptanceService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationClientRecords;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDiff;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDispatchService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDispatchService.BatchResult;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDispatchService.EmailRequest;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDocuments;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.DraftItem;
import com.iortatechnxt.brokerverse.quotation.service.QuotationQueryService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationSearch;
import com.iortatechnxt.brokerverse.quotation.service.QuotationService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationSummary;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@IntegrationTest
class QuotationIT {

  @Autowired private QuotationService quotations;
  @Autowired private QuotationQueryService queries;
  @Autowired private QuotationDispatchService dispatch;
  @Autowired private QuotationAcceptanceService acceptance;
  @Autowired private QuotationDocuments documents;
  @Autowired private QuotationClientRecords clientRecords;
  @Autowired private AccountQueryService accounts;
  @Autowired private MessageService messages;
  @Autowired private NotificationService notifications;
  @Autowired private WorkflowService workflow;
  @Autowired private WorkflowViewService workflowView;
  @Autowired private QuotationFixtures fx;
  @Autowired private AsUser as;

  private static final EmailRequest EMAIL =
      new EmailRequest(
          List.of("client@example.ph"), List.of(), "Your quotation", "Dear client", null);

  private Quotation approved(Quotation q) {
    as.run("ao", () -> quotations.submit(q.getId(), "please review"));
    return as.run("mkttl", () -> quotations.approve(q.getId(), "ok"));
  }

  @Test
  void createdQuotationHasNumbersPremiumTemplateVersionAndWorkCase() {
    Client client = fx.client("CL-DEMO-A001");
    Quotation q = fx.create(client);
    assertThat(q.getQuotationNo()).matches("QT-\\d{4}-\\d{6}");
    assertThat(q.getArn()).matches("ARN-\\d{4}-\\d{6}");
    assertThat(q.getStatus()).isEqualTo(QuotationStatus.DRAFT);
    assertThat(q.getTemplateVersion()).isEqualTo("QUOTATION_LETTER v1");
    assertThat(q.getGrossPremium()).isPositive();
    assertThat(q.getValidUntil()).isAfter(java.time.LocalDate.now());
    QuotationContent content = queries.content(queries.get(q.getId()));
    assertThat(content.isRated()).isTrue();
    assertThat(content.items())
        .singleElement()
        .satisfies(i -> assertThat(i.premium()).isPositive());
    var view = workflowView.view("Quotation", String.valueOf(q.getId())).orElseThrow();
    assertThat(view.workCase().getReference()).isEqualTo(q.getQuotationNo());
    assertThat(clientRecords.recordsOf(client.getId()))
        .extracting("reference")
        .contains(q.getQuotationNo());
    assertThat(
            queries
                .search(
                    new QuotationSearch(
                        fx.company(),
                        q.getArn(),
                        List.of(QuotationStatus.DRAFT),
                        "MTR10",
                        "ao",
                        null,
                        client.getId()),
                    PageRequest.of(0, 5))
                .getContent())
        .extracting(Quotation::getId)
        .containsExactly(q.getId());
    assertThat(documents.pdfFile(q).content()).startsWith("%PDF".getBytes());
    assertThat(documents.xlsxFile(q).fileName()).endsWith(".xlsx");
  }

  @Test
  void approvalIsFourEyesAndARevisionOpensANewVersionWithADiff() {
    Quotation q = fx.create(fx.client("CL-DEMO-A001"));
    as.run("ao", () -> quotations.submit(q.getId(), null));
    assertThatThrownBy(() -> as.run("ao", () -> quotations.approve(q.getId(), null)))
        .extracting("code")
        .isIn("QUOTATION_FOUR_EYES", "WORKFLOW_ACTION_NOT_PERMITTED");
    as.run("mkttl", () -> quotations.approve(q.getId(), "fine"));
    assertThat(queries.get(q.getId()).getStatus()).isEqualTo(QuotationStatus.APPROVED);
    assertThat(queries.get(q.getId()).getApprovedBy()).isEqualTo("mkttl");

    as.run("ao", () -> quotations.revise(q.getId(), "client asked for a higher sum insured"));
    Quotation revised =
        as.run(
            "ao",
            () ->
                quotations.update(
                    q.getId(),
                    motor(
                        q.getClientId(),
                        false,
                        new DraftItem(1, vehicle("1500000")),
                        new DraftItem(1, vehicle("600000")))));
    assertThat(revised.getCurrentVersion()).isEqualTo(2);
    assertThat(queries.versions(q.getId()))
        .hasSize(2)
        .first()
        .satisfies(
            v -> {
              assertThat(v.isFrozen()).isTrue();
              assertThat(v.getFrozenBy()).isEqualTo("ao");
            });
    QuotationDiff diff = queries.diff(q.getId(), 1, 2);
    assertThat(diff.items())
        .extracting(QuotationDiff.ItemChange::change)
        .contains(QuotationDiff.Change.ADDED, QuotationDiff.Change.REMOVED);
    assertThat(diff.grossDelta()).isPositive();
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        quotations.update(
                            q.getId(),
                            new com.iortatechnxt.brokerverse.quotation.service.QuotationDraft(
                                q.getClientId(),
                                "PAR01",
                                "CBG",
                                null,
                                null,
                                null,
                                null,
                                List.of()))))
        .extracting("code")
        .isEqualTo("QUOTATION_PRODUCT_FIXED");
  }

  @Test
  void approvedQuotationIsSentProtectedAcceptedAndConvertedIntoAccountsPerGroup() {
    Client client = fx.client("CL-DEMO-A001");
    Quotation q =
        as.run(
            "ao",
            () ->
                quotations.create(
                    fx.company(),
                    motor(
                        client.getId(),
                        true,
                        new DraftItem(1, vehicle("900000")),
                        new DraftItem(2, vehicle("700000")))));
    assertThatThrownBy(() -> as.run("ao", () -> dispatch.send(q.getId(), EMAIL)))
        .extracting("code")
        .isEqualTo("QUOTATION_NOT_APPROVED");
    approved(q);
    as.run("ao", () -> dispatch.send(q.getId(), EMAIL));
    assertThat(queries.get(q.getId()).getStatus()).isEqualTo(QuotationStatus.SENT_TO_CLIENT);
    List<OutboundMessage> sent = messages.forRecord("Quotation", String.valueOf(q.getId()));
    assertThat(sent).hasSize(2);
    assertThat(messages.attachments(sent.get(1).getId()))
        .hasSize(2)
        .allSatisfy(a -> assertThat(a.isPasswordProtected()).isTrue());
    assertThat(as.run("ao", () -> notifications.mine(false, PageRequest.of(0, 100)).getContent()))
        .anySatisfy(n -> assertThat(n.getTitle()).startsWith(q.getQuotationNo()));

    assertThatThrownBy(() -> as.run("ao", () -> acceptance.accept(q.getId(), List.of(), null)))
        .extracting("code")
        .isEqualTo("ACCEPTANCE_EMAIL_REQUIRED");
    fx.attach("Quotation", q.getId(), "CLIENT_ACCEPTANCE");
    assertThatThrownBy(() -> as.run("ao", () -> acceptance.accept(q.getId(), List.of(3), null)))
        .extracting("code")
        .isEqualTo("ACCEPTANCE_GROUP_UNKNOWN");
    as.run("ao", () -> acceptance.accept(q.getId(), List.of(1, 2), "accepted by e-mail"));
    Quotation converted = as.run("ao", () -> acceptance.createAccounts(q.getId(), null));
    assertThat(converted.getAccountArns()).containsExactly(q.getArn() + "-01", q.getArn() + "-02");
    assertThat(queries.get(q.getId()).getStatus()).isEqualTo(QuotationStatus.CONVERTED);
    Account first = accounts.requireByArn(q.getArn() + "-01");
    assertThat(first.getQuotationRef()).isEqualTo(q.getQuotationNo());
    assertThat(first.getPaymentArrangement()).isEqualTo(PaymentArrangement.DIRECT_TO_INSURER);
    assertThat(first.getItems()).hasSize(1);
    assertThat(first.getPremium().grossPremium()).isPositive();
    assertThat(first.getSales().accountOfficer()).isEqualTo("ao");

    QuotationSummary summary = queries.getByArn(q.getArn() + "-02");
    assertThat(summary.quotationNo()).isEqualTo(q.getQuotationNo());
    assertThat(summary.directPayment()).isTrue();
    assertThat(summary.accountArns()).hasSize(2);
  }

  @Test
  void prospectMayBeQuotedButNotConvertedAndClientsMayDecline() {
    Client prospect = fx.prospect();
    Quotation q = fx.create(prospect);
    assertThat(q.getClientCode()).startsWith("PR-");
    approved(q);
    as.run("ao", () -> dispatch.send(q.getId(), EMAIL));
    fx.attach("Quotation", q.getId(), "CLIENT_ACCEPTANCE");
    as.run("ao", () -> acceptance.accept(q.getId(), null, null));
    assertThatThrownBy(() -> as.run("ao", () -> acceptance.createAccounts(q.getId(), null)))
        .extracting("code")
        .isEqualTo("CLIENT_NOT_CONFIRMED");

    Quotation other = fx.create(prospect);
    approved(other);
    as.run("ao", () -> dispatch.send(other.getId(), EMAIL));
    as.run("ao", () -> quotations.decline(other.getId(), "went elsewhere"));
    assertThat(queries.get(other.getId()).getStatus()).isEqualTo(QuotationStatus.NOT_PROCEEDED);

    Quotation voided = fx.create(prospect);
    as.run(
        "ao",
        () ->
            workflow.transition(
                "Quotation",
                String.valueOf(voided.getId()),
                "void",
                new TransitionNote("DUPLICATE", "duplicate")));
    assertThat(queries.get(voided.getId()).getStatus()).isEqualTo(QuotationStatus.VOIDED);
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        quotations.update(
                            voided.getId(),
                            motor(prospect.getId(), false, new DraftItem(1, vehicle("1"))))))
        .extracting("code")
        .isEqualTo("QUOTATION_NOT_EDITABLE");
  }

  @Test
  void batchSendGroupsApprovedQuotationsByClientAndRefusesOthers() {
    Client client = fx.client("CL-DEMO-A002");
    Quotation a = approved(fx.create(client));
    Quotation b = approved(fx.create(client));
    Quotation draft = fx.create(client);
    assertThatThrownBy(
            () -> as.run("ao", () -> dispatch.sendBatch(List.of(a.getId(), draft.getId()), null)))
        .extracting("code")
        .isEqualTo("QUOTATION_BATCH_INVALID");
    BatchResult result =
        as.run("ao", () -> dispatch.sendBatch(List.of(a.getId(), b.getId()), null));
    assertThat(result.quotations()).isEqualTo(2);
    assertThat(result.emails()).isEqualTo(1);
    assertThat(queries.get(b.getId()).getStatus()).isEqualTo(QuotationStatus.SENT_TO_CLIENT);
    assertThat(messages.forRecord("Quotation", String.valueOf(a.getId())))
        .first()
        .satisfies(m -> assertThat(messages.attachments(m.getId())).isEmpty());
    assertThatThrownBy(() -> dispatch.sendBatch(List.of(), null))
        .extracting("code")
        .isEqualTo("QUOTATION_BATCH_EMPTY");
  }

  @Test
  void submissionNeedsItemsAndAPremium() {
    Client client = fx.client("CL-DEMO-A001");
    Quotation empty =
        as.run("ao", () -> quotations.create(fx.company(), motor(client.getId(), false)));
    assertThat(empty.getGrossPremium()).isNull();
    assertThatThrownBy(() -> as.run("ao", () -> quotations.submit(empty.getId(), null)))
        .extracting("code")
        .isEqualTo("QUOTATION_NO_ITEMS");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        quotations.create(
                            fx.company(),
                            new com.iortatechnxt.brokerverse.quotation.service.QuotationDraft(
                                null, null, null, null, null, null, null, List.of()))))
        .extracting("code")
        .isEqualTo("QUOTATION_INCOMPLETE");
    assertThat(
            as.run(
                    "ao",
                    () ->
                        quotations.preview(
                            fx.company(), motor(null, false, new DraftItem(1, vehicle("800000")))))
                .content()
                .premium()
                .grossPremium())
        .isPositive();
  }
}
