package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.IssuedReceipt;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.ReceiptLine;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.ReceiptRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedSink;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedSink.UnappliedRequest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Collection of direct payment commission and premium receivable reversal (CMRID.010, MKTID.012,
 * OPERATIONS_DESIGN 5 rows 22-24): the commission OR of a billing's approved accounts is asked from
 * cashiering through {@link ReceiptIssuer} (handed over as DEFERRED while cashiering is not
 * installed); each account's collection is posted, then its premium receivable is reversed -
 * guarded by the account's direct payment tag in the ledger. A reversed account can be reinstated
 * with a direct payment reinstatement reason (CSHID.004 b); a cancellation reason sends the
 * collected commission to the unapplied workbench through {@link UnappliedSink}.
 */
@Service
@Transactional
public class DpCollectionService {

  private static final String ITEM = "DpItem";

  private final DpBillingService billings;
  private final DpItemRepository items;
  private final DpPostings postings;
  private final ReceiptIssuer receipts;
  private final UnappliedSink unapplied;
  private final InvoiceLedgerQueryService ledger;
  private final LovService lovs;
  private final SystemParameterService parameters;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param billings billings
   * @param items DP accounts
   * @param postings postings
   * @param receipts OR port (cashiering)
   * @param unapplied unapplied port (cashiering)
   * @param ledger Operations ledger
   * @param lovs lists of values
   * @param parameters business parameters
   * @param workflow workflow engine
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public DpCollectionService(
      DpBillingService billings,
      DpItemRepository items,
      DpPostings postings,
      ReceiptIssuer receipts,
      UnappliedSink unapplied,
      InvoiceLedgerQueryService ledger,
      LovService lovs,
      SystemParameterService parameters,
      WorkflowService workflow,
      AuditTrailService audit,
      Clock clock) {
    this.billings = billings;
    this.items = items;
    this.postings = postings;
    this.receipts = receipts;
    this.unapplied = unapplied;
    this.ledger = ledger;
    this.lovs = lovs;
    this.parameters = parameters;
    this.workflow = workflow;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Collects the commission of a billing's approved accounts and reverses their premium receivable.
   *
   * @param billingId billing (approved)
   * @param request receipt date, bank account and certificate
   * @return the billing
   */
  public DpBilling collect(Long billingId, CollectRequest request) {
    DpBilling billing = billings.require(billingId);
    if (!DpBilling.APPROVED.equals(billing.getStage())) {
      throw new BusinessRuleException(
          "DP_BILLING_NOT_APPROVED",
          "Billing " + billing.getBillingNo() + " has no approved account to collect");
    }
    String bank =
        request.bankAccount() == null || request.bankAccount().isBlank()
            ? parameters.text("CMR_DP_COLLECTION_BANK", "").strip()
            : request.bankAccount().strip();
    if (bank.isEmpty()) {
      throw new BusinessRuleException(
          "DP_BANK_REQUIRED", "Choose the bank account the commission was received in");
    }
    List<DpItem> approved =
        items.findByBillingIdOrderByIdAsc(billingId).stream()
            .filter(i -> i.getTag() == DpTag.APPROVED)
            .toList();
    approved.forEach(this::requireDirectPayment);
    IssuedReceipt or = receipt(billing, approved, request);
    billing.receipt(or.receiptNo(), or.status().name());
    LocalDate date = request.receiptDate() == null ? LocalDate.now(clock) : request.receiptDate();
    for (DpItem item : approved) {
      postings.collect(
          item, new DpPostings.Collection(date, bank, or.receiptNo(), billing.getBillingNo()));
      item.collected(or.receiptNo(), item.getNetCommission(), date);
      reverse(item, date);
    }
    String id = String.valueOf(billingId);
    workflow.systemTransition(
        DpBillingService.ENTITY, id, "collected", TransitionNote.comment(or.message()));
    workflow.systemTransition(
        DpBillingService.ENTITY,
        id,
        "pr_reversed",
        TransitionNote.comment("Premium receivable of the direct payment accounts reversed"));
    audit.record(
        DpBillingService.ENTITY,
        billing.getBillingNo(),
        AuditAction.UPDATE,
        "Collected " + approved.size() + " account(s), OR " + or.status() + " " + or.message());
    return billing;
  }

  private IssuedReceipt receipt(DpBilling billing, List<DpItem> approved, CollectRequest request) {
    List<ReceiptLine> lines =
        approved.stream()
            .map(
                i ->
                    new ReceiptLine(
                        i.getInvoiceNo(),
                        i.getInsurerCode(),
                        i.getCommission(),
                        i.getCommissionVat(),
                        i.getWtax(),
                        "Commission on direct payment " + i.getInvoiceNo()))
            .toList();
    return receipts.issueOfficialReceipt(
        new ReceiptRequest(
            billing.getCompanyId(),
            "COMMISSION",
            new ReceiptIssuer.Payee(billing.getInsurerCode(), billing.getInsurerCode()),
            currency(approved),
            request.receiptDate() == null ? LocalDate.now(clock) : request.receiptDate(),
            lines,
            new ReceiptIssuer.Source(
                DpIntakeService.MODULE,
                "DPC:" + billing.getBillingNo(),
                request.certificateRef(),
                "Direct payment commission billing " + billing.getBillingNo())));
  }

  private String currency(List<DpItem> approved) {
    return approved.stream()
        .findFirst()
        .flatMap(i -> ledger.find(i.getInvoiceNo()))
        .map(OpsInvoice::getCurrency)
        .orElse("PHP");
  }

  private void requireDirectPayment(DpItem item) {
    boolean tagged = ledger.find(item.getInvoiceNo()).map(OpsInvoice::isDpFlag).orElse(false);
    if (!tagged) {
      throw new BusinessRuleException(
          "DP_TAG_INVALID",
          "Invoice "
              + item.getInvoiceNo()
              + " is not tagged direct payment by Marketing: its premium receivable cannot be"
              + " reversed (MKTID.012)");
    }
  }

  private void reverse(DpItem item, LocalDate date) {
    int seq = item.reversed(clock.instant());
    String batch = postings.reversePremium(item, seq, date);
    audit.record(
        ITEM,
        item.getId(),
        AuditAction.UPDATE,
        "Premium receivable reversed" + (batch == null ? "" : " (journal " + batch + ")"));
  }

  /**
   * Reverses again the premium receivable of a reinstated account.
   *
   * @param itemId account (collected)
   * @return the account
   */
  public DpItem reverse(Long itemId) {
    DpItem item = require(itemId);
    requireDirectPayment(item);
    reverse(item, LocalDate.now(clock));
    return item;
  }

  /**
   * Reinstates the premium receivable of a reversed account (CSHID.004 b).
   *
   * @param itemId account (PR reversed)
   * @param reasonCode reason (LOV REINSTATEMENT_REASON, direct payment group)
   * @param comment comment
   * @return the account
   */
  public DpItem reinstate(Long itemId, String reasonCode, String comment) {
    DpItem item = require(itemId);
    if (reasonCode == null || !reasonCode.startsWith("DP_")) {
      throw new BusinessRuleException(
          "DP_REINSTATE_REASON", "Choose a direct payment reinstatement reason");
    }
    lovs.requireValid("REINSTATEMENT_REASON", reasonCode, LocalDate.now(clock));
    int reversal = item.getReversalCount();
    int seq = item.reinstated();
    String batch = postings.reinstatePremium(item, reversal, seq, LocalDate.now(clock));
    if ("DP_CANCELLATION".equals(reasonCode) && item.getCollectedAmount() != null) {
      var handle =
          unapplied.create(
              new UnappliedRequest(
                  item.getCompanyId(),
                  "DP_REINSTATE",
                  new UnappliedSink.Party(item.getClientCode(), item.getBranchCode()),
                  currency(List.of(item)),
                  item.getCollectedAmount(),
                  item.getInvoiceNo(),
                  null,
                  new UnappliedSink.Source(
                      DpIntakeService.MODULE, "DPR:" + item.getId() + ":" + seq, comment)));
      item.returned(handle.reference());
    }
    audit.record(
        ITEM,
        itemId,
        AuditAction.UPDATE,
        "Reinstated ("
            + reasonCode
            + ")"
            + (comment == null ? "" : ": " + comment)
            + (batch == null ? "" : ", journal " + batch));
    return item;
  }

  private DpItem require(Long id) {
    return items.findById(id).orElseThrow(() -> new ResourceNotFoundException(ITEM, id));
  }

  /**
   * A collection.
   *
   * @param receiptDate OR / collection date, today when null
   * @param bankAccount GL bank account, the CMR_DP_COLLECTION_BANK parameter when blank
   * @param certificateRef BIR certificate of the withholding tax, may be null
   */
  public record CollectRequest(LocalDate receiptDate, String bankAccount, String certificateRef) {}
}
