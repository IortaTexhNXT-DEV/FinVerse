package com.iortatechnxt.brokerverse.acsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.acsl.domain.AcslCase;
import com.iortatechnxt.brokerverse.acsl.domain.CaseOutcome;
import com.iortatechnxt.brokerverse.acsl.domain.CaseStage;
import com.iortatechnxt.brokerverse.acsl.domain.CaseType;
import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionLine;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionLineValues;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionStage;
import com.iortatechnxt.brokerverse.acsl.domain.LineOrigin;
import com.iortatechnxt.brokerverse.acsl.service.AcslApprovalSource;
import com.iortatechnxt.brokerverse.acsl.service.CaseLinks;
import com.iortatechnxt.brokerverse.acsl.service.CaseService;
import com.iortatechnxt.brokerverse.acsl.service.CaseService.CaseDraft;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionJournals.OriginalLine;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionPosting;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionService;
import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.PaymentReversalCompleted;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemRepository;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Correction entries (ACSL 2.7.0-2.15.0, 2.9.1) from assignment to posting, with the journal, the
 * matched open items and the invoice ledger CORRECTION movements; and the investigation cases (ACSL
 * 2.5.x-2.6.x) with the payment reversal request and the AO message.
 */
@IntegrationTest
class AcslCorrectionIT {

  @Autowired private AcslFixtures fx;
  @Autowired private CorrectionService corrections;
  @Autowired private CorrectionPosting posting;
  @Autowired private CaseService cases;
  @Autowired private CaseLinks links;
  @Autowired private AcslApprovalSource approvals;
  @Autowired private WorkflowService workflow;
  @Autowired private WorkflowViewService views;
  @Autowired private JournalBatchRepository journals;
  @Autowired private OpenItemRepository openItems;
  @Autowired private InvoiceLedgerQueryService ledger;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;

  private Long caseIdOf(String entity, Long id) {
    return views.view(entity, String.valueOf(id)).orElseThrow().workCase().getId();
  }

  private Correction prepared(OpsInvoice invoice, OriginalLine original) {
    Correction raised =
        as.run(
            "acsltl",
            () ->
                corrections.create(
                    fx.company(),
                    new CorrectionService.Draft(
                        "WRONG_ACCOUNT",
                        invoice.getInvoiceNo(),
                        original.batchNo(),
                        "Basic premium booked as other charges")));
    assertThat(raised.getStage()).isEqualTo(CorrectionStage.ASSIGNED);
    as.run("acsltl", () -> corrections.assign(raised.getId(), "acsl", "Please prepare"));
    return as.run(
        "acsl",
        () ->
            corrections.proposeWrongAccount(
                raised.getId(),
                new CorrectionService.Proposal(
                    original.batchNo(), original.lineNo(), "1210.06", null, "BASIC", "OTHER")));
  }

  @Test
  void aCorrectionIsReviewedApprovedAndPostedWithOpenItemsAndLedgerMovements() {
    OpsInvoice invoice = fx.invoice();
    OriginalLine original = fx.bookingLine(invoice, "1210.01");
    Correction draft = prepared(invoice, original);
    assertThat(draft.getStage()).isEqualTo(CorrectionStage.DRAFT);
    assertThat(draft.getLines())
        .extracting(CorrectionLine::getOrigin)
        .containsExactly(LineOrigin.REVERSAL, LineOrigin.REPOST);
    assertThat(draft.isBalanced()).isTrue();

    as.run("acsl", () -> corrections.submit(draft.getId(), "Ready"));
    assertThatThrownBy(() -> as.run("acsl", () -> corrections.endorse(draft.getId(), null)))
        .isInstanceOf(RuntimeException.class);
    as.run("acsltl", () -> corrections.endorse(draft.getId(), "Reviewed"));
    assertThat(approvals.pendingFor(ApprovalViewer.user("acslhead", Set.of("ACSL_APPROVE"))))
        .anySatisfy(p -> assertThat(p.reference()).isEqualTo(draft.getCorrectionNo()));
    assertThatThrownBy(() -> as.run("acsltl", () -> posting.approve(draft.getId(), null)))
        .isInstanceOf(RuntimeException.class);

    Correction posted = as.run("acslhead", () -> posting.approve(draft.getId(), "Approved"));
    assertThat(posted.getStage()).isEqualTo(CorrectionStage.POSTED);
    assertThat(posted.getOpenItems()).isEqualTo(2);
    assertThat(posted.getLedgerMovements()).isEqualTo(2);
    Integer lines =
        tx.execute(
            s ->
                journals
                    .findByCompanyIdAndBatchNo(fx.company(), posted.getJournalBatchNo())
                    .orElseThrow()
                    .getLines()
                    .size());
    assertThat(lines).isEqualTo(2);
    assertThat(openItems.findAll())
        .filteredOn(i -> posted.getCorrectionNo().equals(i.getDocumentNo()))
        .hasSize(2)
        .allSatisfy(i -> assertThat(i.outstanding()).isZero());
    assertThat(ledger.movements(invoice.getInvoiceNo()))
        .filteredOn(m -> m.getMovementType() == MovementType.CORRECTION)
        .extracting(m -> m.getComponent(), m -> m.getAmount().signum())
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple(LedgerComponent.BASIC, -1),
            org.assertj.core.groups.Tuple.tuple(LedgerComponent.OTHER, 1));
  }

  @Test
  void aReturnedCorrectionMustBalanceBeforeItIsSubmittedAgain() {
    OpsInvoice invoice = fx.invoice();
    OriginalLine original = fx.bookingLine(invoice, "1210.01");
    Correction draft = prepared(invoice, original);
    as.run("acsl", () -> corrections.submit(draft.getId(), null));
    as.run(
        "acsltl",
        () ->
            workflow.genericTransition(
                caseIdOf("AcslCorrection", draft.getId()),
                "return",
                new TransitionNote("INCOMPLETE_DETAILS", "Add the narration")));
    Correction back = corrections.get(draft.getId());
    assertThat(back.getStage()).isEqualTo(CorrectionStage.DRAFT);
    assertThat(back.getReturnComment()).isEqualTo("Add the narration");

    CorrectionLineValues half =
        new CorrectionLineValues(
            "1210.06",
            BalanceSide.DEBIT,
            new BigDecimal("10.00"),
            original.partyCode(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    as.run("acsl", () -> corrections.saveLines(draft.getId(), List.of(half)));
    assertThatThrownBy(() -> as.run("acsl", () -> corrections.submit(draft.getId(), null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("two lines");
    CorrectionLineValues noParty =
        new CorrectionLineValues(
            "1210.06",
            BalanceSide.CREDIT,
            BigDecimal.TEN,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThatThrownBy(
            () -> as.run("acsl", () -> corrections.saveLines(draft.getId(), List.of(noParty))))
        .isInstanceOf(BusinessRuleException.class);
    CorrectionLineValues badComponent =
        new CorrectionLineValues(
            "4101",
            BalanceSide.CREDIT,
            BigDecimal.TEN,
            null,
            null,
            "BASIC",
            null,
            null,
            null,
            null,
            null,
            null);
    assertThatThrownBy(
            () -> as.run("acsl", () -> corrections.saveLines(draft.getId(), List.of(badComponent))))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        "acsl",
        () ->
            workflow.genericTransition(
                caseIdOf("AcslCorrection", draft.getId()),
                "cancel",
                new TransitionNote("ENCODING_ERROR", null)));
    assertThat(corrections.get(draft.getId()).getStage()).isEqualTo(CorrectionStage.CANCELLED);
  }

  @Test
  void anInvestigationRaisesACorrectionAndAsksCashieringForAReversal() {
    OpsInvoice invoice = fx.invoice();
    AcslCase opened =
        as.run(
            "acsl",
            () ->
                cases.open(
                    fx.company(),
                    new CaseDraft(
                        CaseType.INVESTIGATION,
                        invoice.getInvoiceNo(),
                        "AR-INV-1",
                        new BigDecimal("100.00"),
                        "Short payment investigation",
                        "Client says fully paid")));
    assertThat(opened.getCaseNo()).startsWith("ACS-");
    assertThat(opened.accountOrNone().rootInvoiceNo()).isEqualTo(invoice.getRootInvoiceNo());
    as.run("acsltl", () -> cases.assign(opened.getId(), "acsl", null));
    as.run(
        "acsl",
        () ->
            workflow.genericTransition(
                caseIdOf("AcslCase", opened.getId()), "start", TransitionNote.NONE));
    AcslCase reversal =
        as.run(
            "acsl",
            () ->
                links.requestReversal(
                    opened.getId(), "AR-INV-1", null, "Applied to wrong invoice"));
    assertThat(reversal.getReversalStatus()).isEqualTo("SUBMITTED");
    assertThat(reversal.getReversalRef()).startsWith("PRV-");
    tx.executeWithoutResult(
        s ->
            events.publishEvent(
                new PaymentReversalCompleted(
                    fx.company(), "ACSL", opened.getCaseNo(), true, "REV-1", "Done")));
    assertThat(cases.get(opened.getId()).getReversalStatus()).isEqualTo("APPROVED");

    String ao = invoice.getClassification().aoUsername();
    if (ao == null) {
      assertThatThrownBy(
              () -> as.run("acsl", () -> links.messageAccountOfficer(opened.getId(), "Hi")))
          .isInstanceOf(BusinessRuleException.class);
    } else {
      assertThat(as.run("acsl", () -> links.messageAccountOfficer(opened.getId(), "Short by 100")))
          .isEqualTo(ao);
    }
    assertThatThrownBy(
            () ->
                as.run(
                    "acsl",
                    () -> cases.provideResult(opened.getId(), CaseOutcome.CORRECTION, null)))
        .isInstanceOf(BusinessRuleException.class);

    Correction raised =
        as.run(
            "acsl",
            () ->
                corrections.raiseFromCase(
                    opened.getId(),
                    new CorrectionService.Draft("AMOUNT", null, null, "Payment applied short")));
    assertThat(raised.getInvoiceNo()).isEqualTo(invoice.getInvoiceNo());
    AcslCase closed = cases.get(opened.getId());
    assertThat(closed.getStage()).isEqualTo(CaseStage.CORRECTION);
    assertThat(closed.getCorrectionId()).isEqualTo(raised.getId());
    assertThat(closed.getOutcome()).isEqualTo(CaseOutcome.CORRECTION);
  }
}
