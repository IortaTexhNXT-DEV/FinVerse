package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.OpsInvoiceBooked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to committed Operations ledger events, each in a new transaction:
 *
 * <ul>
 *   <li>an invoice was booked: the pre-booked payments of its account are applied at once
 *       (CSHID.020);
 *   <li>Disbursement paid the BIR 2307 request of a batch: the certificates are released to the
 *       insurer and PR2307 is settled against DTIP (DBMID.001).
 * </ul>
 */
@Component
public class CashieringEventListener {

  private final PrebookedService prebooked;
  private final CwtService cwt;
  private final ItemTransactions transactions;

  /**
   * Creates the listener.
   *
   * @param prebooked pre-booked queue
   * @param cwt BIR 2307
   * @param transactions new transactions
   */
  public CashieringEventListener(
      PrebookedService prebooked, CwtService cwt, ItemTransactions transactions) {
    this.prebooked = prebooked;
    this.cwt = cwt;
    this.transactions = transactions;
  }

  /**
   * Applies the pre-booked payments of a newly booked account.
   *
   * @param event invoice booked
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onBooked(OpsInvoiceBooked event) {
    if (event.arn() != null) {
      transactions.run("PRE:" + event.arn(), () -> prebooked.rematchAccount(event.arn()) > 0);
    }
  }

  /**
   * Releases a BIR 2307 batch once Disbursement paid it.
   *
   * @param event Disbursement status change
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDisbursement(DisbursementStatusChanged event) {
    if (CashieringSettings.MODULE.equals(event.sourceModule())
        && event.type() == DisbursementRequest.Type.CWT2307
        && event.status() == DisbursementRequest.Status.PAID) {
      transactions.run(
          "CWB:" + event.sourceRef(), () -> cwt.release(event.sourceRef(), true) != null);
    }
  }
}
