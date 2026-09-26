package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceFlagChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceMovementPosted;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.OpsInvoiceBooked;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.RemittanceStatusChanged;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens to the Operations ledger events for Claims (BRCLM.001/039, OQ46; CLAIMS_BROKING_DESIGN
 * 3.1): after a movement, a flag change, a remittance status change or an endorsement booking is
 * committed, the open claims of the cover are brought in step by {@link ClaimOperationsSync}. A
 * failure is logged only: the daily job {@code BCL_PREMIUM_RECHECK} repairs the premium status, and
 * the ledger posting that raised the event is never affected.
 */
@Component
public class OperationsListener {

  private static final Logger LOG = LoggerFactory.getLogger(OperationsListener.class);

  private final ClaimOperationsSync sync;

  /**
   * Creates the listener.
   *
   * @param sync claim synchroniser
   */
  public OperationsListener(ClaimOperationsSync sync) {
    this.sync = sync;
  }

  /**
   * A payment, reversal or adjustment was posted to an invoice.
   *
   * @param event event
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onMovement(InvoiceMovementPosted event) {
    safely(event.invoiceNo(), () -> sync.invoiceMoved(event.invoiceNo()));
  }

  /**
   * A flag of an invoice changed (e.g. cancelled).
   *
   * @param event event
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onFlag(InvoiceFlagChanged event) {
    safely(event.invoiceNo(), () -> sync.invoiceMoved(event.invoiceNo()));
  }

  /**
   * The remittance status of an invoice changed.
   *
   * @param event event
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRemittance(RemittanceStatusChanged event) {
    if (event.to() == RemittanceStatus.FULLY_REMITTED) {
      safely(event.invoiceNo(), () -> sync.invoiceRemitted(event.invoiceNo()));
    }
  }

  /**
   * An invoice was booked into the ledger; endorsements concern the claims of the cover.
   *
   * @param event event
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onBooked(OpsInvoiceBooked event) {
    if (event.kind() != InvoiceKind.BOOKING) {
      safely(event.invoiceNo(), () -> sync.endorsementBooked(event.invoiceNo()));
    }
  }

  private static void safely(String invoiceNo, Supplier<Integer> work) {
    try {
      work.get();
    } catch (DataAccessException | IllegalStateException ex) {
      LOG.warn("Claims of invoice {} not brought in step: {}", invoiceNo, ex.getMessage());
    }
  }
}
