package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ApplicationSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionAction;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition.DispositionDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition.Execution;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionTypeRule;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationService.ApplyOptions;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPosting.PostingContext;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway.DisbursementTicket;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carries out a disposition of an unapplied item (CSHID.024, OPERATIONS_DESIGN section 5 row 8):
 * apply to another invoice or to its DST (application engine), refund (refund payable and a payment
 * request to Disbursement through {@code DisbursementGateway}), reclass or transfer (unapplied
 * collections moved to another client or unit), or others (released). A reversal undoes it; a
 * refund is reversed only once Disbursement returned it.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class DispositionExecutor {

  private static final String PREFIX = "DSP:";
  private static final String RELEASED = "RELEASED";
  private static final String ASSIGNED = "ASSIGNED";

  private final ApplicationService applier;
  private final ApplicationRepository applications;
  private final InvoiceLedgerQueryService ledger;
  private final CashieringPosting posting;
  private final DisbursementGateway disbursement;
  private final AuditTrailService audit;
  private final CollectorRequestTracker collectorRequests;
  private final Clock clock;

  /**
   * Creates the executor.
   *
   * @param applier application engine
   * @param applications applications
   * @param ledger invoice ledger
   * @param posting accounting events
   * @param disbursement Disbursement gateway
   * @param audit audit trail
   * @param collectorRequests collector requests of the dispositions (BRCLXN.030-033)
   * @param clock clock
   */
  public DispositionExecutor(
      ApplicationService applier,
      ApplicationRepository applications,
      InvoiceLedgerQueryService ledger,
      CashieringPosting posting,
      DisbursementGateway disbursement,
      AuditTrailService audit,
      CollectorRequestTracker collectorRequests,
      Clock clock) {
    this.applier = applier;
    this.applications = applications;
    this.ledger = ledger;
    this.posting = posting;
    this.disbursement = disbursement;
    this.audit = audit;
    this.collectorRequests = collectorRequests;
    this.clock = clock;
  }

  /**
   * Checks the fields a disposition type needs.
   *
   * @param item unapplied item
   * @param rule type rule
   * @param d amount and target fields
   */
  public void validate(Unapplied item, DispositionTypeRule rule, DispositionDetails d) {
    requireAmount(item, d.amount());
    switch (rule.getAction()) {
      case APPLY, DST_APPLY -> requireInvoice(item, d.targetInvoiceNo());
      case RECLASS -> requireText(d.targetClientCode(), "the client to reclass to");
      case TRANSFER -> requireText(d.targetUnit(), "the marketing unit to transfer to");
      default -> {
        // refund and others need no target
      }
    }
    boolean moves =
        rule.getAction() == DispositionAction.RECLASS
            || rule.getAction() == DispositionAction.TRANSFER;
    if (moves && d.amount().compareTo(item.getBalance()) != 0) {
      throw new BusinessRuleException(
          "DISPOSITION_WHOLE_BALANCE",
          "A reclass or transfer moves the whole balance " + item.getBalance());
    }
  }

  private static void requireAmount(Unapplied item, BigDecimal amount) {
    if (amount == null || amount.signum() <= 0 || amount.compareTo(item.getBalance()) > 0) {
      throw new BusinessRuleException(
          "DISPOSITION_AMOUNT",
          "The amount must be above zero and at most the balance " + item.getBalance());
    }
  }

  /**
   * Executes a disposition.
   *
   * @param item unapplied item
   * @param d disposition
   */
  public void execute(Unapplied item, Disposition d) {
    String ref = PREFIX + d.getId();
    Execution result =
        switch (d.getAction()) {
          case APPLY, DST_APPLY -> apply(item, d, ref);
          case REFUND -> refund(item, d, ref);
          case RECLASS, TRANSFER -> move(item, d, ref);
          case MANUAL -> {
            item.consume(d.getAmount());
            yield new Execution(null, null, null, null, null);
          }
        };
    d.complete(result, clock.instant());
    audit.record(
        UnappliedService.ENTITY,
        item.getReference(),
        AuditAction.POST,
        d.getDispositionType() + " " + d.getAmount() + " executed (" + ref + ")");
    collectorRequests.executed(item, d);
  }

  /**
   * Undoes an executed disposition.
   *
   * @param item unapplied item
   * @param d completed disposition
   */
  public void reverse(Unapplied item, Disposition d) {
    String ref = PREFIX + d.getId() + ":REV";
    switch (d.getAction()) {
      case APPLY, DST_APPLY -> {
        Application app =
            applications
                .findById(d.getApplicationId())
                .orElseThrow(
                    () ->
                        new BusinessRuleException("DISPOSITION_NO_APPLICATION", "No application"));
        applier.reverse(app, ledger.require(app.getInvoiceNo()), ref, d.getReversalReason());
        item.restore(app.getAmount());
      }
      case REFUND -> {
        DisbursementTicket ticket =
            disbursement.status(CashieringSettings.MODULE, PREFIX + d.getId()).orElse(null);
        if (ticket == null || ticket.status() != DisbursementRequest.Status.RETURNED) {
          throw new BusinessRuleException(
              "REFUND_NOT_RETURNED",
              "A refund is reversed only after Disbursement returned the request");
        }
        posting.publish(
            context(item, "Refund reversed"),
            CashieringPosting.UNAPPLIED_REFUND,
            ref,
            Map.of(CashieringPosting.AMOUNT, d.getAmount().negate()));
        item.restore(d.getAmount());
      }
      case RECLASS, TRANSFER -> {
        posting.publish(
            context(item, "Reclass reversed"),
            CashieringPosting.UNAPPLIED_RECLASS,
            ref,
            Map.of(RELEASED, d.getAmount(), ASSIGNED, d.getAmount()),
            Map.of(RELEASED, nz(item.getClientCode()), ASSIGNED, nz(d.getPreviousClientCode())));
        item.reassign(d.getPreviousClientCode(), d.getPreviousUnit());
      }
      default -> item.restore(d.getAmount());
    }
  }

  private Execution apply(Unapplied item, Disposition d, String ref) {
    OpsInvoice invoice = ledger.require(d.getTargetInvoiceNo());
    invoice.loadCollections();
    Application app =
        applier
            .apply(
                invoice,
                d.getAmount(),
                new Application.Origin(
                    item.getReceiptId(), item.getId(), ApplicationSource.DISPOSITION, ref),
                new ApplyOptions(
                    LocalDate.now(clock), d.getAction() == DispositionAction.DST_APPLY, null))
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "DISPOSITION_NOTHING_TO_APPLY",
                        "Invoice "
                            + d.getTargetInvoiceNo()
                            + " has nothing outstanding to apply to"));
    item.consume(app.getAmount());
    return new Execution(app.getId(), null, app.getJournalBatchNo(), null, null);
  }

  private Execution refund(Unapplied item, Disposition d, String ref) {
    String batch =
        posting.publish(
            context(item, "Refund of unapplied payment"),
            CashieringPosting.UNAPPLIED_REFUND,
            ref,
            Map.of(CashieringPosting.AMOUNT, d.getAmount()));
    String payee = d.getPayeeName() != null ? d.getPayeeName() : item.getPayorName();
    DisbursementTicket ticket =
        disbursement.send(
            item.getCompanyId(),
            new DisbursementRequest.Spec(
                DisbursementRequest.Type.REFUND,
                CashieringSettings.MODULE,
                ref,
                nz(item.getClientCode()),
                payee,
                item.getCurrency(),
                d.getAmount(),
                "Refund of unapplied payment " + item.getReference(),
                null));
    item.consume(d.getAmount());
    return new Execution(null, ticket.requestNo(), batch, null, null);
  }

  private Execution move(Unapplied item, Disposition d, String ref) {
    String previousClient = item.getClientCode();
    String previousUnit = item.getSalesUnit();
    String newClient = d.getTargetClientCode() != null ? d.getTargetClientCode() : previousClient;
    String batch =
        posting.publish(
            context(item, d.getDispositionType() + " of unapplied payment"),
            CashieringPosting.UNAPPLIED_RECLASS,
            ref,
            Map.of(RELEASED, d.getAmount(), ASSIGNED, d.getAmount()),
            Map.of(RELEASED, nz(previousClient), ASSIGNED, nz(newClient)));
    item.reassign(d.getTargetClientCode(), d.getTargetUnit());
    return new Execution(null, null, batch, previousClient, previousUnit);
  }

  private void requireInvoice(Unapplied item, String invoiceNo) {
    requireText(invoiceNo, "the invoice to apply to");
    OpsInvoice invoice =
        ledger
            .find(invoiceNo)
            .filter(i -> i.getCompanyId().equals(item.getCompanyId()))
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "DISPOSITION_INVOICE_UNKNOWN", "Unknown invoice " + invoiceNo));
    if (!ApplicationService.receivable(invoice)) {
      throw new BusinessRuleException(
          "DISPOSITION_INVOICE_NOT_RECEIVABLE",
          "Invoice " + invoiceNo + " takes no client payment");
    }
  }

  private static void requireText(String value, String what) {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException("DISPOSITION_TARGET_REQUIRED", "Enter " + what);
    }
  }

  private PostingContext context(Unapplied item, String narration) {
    return new PostingContext(
        item.getCompanyId(),
        item.getBranchId(),
        LocalDate.now(clock),
        item.getCurrency(),
        item.getReference(),
        item.getClientCode(),
        null,
        null,
        narration + " " + item.getReference(),
        null);
  }

  private static String nz(String value) {
    return value == null ? "" : value;
  }
}
