package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The ledger side of a batch line (RMTID.019/040): takes an invoice into a batch (remittance status
 * REVIEW_IN_PROCESS and the remittance lock), gives it back (previous status, lock released) on
 * exclusion or return, and checks before submission that every line is still in remittance.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class BatchLedger {

  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService writer;

  /**
   * Creates the helper.
   *
   * @param ledger ledger reads
   * @param writer ledger statuses and locks
   */
  public BatchLedger(InvoiceLedgerQueryService ledger, InvoiceLedgerService writer) {
    this.ledger = ledger;
    this.writer = writer;
  }

  /**
   * Takes an invoice into a batch.
   *
   * @param invoiceNo invoice
   * @param batchNo batch
   */
  public void take(String invoiceNo, String batchNo) {
    writer.setRemittanceStatus(
        invoiceNo, RemittanceStatus.REVIEW_IN_PROCESS, RemittanceSettings.MODULE, "In " + batchNo);
    writer.lock(invoiceNo, RemittanceSettings.MODULE, "In remittance batch " + batchNo);
  }

  /**
   * Gives an invoice back: its status before the extraction (derived statuses are derived again)
   * and the lock released.
   *
   * @param line line
   * @param why reason recorded in the invoice history
   */
  public void release(BatchLine line, String why) {
    RemittanceStatus previous = line.getPreviousStatus();
    writer.setRemittanceStatus(
        line.getInvoiceNo(),
        previous.isDerived() ? RemittanceStatus.UNPROCESSED : previous,
        RemittanceSettings.MODULE,
        why);
    writer.unlock(line.getInvoiceNo(), RemittanceSettings.MODULE, why);
  }

  /**
   * Refuses to take back an excluded invoice that is no longer free: in another batch, locked by
   * another team, held, written off or with a negative adjustment pending (RMTID.002/020).
   *
   * @param invoiceNo invoice
   */
  public void requireFree(String invoiceNo) {
    OpsInvoice i = ledger.require(invoiceNo);
    boolean free =
        ExtractionWork.isExaminable(i)
            && i.getLockOwner() == null
            && !i.isHoldFlag()
            && !i.isPendingNegAdj()
            && !i.isWrittenOff();
    if (!free) {
      throw new BusinessRuleException(
          "REMIT_RESTORE_NOT_ELIGIBLE",
          "Invoice "
              + invoiceNo
              + " can no longer be restored (remittance status "
              + i.getRemittanceStatus()
              + (i.getLockOwner() == null ? "" : ", locked by " + i.getLockOwner())
              + ")");
    }
  }

  /**
   * Checks the lines of a batch before submission or approval (RMTID.019/020): each invoice must
   * still be in review, locked by remittance, and neither held, written off nor waiting for a
   * negative adjustment.
   *
   * @param lines lines to remit
   * @return problems, empty when the batch may go on
   */
  public List<String> problems(List<BatchLine> lines) {
    List<String> problems = new ArrayList<>();
    for (BatchLine line : lines) {
      OpsInvoice i = ledger.require(line.getInvoiceNo());
      String issue = issue(i);
      if (issue != null) {
        problems.add(line.getInvoiceNo() + ": " + issue);
      }
    }
    return problems;
  }

  private static String issue(OpsInvoice i) {
    if (i.getRemittanceStatus() != RemittanceStatus.REVIEW_IN_PROCESS) {
      return "remittance status is " + i.getRemittanceStatus();
    }
    if (!RemittanceSettings.MODULE.equals(i.getLockOwner())) {
      return "not locked for remittance";
    }
    return flagIssue(i);
  }

  private static String flagIssue(OpsInvoice i) {
    if (i.isCancelled()) {
      return "cancelled";
    }
    if (i.isHoldFlag() || i.isWrittenOff()) {
      return i.isHoldFlag() ? "on hold" : "written off";
    }
    return i.isPendingNegAdj() ? "negative adjustment pending" : null;
  }
}
