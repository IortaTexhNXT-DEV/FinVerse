package com.iortatechnxt.brokerverse.adjustment;

import static com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures.FROM;
import static com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures.LEADER;
import static com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures.PROCESSOR;
import static com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures.REQUESTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.Computation;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestClass;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentDocuments;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentRelatedItems;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentWorkCounts;
import com.iortatechnxt.brokerverse.adjustment.service.EndorsementRequestService;
import com.iortatechnxt.brokerverse.adjustment.service.PostingBatchService;
import com.iortatechnxt.brokerverse.adjustment.service.RequestDraft;
import com.iortatechnxt.brokerverse.adjustment.service.RequestWorkflowService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems.RelatedItem;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource.WorkCount;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Endorsement requests before posting (ADJID.001-008/010/015/018/020-025): the invoice lock,
 * duplicates with override, the workflow with four eyes and returns, non-financial, internal and
 * commission-only requests, the "quotation required" hand-off, slips, invoice 360 items and work
 * tiles.
 */
@IntegrationTest
class AdjustmentRequestIT {

  @Autowired private AdjustmentFixtures fx;
  @Autowired private EndorsementRequestService requests;
  @Autowired private RequestWorkflowService workflow;
  @Autowired private PostingBatchService batches;
  @Autowired private AdjustmentDocuments documents;
  @Autowired private AdjustmentRelatedItems related;
  @Autowired private AdjustmentWorkCounts counts;
  @Autowired private InvoiceLedgerService ledger;
  @Autowired private WorkflowService workflows;
  @Autowired private WorkCaseRepository cases;
  @Autowired private AsUser as;
  @Autowired private TransactionTemplate tx;

  private static RequestTerms descriptive(String reference) {
    return new RequestTerms(
        "NF_DESCRIPTIVE",
        null,
        null,
        reference,
        FROM.plusDays(10),
        null,
        null,
        null,
        null,
        null,
        "Correct the plate number",
        null);
  }

  private void cancel(EndorsementRequest request) {
    Long caseId =
        cases
            .findByEntityTypeAndEntityId("EndorsementRequest", String.valueOf(request.getId()))
            .orElseThrow()
            .getId();
    as.run(
        REQUESTER,
        () ->
            tx.execute(
                s -> workflows.genericTransition(caseId, "cancel", TransitionNote.comment("x"))));
  }

  @Test
  void anOpenRequestLocksTheInvoiceAgainstRemittanceUntilItEnds() {
    OpsInvoice invoice = fx.invoice();
    EndorsementRequest request =
        fx.raise(
            invoice, AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM), AmountInput.NONE);

    assertThat(request.getRequestNo()).startsWith("ENR-");
    assertThat(request.getStage()).isEqualTo(RequestStage.DRAFT);
    assertThat(request.getRequestClass()).isEqualTo(RequestClass.FINANCIAL);
    assertThat(request.getComputation()).isEqualTo(Computation.CANCELLATION_FLAT);
    assertThat(request.getSubject().arn()).isEqualTo(invoice.getArn());
    assertThat(fx.reload(invoice).getLockOwner()).isEqualTo("ADJUSTMENT");
    assertThatThrownBy(
            () ->
                as.run(
                    "remit",
                    () ->
                        tx.execute(
                            s -> ledger.lock(invoice.getInvoiceNo(), "REMITTANCE", "Batch"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", "INVOICE_LOCKED");

    cancel(request);
    assertThat(fx.reload(request).getStage()).isEqualTo(RequestStage.CANCELLED);
    assertThat(fx.reload(request).trail().completedAt()).isNotNull();
    assertThat(fx.reload(invoice).getLockOwner()).isNull();
  }

  @Test
  void anInvoiceInTheRemittanceQueueCannotBeAdjusted() {
    OpsInvoice invoice = fx.invoice();
    as.run(
        "remit",
        () -> tx.execute(s -> ledger.lock(invoice.getInvoiceNo(), "REMITTANCE", "In batch")));

    assertThatThrownBy(
            () ->
                fx.raise(
                    invoice,
                    AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM),
                    AmountInput.NONE))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", "INVOICE_LOCKED");
  }

  @Test
  void aDuplicateNeedsAJustificationAndACancellationExcludesOtherChanges() {
    OpsInvoice invoice = fx.invoice();
    fx.raise(invoice, descriptive("END-DUP-1"), AmountInput.NONE);

    assertThatThrownBy(() -> fx.raise(invoice, descriptive("END-DUP-1"), AmountInput.NONE))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", "ADJ_DUPLICATE_REQUEST");
    EndorsementRequest overridden =
        fx.raise(invoice, descriptive("END-DUP-1"), AmountInput.NONE, "Second insurer copy");
    assertThat(overridden.getDuplicateOverride()).isEqualTo("Second insurer copy");
    assertThat(
            as.run(
                REQUESTER,
                () ->
                    requests
                        .preview(
                            new RequestDraft(
                                invoice.getInvoiceNo(), descriptive("END-DUP-1"), null, null, null))
                        .duplicates()))
        .hasSize(2);

    fx.raise(invoice, AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM), AmountInput.NONE);
    assertThatThrownBy(
            () ->
                fx.raise(
                    invoice,
                    AdjustmentFixtures.terms(
                        "FIN_TSI", "TSI_CHANGE", null, FROM, new BigDecimal("-100000")),
                    AmountInput.NONE))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", "ADJ_INCOMPATIBLE_REQUEST");
  }

  @Test
  void invalidRequestsAreRefusedWithTheirReason() {
    OpsInvoice invoice = fx.invoice();

    assertThatThrownBy(
            () ->
                fx.raise(
                    invoice,
                    AdjustmentFixtures.terms("FIN_TSI", null, null, FROM, BigDecimal.TEN),
                    AmountInput.NONE))
        .hasFieldOrPropertyWithValue("code", "ADJ_REQUEST_TYPE_REQUIRED");
    assertThatThrownBy(
            () ->
                fx.raise(
                    invoice,
                    AdjustmentFixtures.terms("NF_DESCRIPTIVE", "TSI_CHANGE", null, FROM, null),
                    AmountInput.NONE))
        .hasFieldOrPropertyWithValue("code", "ADJ_NON_FINANCIAL_WITH_AMOUNTS");
    assertThatThrownBy(
            () ->
                fx.raise(
                    invoice,
                    AdjustmentFixtures.terms(
                        "FIN_CHANGE_COVER", "FLAT_CANCELLATION", null, FROM, null),
                    AmountInput.NONE))
        .hasFieldOrPropertyWithValue("code", "ADJ_CANCELLATION_REASON_REQUIRED");
    assertThatThrownBy(
            () ->
                fx.raise(
                    invoice,
                    AdjustmentFixtures.terms(
                        "FIN_TSI", "TSI_CHANGE", null, FROM.minusDays(1), BigDecimal.TEN),
                    AmountInput.NONE))
        .hasFieldOrPropertyWithValue("code", "ADJ_EFFECTIVE_DATE_OUTSIDE_TERM");
    assertThatThrownBy(
            () ->
                fx.raise(
                    invoice,
                    AdjustmentFixtures.terms("NF_PERIOD_CHANGE", null, null, FROM, null),
                    AmountInput.NONE))
        .hasFieldOrPropertyWithValue("code", "ADJ_PERIOD_INVALID");
    assertThatThrownBy(
            () ->
                fx.raise(
                    invoice,
                    AdjustmentFixtures.terms(
                        "FIN_PREMIUM_RATE", "PREMIUM_RATE_CHANGE", null, FROM, null),
                    AmountInput.NONE))
        .hasFieldOrPropertyWithValue("code", "ADJ_AMOUNTS_REQUIRED");
  }

  @Test
  void approvalIsFourEyesAndAReturnCarriesItsReason() {
    EndorsementRequest request =
        fx.raise(
            fx.invoice(),
            AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM),
            AmountInput.NONE);
    Long id = request.getId();
    as.run(REQUESTER, () -> workflow.submit(id, "Please process"));
    assertThat(fx.reload(request).getStage()).isEqualTo(RequestStage.FOR_VALIDATION);
    int returned =
        as.run(
            PROCESSOR,
            () -> batches.returnRequests(List.of(id), "INCOMPLETE_DOCUMENTS", "Deed of sale"));
    assertThat(returned).isEqualTo(1);
    EndorsementRequest back = fx.reload(request);
    assertThat(back.getStage()).isEqualTo(RequestStage.RETURNED);
    assertThat(back.getReturnReason()).isEqualTo("INCOMPLETE_DOCUMENTS");
    assertThat(back.getReturnComment()).isEqualTo("Deed of sale");

    as.run(REQUESTER, () -> workflow.resubmit(id, "Attached"));
    as.run(LEADER, () -> workflow.validate(id, null));
    assertThat(fx.reload(request).getStage()).isEqualTo(RequestStage.FOR_APPROVAL);
    assertThatThrownBy(() -> as.run(LEADER, () -> workflow.approve(id, null)))
        .hasFieldOrPropertyWithValue("code", "ADJ_FOUR_EYES");
    assertThatThrownBy(() -> as.run(REQUESTER, () -> workflow.approve(id, null)))
        .hasFieldOrPropertyWithValue("code", "ADJ_FOUR_EYES");
  }

  @Test
  void aNonFinancialEndorsementIsRecordedByBookingWithoutApproval() {
    OpsInvoice invoice = fx.invoice();
    EndorsementRequest request =
        fx.toPosting(fx.raise(invoice, descriptive(null), AmountInput.NONE));
    assertThat(request.isNeedsApproval()).isFalse();
    assertThat(request.getStage()).isEqualTo(RequestStage.FOR_POSTING);
    fx.post(request);

    EndorsementRequest posted = fx.reload(request);
    assertThat(posted.getStage()).isEqualTo(RequestStage.POSTED);
    assertThat(posted.outcome().endorsementNo()).startsWith("EN-");
    assertThat(posted.outcome().newInvoiceNo()).isNull();
    assertThat(posted.getJournals()).isEmpty();
    assertThat(fx.reload(invoice).premiumBalance()).isEqualByComparingTo(invoice.premiumBalance());
  }

  @Test
  void aCommissionChangeAloneIssuesAServiceInvoice() {
    OpsInvoice invoice = fx.invoice();
    AmountInput commission =
        new AmountInput(
            null, null, null, null, null, null, new BigDecimal("100.00"), new BigDecimal("12.00"));
    EndorsementRequest request =
        fx.raiseAndPost(
            invoice,
            AdjustmentFixtures.terms("FIN_COMMISSION_RATE", "COMMISSION_CHANGE", null, FROM, null),
            commission);

    assertThat(request.getStage()).isEqualTo(RequestStage.POSTED);
    assertThat(request.outcome().serviceInvoices()).startsWith("SI-");
    assertThat(request.getJournals()).hasSize(1);
    OpsInvoice after = fx.reload(invoice);
    assertThat(after.component(LedgerComponent.COMMISSION).getBalance())
        .isEqualByComparingTo(
            invoice.component(LedgerComponent.COMMISSION).getBalance().add(new BigDecimal("100")));
    assertThat(after.premiumBalance()).isEqualByComparingTo(invoice.premiumBalance());
  }

  @Test
  void aTsiIncreaseAboveThePackageLimitNeedsAQuotation() {
    OpsInvoice invoice = fx.invoice();
    RequestDraft draft =
        new RequestDraft(
            invoice.getInvoiceNo(),
            AdjustmentFixtures.terms(
                "FIN_TSI", "TSI_CHANGE", null, FROM, new BigDecimal("5000000")),
            AmountInput.NONE,
            null,
            null);
    Long first = as.run(REQUESTER, () -> requests.create(draft)).getId();
    assertThatThrownBy(() -> as.run(REQUESTER, () -> workflow.submit(first, null)))
        .hasFieldOrPropertyWithValue("code", "ADJ_OVER_BASELINE");
    EndorsementRequest request =
        as.run(
            REQUESTER,
            () ->
                requests.create(
                    new RequestDraft(
                        invoice.getInvoiceNo(),
                        draft.terms(),
                        AmountInput.NONE,
                        "Second request for the quotation",
                        "Fleet addition agreed with the client")));
    assertThat(request.isQuotationRequired()).isTrue();
    Long id = request.getId();
    as.run(REQUESTER, () -> workflow.submit(id, null));
    assertThat(fx.reload(request).getHandoffRef()).isNotNull();
    assertThatThrownBy(() -> as.run(PROCESSOR, () -> workflow.validate(id, null)))
        .hasFieldOrPropertyWithValue("code", "ADJ_QUOTATION_REQUIRED");

    as.run(REQUESTER, () -> requests.linkQuotation(id, "QT-2026-000777"));
    as.run(PROCESSOR, () -> workflow.validate(id, null));
    EndorsementRequest validated = fx.reload(request);
    assertThat(validated.getQuotationRef()).isEqualTo("QT-2026-000777");
    assertThat(validated.getStage()).isEqualTo(RequestStage.FOR_APPROVAL);
  }

  @Test
  void slipsTileAndInvoiceItemsShowTheRequest() {
    OpsInvoice invoice = fx.invoice();
    EndorsementRequest request =
        fx.raise(
            invoice, AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM), AmountInput.NONE);

    assertThatThrownBy(() -> as.run(REQUESTER, () -> documents.validationSlip(request.getId())))
        .hasFieldOrPropertyWithValue("code", "ADJ_NOT_VALIDATED");
    AdjustmentDocuments.Generated slip =
        as.run(REQUESTER, () -> documents.endorsementSlip(request.getId()));
    assertThat(new String(slip.content(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    assertThat(fx.reload(request).getSlipNo()).startsWith("ES-");
    assertThat(slip.fileName()).isEqualTo(fx.reload(request).getSlipNo() + ".pdf");

    EndorsementRequest posting = fx.toPosting(request);
    assertThat(posting.trail().validatedBy()).isEqualTo(PROCESSOR);
    AdjustmentDocuments.Generated validation =
        as.run(PROCESSOR, () -> documents.validationSlip(request.getId()));
    assertThat(validation.content()).isNotEmpty();

    List<RelatedItem> items = related.itemsFor(invoice.getInvoiceNo());
    assertThat(items).anySatisfy(i -> assertThat(i.reference()).isEqualTo(request.getRequestNo()));
    List<WorkCount> tiles = counts.counts(fx.company());
    assertThat(tiles)
        .anySatisfy(
            t -> {
              assertThat(t.key()).isEqualTo("ADJ_FOR_POSTING");
              assertThat(t.count()).isPositive();
            });
  }

  @Test
  void anInternalAdjustmentHasNoEndorsementSlip() {
    EndorsementRequest request =
        fx.raise(
            fx.invoice(),
            AdjustmentFixtures.terms("INT_ADJUSTMENT", null, null, FROM, null),
            AmountInput.NONE);

    assertThat(request.getRequestClass()).isEqualTo(RequestClass.INTERNAL);
    assertThatThrownBy(() -> as.run(REQUESTER, () -> documents.endorsementSlip(request.getId())))
        .hasFieldOrPropertyWithValue("code", "ADJ_NO_SLIP_FOR_INTERNAL");
    EndorsementRequest posting = fx.toPosting(request);
    fx.post(posting);
    EndorsementRequest posted = fx.reload(request);
    assertThat(posted.getStage()).isEqualTo(RequestStage.POSTED);
    assertThat(posted.outcome().endorsementNo()).isNull();
  }
}
