package com.iortatechnxt.brokerverse.payrequest;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLineValues;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestContent;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestQueryService;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestWorkflowService;
import com.iortatechnxt.brokerverse.payrequest.service.RequestDrafts;
import com.iortatechnxt.brokerverse.payrequest.service.RequestFormService;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Request test data: refunds of booked invoices raised by the Marketing processor ({@code mktao}),
 * reviewed by {@code mktrev}, approved by {@code mktappr} (HR: {@code hrappr}), and the in-app
 * Disbursement queue worked by {@code disb} as the Disbursement module would.
 */
@Component
public class PayRequestFixtures {

  /** Marketing processor (raises and submits). */
  public static final String PROCESSOR = "mktao";

  /** Marketing reviewer (assigns, reviews). */
  public static final String REVIEWER = "mktrev";

  /** Marketing approver. */
  public static final String APPROVER = "mktappr";

  /** HR approver. */
  public static final String HR = "hrappr";

  private final OpsLedgerFixtures ledger;
  private final RequestFormService forms;
  private final PayRequestWorkflowService workflow;
  private final PayRequestQueryService queries;
  private final DisbursementQueueService queue;
  private final AsUser as;

  PayRequestFixtures(
      OpsLedgerFixtures ledger,
      RequestFormService forms,
      PayRequestWorkflowService workflow,
      PayRequestQueryService queries,
      DisbursementQueueService queue,
      AsUser as) {
    this.ledger = ledger;
    this.forms = forms;
    this.workflow = workflow;
    this.queries = queries;
    this.queue = queue;
    this.as = as;
  }

  /** The seed company. */
  public Long company() {
    return ledger.company();
  }

  /** A booked motor invoice in the ledger. */
  public OpsInvoice invoice() {
    return ledger.motorInvoice();
  }

  /** A unique AR number. */
  public static String arNo() {
    return "AR-T" + BookingFixtures.token();
  }

  /** A refund line of an invoice. */
  public static RefundLineValues line(OpsInvoice invoice, String reason, String amount) {
    return new RefundLineValues(
        arNo(),
        invoice.getClientCode(),
        invoice.getAssuredName(),
        invoice.getInvoiceNo(),
        new BigDecimal(amount),
        reason,
        "Makati",
        null,
        null,
        null);
  }

  /** A refund form paid by check. */
  public static RequestDrafts.Refund refund(RefundLineValues... lines) {
    return new RequestDrafts.Refund(
        new RequestContent("CBG", "2026_T Refund", "Marketing", null, null, "PHP"),
        new RequestDrafts.Payout("CHECK", null, "Check Payee " + BookingFixtures.token()),
        List.of(lines));
  }

  /** A cash-advance form credited to an account. */
  public static RequestDrafts.CashAdvance cashAdvance(String amount) {
    return new RequestDrafts.CashAdvance(
        new RequestContent("CBG", null, "Marketing", "CASH_ADVANCE", "Client visits", "PHP"),
        "EMP-" + BookingFixtures.token(),
        "Employee Test",
        new RequestDrafts.Payout("CTA", "00123456789" + (System.nanoTime() % 10), "Employee Test"),
        new BigDecimal(amount));
  }

  /** Raises a refund as the Marketing processor. */
  public PaymentRequest raise(RequestDrafts.Refund draft) {
    return as.run(PROCESSOR, () -> forms.createRefund(company(), draft));
  }

  /** Raises a refund of a new invoice and takes it to Disbursement. */
  public PaymentRequest approvedRefund() {
    PaymentRequest r = raise(refund(line(invoice(), "OVERPAYMENT", "150.00")));
    return approve(r);
  }

  /** Submits, endorses and approves a request (Marketing only). */
  public PaymentRequest approve(PaymentRequest r) {
    as.run(PROCESSOR, () -> workflow.submit(r.getId(), "Please refund"));
    as.run(REVIEWER, () -> workflow.endorse(r.getId(), "Checked"));
    return as.run(APPROVER, () -> workflow.approve(r.getId(), "Approved"));
  }

  /** The request as it is now. */
  public PaymentRequest reload(PaymentRequest r) {
    return queries.get(r.getId());
  }

  /** The gateway request of a request. */
  public DisbursementRequest gatewayRequest(PaymentRequest r) {
    PaymentRequest now = reload(r);
    return queue.find("PAYREQUEST", now.currentSendRef()).orElseThrow();
  }

  /** Disbursement assigns a DV and pays the request. */
  public void pay(PaymentRequest r, String dvNo) {
    Long id = gatewayRequest(r).getId();
    as.run(
        "disb",
        () -> {
          queue.acknowledge(id);
          queue.assignDv(id, dvNo);
          queue.track(id, "APPROVED", "RELEASED");
          return queue.markPaid(id);
        });
  }

  /** Disbursement returns the request. */
  public void returnToSource(PaymentRequest r, String reason) {
    Long id = gatewayRequest(r).getId();
    as.run("disb", () -> queue.returnToSource(id, reason));
  }
}
