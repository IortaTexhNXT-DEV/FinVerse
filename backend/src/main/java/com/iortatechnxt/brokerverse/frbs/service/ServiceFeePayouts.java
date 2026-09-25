package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine.Ticket;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway.DisbursementTicket;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * What an approved service-fee line does outside frbs (FRBS 2.10.0; design section 6 row 20): the
 * accrual {@code FRBS_SERVICE_FEE_ACCRUE} (Dr service fee expense with the line's cost centre, or
 * the cost-centre rules of the engine / Cr service fee payable) and the payout request to
 * Disbursement through the Operations {@code DisbursementGateway} (type SERVICE_FEE, the run number
 * as RFP number), which Disbursement pays by DV (Dr service fee payable / Cr bank).
 */
@Component
public class ServiceFeePayouts {

  private static final String PAYEE_CLASS = "OTHER";
  private static final int MAX_DESCRIPTION = 500;

  private final AccountingEventPublisher publisher;
  private final BookRates bookRates;
  private final DisbursementGateway gateway;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the component.
   *
   * @param publisher accounting engine
   * @param bookRates BOOK rates of foreign-currency lines
   * @param gateway Disbursement port
   * @param audit audit trail
   * @param clock clock
   */
  public ServiceFeePayouts(
      AccountingEventPublisher publisher,
      BookRates bookRates,
      DisbursementGateway gateway,
      AuditTrailService audit,
      Clock clock) {
    this.publisher = publisher;
    this.bookRates = bookRates;
    this.gateway = gateway;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Accrues a line (idempotent on the run and line).
   *
   * @param run run
   * @param line line
   */
  public void accrue(ServiceFeeRun run, ServiceFeeLine line) {
    if (line.getFee().signum() == 0) {
      return;
    }
    BusinessEvent event =
        new BusinessEvent(
            ServiceFees.ACCRUAL_EVENT,
            run.getCompanyId(),
            line.getBranchId(),
            LocalDate.now(clock),
            line.getCurrency(),
            ServiceFees.MODULE,
            run.getRunNo() + ":" + line.getLineNo(),
            run.getRunNo(),
            line.getPayeeCode(),
            null,
            line.getCostCenter(),
            "Service fee " + line.getSegment() + " " + line.getSalesUnit() + " " + run.getRunNo(),
            Map.of(ServiceFees.AMOUNT, line.getFee()),
            Map.of());
    JournalBatch journal = publisher.publish(bookRates.price(event));
    line.accrued(journal.getBatchNo());
  }

  /**
   * Sends a line to Disbursement (again after a return).
   *
   * @param run run
   * @param line line
   */
  public void send(ServiceFeeRun run, ServiceFeeLine line) {
    if (line.getFee().signum() == 0) {
      return;
    }
    line.nextSending();
    DisbursementRequest.Spec spec =
        new DisbursementRequest.Spec(
                DisbursementRequest.Type.SERVICE_FEE,
                ServiceFees.MODULE,
                line.sourceRef(run.getRunNo()),
                line.getPayeeCode(),
                line.getPayeeName(),
                line.getCurrency(),
                line.getFee(),
                description(run, line),
                null)
            .routed(
                run.getRunNo(),
                PAYEE_CLASS,
                DisbursementRequest.Type.SERVICE_FEE.name(),
                null,
                false)
            .withReferences(
                List.of(),
                line.getAccrualBatchNo() == null ? List.of() : List.of(line.getAccrualBatchNo()));
    DisbursementTicket ticket = gateway.send(run.getCompanyId(), spec);
    line.sent(
        new Ticket(ticket.requestNo(), ticket.status().name(), ticket.dvNo(), ticket.message()));
    audit.record(
        ServiceFees.ENTITY,
        run.getRunNo(),
        AuditAction.SUBMIT,
        "Line " + line.getLineNo() + " sent to Disbursement as " + ticket.requestNo());
  }

  private static String description(ServiceFeeRun run, ServiceFeeLine line) {
    String text =
        "Service fee "
            + run.getRunNo()
            + " line "
            + line.getLineNo()
            + ": "
            + line.getSegment()
            + " "
            + line.getSalesUnit()
            + ", "
            + line.getInvoiceCount()
            + " invoice(s) paid "
            + run.getPeriodFrom()
            + " to "
            + run.getPeriodTo();
    return text.length() > MAX_DESCRIPTION ? text.substring(0, MAX_DESCRIPTION) : text;
  }
}
