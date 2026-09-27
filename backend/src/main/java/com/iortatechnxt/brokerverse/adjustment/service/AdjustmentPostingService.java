package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.Computation;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingOutcome;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestClass;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.EndorsementType;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPosting;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPostingService;
import com.iortatechnxt.brokerverse.booking.service.EndorsementResult;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceRegister;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService.FlagChange;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier.ReapplyResult;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posting of an endorsement request (ADJID.001/003/009/011-014, OPERATIONS_DESIGN section 5 rows
 * 16-21), in one transaction:
 *
 * <ol>
 *   <li>recompute with the amounts in force;
 *   <li>financial endorsements and cancellations through booking's {@code
 *       EndorsementPostingService.post} (premium entries, return or endorsement invoice, service
 *       invoice or credit); non-financial endorsements recorded by booking without GL; a commission
 *       change alone, a write-off: own events;
 *   <li>the ledger: {@code ADJUSTED} on the original invoice for a decrease, {@code CANCELLED} flag
 *       for a cancellation, AR Insurer of a remitted decrease;
 *   <li>the payments of a paid invoice re-applied through cashiering; while cashiering is not
 *       installed the request waits in AWAITING_REAPPLICATION;
 *   <li>the invoice's lock and pending negative adjustment released.
 * </ol>
 */
@Service
@Transactional
public class AdjustmentPostingService {

  private static final int TEXT_SIZE = 128;

  private final EndorsementRequestService requests;
  private final RecomputeService recompute;
  private final PremiumDeltas deltas;
  private final EndorsementPostingService endorsements;
  private final ServiceInvoiceRegister serviceInvoices;
  private final CommissionAdjuster commissions;
  private final WriteOffService writeOffs;
  private final LedgerEffects effects;
  private final InvoiceGuard guard;
  private final InvoiceLedgerQueryService queries;
  private final InvoiceLedgerService ledger;
  private final WorkflowService workflow;
  private final RequestNotifier notifier;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param recompute recompute
   * @param deltas cancellation postings
   * @param endorsements booking endorsement posting (contract)
   * @param serviceInvoices service invoices of a booked invoice
   * @param commissions commission-only changes
   * @param writeOffs write-offs
   * @param effects ledger effects
   * @param guard invoice lock and flag
   * @param queries ledger reads
   * @param ledger ledger writes (flags)
   * @param workflow workflow engine
   * @param notifier notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection of the posting collaborators
  public AdjustmentPostingService(
      EndorsementRequestService requests,
      RecomputeService recompute,
      PremiumDeltas deltas,
      EndorsementPostingService endorsements,
      ServiceInvoiceRegister serviceInvoices,
      CommissionAdjuster commissions,
      WriteOffService writeOffs,
      LedgerEffects effects,
      InvoiceGuard guard,
      InvoiceLedgerQueryService queries,
      InvoiceLedgerService ledger,
      WorkflowService workflow,
      RequestNotifier notifier,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.recompute = recompute;
    this.deltas = deltas;
    this.endorsements = endorsements;
    this.serviceInvoices = serviceInvoices;
    this.commissions = commissions;
    this.writeOffs = writeOffs;
    this.effects = effects;
    this.guard = guard;
    this.queries = queries;
    this.ledger = ledger;
    this.workflow = workflow;
    this.notifier = notifier;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Posts a request ready for posting.
   *
   * @param id request
   * @param batchNo posting batch, null for a single posting
   * @return the request, POSTED or AWAITING_REAPPLICATION
   */
  public EndorsementRequest post(Long id, String batchNo) {
    EndorsementRequest request = requests.get(id);
    requireStage(request, RequestStage.FOR_POSTING);
    if (batchNo != null) {
      request.inBatch(batchNo);
    }
    Recompute result =
        recompute.compute(
            request.getSubject().invoiceNo(),
            request.getComputation(),
            request.getTerms(),
            request.getAmounts());
    request.recordRecompute(result.components(), result.shares());
    List<String> journals = new ArrayList<>();
    PostingOutcome outcome = postAmounts(request, result, journals);
    Settled settled = settle(request, result, outcome, journals);
    workflow.transition(
        Adjustments.ENTITY,
        String.valueOf(id),
        settled.pending() ? "post_pending" : "post",
        TransitionNote.comment(batchNo));
    request.posted(settled.outcome(), journals, currentUser.username(), clock.instant());
    notifier.requester(request, settled.pending() ? "posted - payments to re-apply" : "posted");
    audit.record(
        Adjustments.ENTITY, request.getRequestNo(), AuditAction.POST, summary(request, settled));
    return request;
  }

  /**
   * The ledger after the amounts are posted: cancellation flag, AR Insurer of a remitted decrease,
   * payments re-applied, then the lock and the pending negative adjustment released.
   */
  private Settled settle(
      EndorsementRequest request, Recompute result, PostingOutcome outcome, List<String> journals) {
    if (request.getComputation().isCancellation()) {
      ledger.setFlag(
          new FlagChange(
              request.getSubject().invoiceNo(),
              InvoiceFlag.CANCELLED,
              true,
              Adjustments.MODULE,
              request.getRequestNo()));
    }
    BigDecimal arInsurer =
        result.reduces() && outcome.newInvoiceNo() != null
            ? effects.setUpArInsurer(request, journals)
            : BigDecimal.ZERO;
    PostingOutcome settled = withArInsurer(outcome, arInsurer);
    boolean pending = false;
    if (result.settlement().reapplication()) {
      guard.release(request);
      Optional<ReapplyResult> reapplied = effects.reapply(request);
      pending = reapplied.isEmpty();
      PostingOutcome posted = settled;
      settled = reapplied.map(r -> withExcess(posted, r)).orElse(posted);
    }
    if (!pending && arInsurer.signum() == 0) {
      guard.clearNegative(request);
    }
    guard.release(request);
    return new Settled(settled, pending);
  }

  private static String summary(EndorsementRequest request, Settled settled) {
    PostingOutcome outcome = settled.outcome();
    StringBuilder text =
        new StringBuilder(TEXT_SIZE).append("Posted ").append(request.getComputation());
    if (outcome.newInvoiceNo() != null) {
      text.append(" as ").append(outcome.newInvoiceNo());
    }
    if (settled.pending()) {
      text.append("; payments to re-apply");
    }
    if (outcome.arInsurerAmount() != null) {
      text.append("; AR Insurer ").append(outcome.arInsurerAmount().toPlainString());
    }
    return text.toString();
  }

  /**
   * Re-applies the payments of a request posted while cashiering was not available.
   *
   * @param id request
   * @return the request, POSTED
   */
  public EndorsementRequest reapply(Long id) {
    EndorsementRequest request = requests.get(id);
    requireStage(request, RequestStage.AWAITING_REAPPLICATION);
    ReapplyResult result =
        effects
            .reapply(request)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        LedgerEffects.REAPPLIER_UNAVAILABLE,
                        "Payments can only be re-applied once Cashiering is available; the"
                            + " request stays awaiting re-application"));
    BigDecimal arInsurer = request.outcome().arInsurerAmount();
    if (arInsurer == null || arInsurer.signum() == 0) {
      guard.clearNegative(request);
    }
    workflow.transition(Adjustments.ENTITY, String.valueOf(id), "reapply", TransitionNote.NONE);
    request.reapplied(withExcess(request.outcome(), result), clock.instant());
    notifier.requester(request, "completed: payments re-applied");
    audit.record(
        Adjustments.ENTITY,
        request.getRequestNo(),
        AuditAction.POST,
        "Payments re-applied; excess " + result.excess().toPlainString());
    return request;
  }

  private PostingOutcome postAmounts(
      EndorsementRequest request, Recompute result, List<String> journals) {
    OpsInvoice invoice = queries.require(request.getSubject().invoiceNo());
    Computation computation = request.getComputation();
    PostingOutcome base = request.outcome();
    if (computation == Computation.NONE) {
      return recordOnly(request, base);
    }
    if (computation == Computation.WRITE_OFF) {
      String journal =
          writeOffs
              .writeOff(invoice.getInvoiceNo(), request.getRequestNo(), false)
              .getJournalBatchNo();
      journals.add(journal);
      return base;
    }
    if (computation == Computation.AMOUNTS && result.premium().isZero()) {
      List<String> documents = commissions.adjust(request, invoice, result, journals);
      return outcome(base, null, null, documents);
    }
    EndorsementResult posted = endorsements.post(bookingPosting(request, invoice, result));
    journals.addAll(posted.journalBatches());
    if (posted.invoice() != null && posted.invoice().kind().isNegative()) {
      effects.adjustOriginal(
          request,
          posted.invoice(),
          posted.journalBatches().isEmpty() ? null : posted.journalBatches().get(0));
    }
    List<String> documents =
        serviceInvoices.forInvoice(posted.invoiceNo()).stream()
            .map(ServiceInvoice::getSiNo)
            .toList();
    return outcome(base, posted.endorsementNo(), posted.invoiceNo(), documents);
  }

  private PostingOutcome recordOnly(EndorsementRequest request, PostingOutcome base) {
    if (request.getRequestClass() != RequestClass.NON_FINANCIAL) {
      return base;
    }
    EndorsementResult recorded =
        endorsements.post(
            new EndorsementPosting(
                request.getSubject().arn(),
                EndorsementType.NON_FINANCIAL,
                null,
                request.getTerms().effectiveDate(),
                null,
                null,
                null,
                null,
                null,
                request.getTerms().description(),
                request.getTerms().reasonCode(),
                null,
                Adjustments.sourceRef(request.getRequestNo())));
    return outcome(base, recorded.endorsementNo(), null, List.of());
  }

  private EndorsementPosting bookingPosting(
      EndorsementRequest request, OpsInvoice invoice, Recompute result) {
    String ref = Adjustments.sourceRef(request.getRequestNo());
    if (request.getComputation().isCancellation()) {
      return deltas.cancellationPosting(invoice, request.getComputation(), request.getTerms(), ref);
    }
    return new EndorsementPosting(
        invoice.getArn(),
        result.premium().total().signum() > 0 ? EndorsementType.POSITIVE : EndorsementType.NEGATIVE,
        null,
        request.getTerms().effectiveDate(),
        PremiumDeltas.basisOf(request.getTerms().refundBasis()),
        null,
        null,
        result.premium(),
        result.commission(),
        request.getTerms().description(),
        request.getTerms().reasonCode(),
        null,
        ref);
  }

  private static PostingOutcome outcome(
      PostingOutcome base, String endorsementNo, String invoiceNo, List<String> documents) {
    return new PostingOutcome(
        base.batchNo(),
        endorsementNo,
        invoiceNo,
        documents.isEmpty() ? null : documents.stream().collect(Collectors.joining(", ")),
        null,
        null,
        null);
  }

  private static PostingOutcome withArInsurer(PostingOutcome outcome, BigDecimal arInsurer) {
    return new PostingOutcome(
        outcome.batchNo(),
        outcome.endorsementNo(),
        outcome.newInvoiceNo(),
        outcome.serviceInvoices(),
        arInsurer.signum() > 0 ? arInsurer : null,
        outcome.excessAmount(),
        outcome.unappliedRef());
  }

  /**
   * The outcome of a posting and whether payments wait for re-application.
   *
   * @param outcome posting outcome
   * @param pending payments to re-apply
   */
  private record Settled(PostingOutcome outcome, boolean pending) {}

  private static PostingOutcome withExcess(PostingOutcome outcome, ReapplyResult result) {
    return outcome.withReapplication(result.excess(), result.unappliedRef());
  }

  private static void requireStage(EndorsementRequest request, RequestStage expected) {
    if (request.getStage() != expected) {
      throw new BusinessRuleException(
          "ADJ_WRONG_STAGE",
          request.getRequestNo() + " is " + request.getStage() + ", not " + expected);
    }
  }
}
