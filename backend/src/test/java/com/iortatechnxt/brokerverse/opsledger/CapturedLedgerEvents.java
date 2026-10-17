package com.iortatechnxt.brokerverse.opsledger;

import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceFlagChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceLocked;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceMovementPosted;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.OpsInvoiceBooked;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Records the committed Operations ledger events, as an Operations module would receive them. */
@Component
public class CapturedLedgerEvents {

  private final List<Object> received = new CopyOnWriteArrayList<>();

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void booked(OpsInvoiceBooked event) {
    received.add(event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void moved(InvoiceMovementPosted event) {
    received.add(event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void flagged(InvoiceFlagChanged event) {
    received.add(event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void locked(InvoiceLocked event) {
    received.add(event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void disbursement(DisbursementStatusChanged event) {
    received.add(event);
  }

  /** Events of a type. */
  public <T> List<T> of(Class<T> type) {
    return received.stream().filter(type::isInstance).map(type::cast).toList();
  }
}
