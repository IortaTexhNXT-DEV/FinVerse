package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ApplicationSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentReversal;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentReversalRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationService.ApplyOptions;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.PaymentReversalCompleted;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReversalRequester;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering's implementation of the port {@link PaymentReversalRequester} (ACSL 2.6.0-2.6.1;
 * ACCOUNTING_DISBURSEMENT_DESIGN 6 row 19): the request is recorded for a cashiering approver
 * ({@code CASH_APPROVE}), idempotent on the requester's reference. On approval (four eyes) the
 * receipt's active applications on the invoice are reversed newest first with the application
 * engine (negative {@code OPS_PAYMENT_APPLY}: Dr PR by component / Cr unapplied collections, and an
 * {@code UNAPPLIED} ledger movement); what was reversed beyond the requested amount is applied
 * again, and the money reversed goes back to the unapplied list for disposition. The decision goes
 * back as {@code PaymentReversalCompleted}.
 */
@Service
@Transactional
public class CashieringPaymentReversals implements PaymentReversalRequester {

  /** Entity of the audit trail. */
  public static final String ENTITY = "CashPaymentReversal";

  private final PaymentReversalRepository reversals;
  private final CashReceiptRepository receipts;
  private final ApplicationRepository applications;
  private final ApplicationService applier;
  private final UnappliedService unapplied;
  private final InvoiceLedgerQueryService ledger;
  private final PaymentReversalSupport support;
  private final ApplicationEventPublisher events;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the adapter.
   *
   * @param reversals reversal requests
   * @param receipts receipts
   * @param applications applications
   * @param applier application engine
   * @param unapplied unapplied workbench
   * @param ledger invoice ledger
   * @param support numbers, notifications and audit
   * @param events event publisher
   * @param currentUser current user
   * @param clock clock
   */
  public CashieringPaymentReversals(
      PaymentReversalRepository reversals,
      CashReceiptRepository receipts,
      ApplicationRepository applications,
      ApplicationService applier,
      UnappliedService unapplied,
      InvoiceLedgerQueryService ledger,
      PaymentReversalSupport support,
      ApplicationEventPublisher events,
      CurrentUser currentUser,
      Clock clock) {
    this.reversals = reversals;
    this.receipts = receipts;
    this.applications = applications;
    this.applier = applier;
    this.unapplied = unapplied;
    this.ledger = ledger;
    this.support = support;
    this.events = events;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Override
  public ReversalTicket request(ReversalRequest request) {
    PaymentReversal r =
        reversals
            .findBySourceModuleAndSourceRef(request.source().module(), request.source().reference())
            .orElseGet(() -> create(request));
    return new ReversalTicket(
        Status.SUBMITTED,
        r.getRequestNo(),
        "Payment reversal " + r.getRequestNo() + " is " + r.getStatus() + " in Cashiering");
  }

  private PaymentReversal create(ReversalRequest request) {
    PaymentReversal r =
        reversals.save(
            new PaymentReversal(
                support.nextNumber(),
                new PaymentReversal.Spec(
                    request.companyId(),
                    request.source().module(),
                    request.source().reference(),
                    request.invoiceNo(),
                    request.receiptNo(),
                    request.currency(),
                    request.amount(),
                    request.valueDate() == null ? LocalDate.now(clock) : request.valueDate(),
                    request.reason(),
                    request.source().requestedBy())));
    support.audit(
        r, AuditAction.CREATE, "Requested by " + r.getSourceModule() + " " + r.getSourceRef());
    support.notifyApprovers(
        new Notice(
            r.getRequestNo() + ": payment reversal for approval",
            "Receipt " + r.getReceiptNo() + " on invoice " + r.getInvoiceNo(),
            "/cashiering/requests?tab=REVERSALS",
            ENTITY,
            String.valueOf(r.getId())));
    return r;
  }

  /**
   * Reversal requests in some statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return requests, newest first
   */
  @Transactional(readOnly = true)
  public Page<PaymentReversal> list(
      Long companyId, Collection<PaymentReversal.Status> statuses, Pageable pageable) {
    return reversals.findByCompanyIdAndStatusInOrderByIdDesc(companyId, statuses, pageable);
  }

  /**
   * Requests waiting for approval.
   *
   * @param companyId company
   * @return count
   */
  @Transactional(readOnly = true)
  public long submittedCount(Long companyId) {
    return reversals.countByCompanyIdAndStatus(companyId, PaymentReversal.Status.SUBMITTED);
  }

  /**
   * Approves a request: reverses the receipt's applications on the invoice and puts the money back
   * in the unapplied list.
   *
   * @param id request
   * @return the request
   */
  public PaymentReversal approve(Long id) {
    PaymentReversal r = submitted(id);
    if (CurrentUser.sameUser(currentUser.username(), r.getRequestedBy())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "The requester cannot approve the payment reversal");
    }
    Receipt receipt =
        receipts
            .findByReceiptNo(r.getReceiptNo())
            .filter(x -> x.getCompanyId().equals(r.getCompanyId()))
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "PAYMENT_REVERSAL_RECEIPT_UNKNOWN", "Unknown receipt " + r.getReceiptNo()));
    OpsInvoice invoice = ledger.require(r.getInvoiceNo());
    invoice.loadCollections();
    BigDecimal net = reverse(r, receipt, invoice);
    Unapplied item =
        unapplied.create(
            r.getCompanyId(),
            receipt.getBranchId(),
            new UnappliedSpec(
                UnappliedOrigin.OTHER,
                receipt.getId(),
                null,
                r.getInvoiceNo(),
                invoice.getClientCode(),
                receipt.getPayorName(),
                receipt.getSalesUnit(),
                invoice.getCurrency(),
                net,
                null,
                CashieringSettings.MODULE,
                "PRV:" + r.getRequestNo(),
                "Payment reversal " + r.getRequestNo()));
    r.approve(net, item.getId(), currentUser.username(), clock.instant());
    String remarks =
        "Reversed "
            + net.toPlainString()
            + " of "
            + r.getReceiptNo()
            + "; back in unapplied item "
            + item.getReference();
    support.audit(r, AuditAction.REVERSE, remarks);
    publish(r, true, remarks);
    return r;
  }

  private BigDecimal reverse(PaymentReversal r, Receipt receipt, OpsInvoice invoice) {
    List<Application> active = activeNewestFirst(r, receipt);
    BigDecimal available =
        active.stream().map(Application::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal target = r.getAmount() == null ? available : r.getAmount();
    if (available.signum() <= 0 || target.compareTo(available) > 0) {
      throw new BusinessRuleException(
          "PAYMENT_REVERSAL_NOTHING_APPLIED",
          "Receipt "
              + r.getReceiptNo()
              + " has "
              + available
              + " applied to "
              + r.getInvoiceNo()
              + ", less than "
              + target);
    }
    BigDecimal reversed = BigDecimal.ZERO;
    String reason = r.getReason() == null ? "Payment reversal " + r.getRequestNo() : r.getReason();
    for (Application app : active) {
      if (reversed.compareTo(target) >= 0) {
        break;
      }
      applier.reverse(app, invoice, app.reference() + ":PRV:" + r.getRequestNo(), reason);
      reversed = reversed.add(app.getAmount());
    }
    return reversed.subtract(applyBack(r, receipt, invoice, reversed.subtract(target)));
  }

  private List<Application> activeNewestFirst(PaymentReversal r, Receipt receipt) {
    List<Application> active = new ArrayList<>();
    for (Application app : applications.findByReceiptIdOrderByIdAsc(receipt.getId())) {
      if (app.isActive() && app.getInvoiceNo().equals(r.getInvoiceNo())) {
        active.add(0, app);
      }
    }
    return active;
  }

  private BigDecimal applyBack(
      PaymentReversal r, Receipt receipt, OpsInvoice invoice, BigDecimal over) {
    if (over.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    return applier
        .apply(
            invoice,
            over,
            new Application.Origin(
                receipt.getId(), null, ApplicationSource.REAPPLY, "PRV:" + r.getRequestNo()),
            new ApplyOptions(r.getValueDate(), false, receipt.getReceiptNo()))
        .map(Application::getAmount)
        .orElse(BigDecimal.ZERO);
  }

  /**
   * Rejects a request.
   *
   * @param id request
   * @param reason reason
   * @return the request
   */
  public PaymentReversal reject(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("REJECT_REASON_REQUIRED", "Enter the reason for rejecting");
    }
    PaymentReversal r = submitted(id);
    r.reject(reason.strip(), currentUser.username(), clock.instant());
    support.audit(r, AuditAction.REJECT, reason.strip());
    publish(r, false, reason.strip());
    return r;
  }

  private void publish(PaymentReversal r, boolean approved, String remarks) {
    events.publishEvent(
        new PaymentReversalCompleted(
            r.getCompanyId(),
            r.getSourceModule(),
            r.getSourceRef(),
            approved,
            r.getRequestNo(),
            remarks));
  }

  private PaymentReversal submitted(Long id) {
    PaymentReversal r =
        reversals.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    if (r.getStatus() != PaymentReversal.Status.SUBMITTED) {
      throw new BusinessRuleException(
          "PAYMENT_REVERSAL_DECIDED", r.getRequestNo() + " is already " + r.getStatus());
    }
    return r;
  }
}
