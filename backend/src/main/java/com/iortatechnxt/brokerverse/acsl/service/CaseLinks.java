package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.AcslCase;
import com.iortatechnxt.brokerverse.acsl.domain.AcslCaseRepository;
import com.iortatechnxt.brokerverse.acsl.domain.CaseStage;
import com.iortatechnxt.brokerverse.acsl.domain.CaseSubject;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.PaymentReversalCompleted;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReversalRequester;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReversalRequester.ReversalRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReversalRequester.ReversalTicket;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What a case asks of other teams (ACSL 2.6.0-2.6.2): the sub-ledger payment reversal requested
 * from Cashiering through the Operations port {@link PaymentReversalRequester} (the default adapter
 * hands it over to the team CASH_APPLY until cashiering implements it) with its outcome ({@code
 * PaymentReversalCompleted}); and the message to the Account Officer of the invoice about a short
 * or over payment.
 */
@Service
@Transactional
public class CaseLinks {

  private final CaseService cases;
  private final AcslCaseRepository repository;
  private final PaymentReversalRequester reversals;
  private final InvoiceLedgerQueryService ledger;
  private final AcslNotifier notifier;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param repository cases by requester reference
   * @param reversals Cashiering payment reversal port
   * @param ledger Operations invoice ledger (the AO)
   * @param notifier notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CaseLinks(
      CaseService cases,
      AcslCaseRepository repository,
      PaymentReversalRequester reversals,
      InvoiceLedgerQueryService ledger,
      AcslNotifier notifier,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.cases = cases;
    this.repository = repository;
    this.reversals = reversals;
    this.ledger = ledger;
    this.notifier = notifier;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Requests the sub-ledger payment reversal of the case's invoice (ACSL 2.6.1): routed for
   * approval in Cashiering, idempotent on the case number.
   *
   * @param id case
   * @param receiptNo AR / OR of the payment
   * @param amount amount, null for the whole application
   * @param reason reason
   * @return the case
   */
  public AcslCase requestReversal(Long id, String receiptNo, BigDecimal amount, String reason) {
    AcslCase c = cases.get(id);
    requireOpen(c);
    CaseSubject account = c.accountOrNone();
    if (account.invoiceNo() == null || Acsl.blankToNull(receiptNo) == null) {
      throw new BusinessRuleException(
          "ACSL_REVERSAL_INCOMPLETE",
          "A payment reversal needs the case's invoice and the receipt");
    }
    ReversalTicket ticket =
        reversals.request(
            new ReversalRequest(
                c.getCompanyId(),
                account.invoiceNo(),
                receiptNo.strip(),
                account.currency(),
                amount,
                LocalDate.now(clock),
                Acsl.blankToNull(reason),
                new PaymentReversalRequester.Source(
                    Acsl.MODULE, c.getCaseNo(), currentUser.username())));
    c.reversal(ticket.reference(), ticket.status().name(), ticket.message());
    audit.record(
        Acsl.CASE_ENTITY,
        c.getCaseNo(),
        AuditAction.SUBMIT,
        "Payment reversal requested: " + ticket.status());
    return c;
  }

  /**
   * The decision of Cashiering on a reversal (ACSL 2.6.1).
   *
   * @param event decision
   */
  @EventListener
  public void on(PaymentReversalCompleted event) {
    if (Acsl.MODULE.equals(event.sourceModule())) {
      repository
          .findByCaseNo(event.sourceRef())
          .ifPresent(
              c -> {
                c.reversal(
                    event.reference(), event.approved() ? "APPROVED" : "REJECTED", event.remarks());
                notifier.user(
                    c.getResultBy() == null ? c.getRequestedBy() : c.getResultBy(),
                    c.getCaseNo()
                        + " payment reversal "
                        + (event.approved() ? "approved" : "rejected"),
                    c.getSubject(),
                    Acsl.caseLink(c.getId()),
                    Acsl.CASE_ENTITY,
                    c.getId());
              });
    }
  }

  /**
   * Sends a message about a short or over payment to the Account Officer of the invoice (ACSL
   * 2.6.2).
   *
   * @param id case
   * @param message message
   * @return the Account Officer notified
   */
  public String messageAccountOfficer(Long id, String message) {
    AcslCase c = cases.get(id);
    String invoiceNo = c.accountOrNone().invoiceNo();
    String ao =
        invoiceNo == null
            ? null
            : ledger.find(invoiceNo).map(i -> i.getClassification().aoUsername()).orElse(null);
    if (ao == null || Acsl.blankToNull(message) == null) {
      throw new BusinessRuleException(
          "ACSL_NO_ACCOUNT_OFFICER",
          "The case's invoice has no Account Officer, or the message is empty");
    }
    notifier.user(
        ao,
        c.getCaseNo() + ": message from ACSL",
        message.strip(),
        Acsl.caseLink(c.getId()),
        Acsl.CASE_ENTITY,
        c.getId());
    audit.record(Acsl.CASE_ENTITY, c.getCaseNo(), AuditAction.UPDATE, "Message to AO " + ao);
    return ao;
  }

  private static void requireOpen(AcslCase c) {
    if (c.getStage() != CaseStage.ASSIGNED && c.getStage() != CaseStage.INVESTIGATING) {
      throw new BusinessRuleException(
          Acsl.WRONG_STAGE, c.getCaseNo() + " is " + c.getStage() + ": no request can be made");
    }
  }
}
