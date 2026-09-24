package com.iortatechnxt.brokerverse.adjustment;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingBatch;
import com.iortatechnxt.brokerverse.adjustment.domain.RefundBasis;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService;
import com.iortatechnxt.brokerverse.adjustment.service.EndorsementRequestService;
import com.iortatechnxt.brokerverse.adjustment.service.PostingBatchService;
import com.iortatechnxt.brokerverse.adjustment.service.RequestDraft;
import com.iortatechnxt.brokerverse.adjustment.service.RequestWorkflowService;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Adjustment test data: motor invoices booked through booking (and copied to the ledger), requests
 * raised and moved through the real services as the demo users (mktcoll requests, adjust validates
 * and posts, adjtl approves), payments and remittances posted on the ledger as the cashiering and
 * remittance modules would.
 */
@Component
public class AdjustmentFixtures {

  /** Requester (Marketing Collection). */
  public static final String REQUESTER = "mktcoll";

  /** Adjustment processor (validates, posts). */
  public static final String PROCESSOR = "adjust";

  /** Adjustment team leader (approves). */
  public static final String LEADER = "adjtl";

  /** Start of the test accounts' cover. */
  public static final LocalDate FROM = BookingFixtures.FROM;

  private final OpsLedgerFixtures ledgerFx;
  private final EndorsementRequestService requests;
  private final RequestWorkflowService workflow;
  private final PostingBatchService batches;
  private final AdjustmentQueryService queries;
  private final InvoiceLedgerService ledger;
  private final InvoiceLedgerQueryService ledgerQueries;
  private final AsUser as;
  private final TransactionTemplate tx;

  AdjustmentFixtures(
      OpsLedgerFixtures ledgerFx,
      EndorsementRequestService requests,
      RequestWorkflowService workflow,
      PostingBatchService batches,
      AdjustmentQueryService queries,
      InvoiceLedgerService ledger,
      InvoiceLedgerQueryService ledgerQueries,
      AsUser as,
      TransactionTemplate tx) {
    this.ledgerFx = ledgerFx;
    this.requests = requests;
    this.workflow = workflow;
    this.batches = batches;
    this.queries = queries;
    this.ledger = ledger;
    this.ledgerQueries = ledgerQueries;
    this.as = as;
    this.tx = tx;
  }

  /** The demo company. */
  public Long company() {
    return ledgerFx.company();
  }

  /** A new booked motor invoice in the ledger. */
  public OpsInvoice invoice() {
    return ledgerFx.motorInvoice();
  }

  /** The invoice as it is now. */
  public OpsInvoice reload(OpsInvoice invoice) {
    return ledgerQueries.require(invoice.getInvoiceNo());
  }

  /** A request as it is now (collections loaded). */
  public EndorsementRequest reload(EndorsementRequest request) {
    return queries.get(request.getId());
  }

  /** Terms of a request. */
  public static RequestTerms terms(
      String type, String requestType, String reason, LocalDate effective, BigDecimal tsi) {
    return new RequestTerms(
        type,
        requestType,
        reason,
        null,
        effective,
        RefundBasis.PRO_RATA,
        tsi,
        null,
        null,
        null,
        "Test " + (requestType == null ? type : requestType),
        null);
  }

  /** Terms of a flat, flat retain-DST or partial cancellation. */
  public static RequestTerms cancellation(String requestType, LocalDate effective) {
    return terms("FIN_CHANGE_COVER", requestType, "UNIT_SOLD", effective, null);
  }

  /** Amounts changing the basic premium only (commission derived). */
  public static AmountInput basic(String amount) {
    return new AmountInput(new BigDecimal(amount), null, null, null, null, null, null, null);
  }

  /** Raises a request as the requester. */
  public EndorsementRequest raise(OpsInvoice invoice, RequestTerms terms, AmountInput amounts) {
    return raise(invoice, terms, amounts, null);
  }

  /** Raises a request with a duplicate justification. */
  public EndorsementRequest raise(
      OpsInvoice invoice, RequestTerms terms, AmountInput amounts, String duplicateOverride) {
    RequestDraft draft =
        new RequestDraft(invoice.getInvoiceNo(), terms, amounts, duplicateOverride, null);
    return as.run(REQUESTER, () -> requests.create(draft));
  }

  /** Submits, validates and (when needed) approves a request. */
  public EndorsementRequest toPosting(EndorsementRequest request) {
    Long id = request.getId();
    as.run(REQUESTER, () -> workflow.submit(id, null));
    EndorsementRequest validated = as.run(PROCESSOR, () -> workflow.validate(id, null));
    if (validated.getStage() == RequestStage.FOR_APPROVAL) {
      as.run(LEADER, () -> workflow.approve(id, null));
    }
    return queries.get(id);
  }

  /** Posts requests as one batch (as the processor). */
  public PostingBatch post(EndorsementRequest... posted) {
    List<Long> ids = java.util.Arrays.stream(posted).map(EndorsementRequest::getId).toList();
    return as.run(PROCESSOR, () -> batches.post(company(), ids, "Test batch"));
  }

  /** Raises, moves and posts a request; returns it as posted. */
  public EndorsementRequest raiseAndPost(
      OpsInvoice invoice, RequestTerms terms, AmountInput amounts) {
    EndorsementRequest request = toPosting(raise(invoice, terms, amounts));
    PostingBatch batch = post(request);
    PostingBatch.Line line = batch.getLines().get(0);
    if (line.message() != null) {
      throw new IllegalStateException(line.message());
    }
    return queries.get(request.getId());
  }

  /** Applies payments for every outstanding premium component (cashiering). */
  public void payInFull(OpsInvoice invoice) {
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    reload(invoice).getComponents().stream()
        .filter(c -> c.getComponent().isPremiumReceivable())
        .forEach(c -> amounts.put(c.getComponent(), c.getBalance()));
    move(invoice, MovementType.APPLIED, "CASHIERING", amounts);
  }

  /** Applies a payment leaving a balance on the basic premium. */
  public void payAllBut(OpsInvoice invoice, BigDecimal left) {
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    for (OpsInvoiceComponent c : reload(invoice).getComponents()) {
      if (c.getComponent().isPremiumReceivable()) {
        BigDecimal pay =
            c.getComponent() == LedgerComponent.BASIC
                ? c.getBalance().subtract(left)
                : c.getBalance();
        amounts.put(c.getComponent(), pay);
      }
    }
    move(invoice, MovementType.APPLIED, "CASHIERING", amounts);
  }

  /** Applies a payment above the basic premium due (overpayment, credit balance). */
  public void overpay(OpsInvoice invoice, BigDecimal extra) {
    move(invoice, MovementType.APPLIED, "CASHIERING", Map.of(LedgerComponent.BASIC, extra));
  }

  /** Remits the whole DTIP to the insurer (remittance). */
  public void remitInFull(OpsInvoice invoice) {
    BigDecimal dtip = reload(invoice).component(LedgerComponent.DTIP).getBalance();
    move(invoice, MovementType.REMITTED, "REMITTANCE", Map.of(LedgerComponent.DTIP, dtip));
    as.run(
        "remit",
        () ->
            tx.execute(
                s ->
                    ledger.setRemittanceStatus(
                        invoice.getInvoiceNo(),
                        RemittanceStatus.FULLY_REMITTED,
                        "REMITTANCE",
                        "Test remittance")));
  }

  private void move(
      OpsInvoice invoice,
      MovementType type,
      String module,
      Map<LedgerComponent, BigDecimal> amounts) {
    String ref = type + ":" + BookingFixtures.token();
    as.run(
        "proc",
        () ->
            tx.execute(
                s ->
                    ledger.post(
                        new MovementRequest(
                            invoice.getInvoiceNo(),
                            type,
                            module,
                            ref,
                            LocalDate.of(2026, 9, 20),
                            amounts,
                            null,
                            "Test " + type))));
  }
}
