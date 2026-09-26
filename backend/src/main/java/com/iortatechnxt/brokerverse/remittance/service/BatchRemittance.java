package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes the payment side of an approved batch (RMTID.019): the invoices become PARTIALLY_REMITTED
 * or FULLY_REMITTED (DTIP balance left or not), their remittance lock is released and the batch
 * moves on ({@code dv_full} / {@code dv_partial}). Run when the DV is assigned, or at approval when
 * the deductions left nothing to pay (ACSL 2.9.2).
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class BatchRemittance {

  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService writer;
  private final WorkflowService workflow;

  /**
   * Creates the component.
   *
   * @param ledger ledger reads
   * @param writer ledger statuses and locks
   * @param workflow batch workflow
   */
  public BatchRemittance(
      InvoiceLedgerQueryService ledger, InvoiceLedgerService writer, WorkflowService workflow) {
    this.ledger = ledger;
    this.writer = writer;
    this.workflow = workflow;
  }

  /**
   * Marks the batch's invoices remitted and moves the batch on.
   *
   * @param batch approved batch
   * @param why what settled it (DV number or deductions), for the histories
   */
  public void remitted(RemittanceBatch batch, String why) {
    boolean full = true;
    for (BatchLine line : batch.included()) {
      OpsInvoice invoice = ledger.require(line.getInvoiceNo());
      boolean cleared = invoice.component(LedgerComponent.DTIP).getBalance().signum() <= 0;
      RemittanceStatus status =
          cleared ? RemittanceStatus.FULLY_REMITTED : RemittanceStatus.PARTIALLY_REMITTED;
      full &= cleared;
      String reason = why + " of " + batch.getBatchNo();
      writer.setRemittanceStatus(line.getInvoiceNo(), status, RemittanceSettings.MODULE, reason);
      writer.unlock(line.getInvoiceNo(), RemittanceSettings.MODULE, reason);
      line.remitted(status);
    }
    workflow.systemTransition(
        BatchService.ENTITY,
        batch.getId().toString(),
        full ? "dv_full" : "dv_partial",
        TransitionNote.comment(why));
  }
}
