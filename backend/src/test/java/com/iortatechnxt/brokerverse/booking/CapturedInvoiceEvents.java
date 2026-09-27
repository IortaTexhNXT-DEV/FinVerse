package com.iortatechnxt.brokerverse.booking;

import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A consumer of {@link InvoiceBooked} as Operations will write it: it listens AFTER_COMMIT, so it
 * only ever sees committed bookings.
 */
@Component
public class CapturedInvoiceEvents {

  private final List<InvoiceBooked> received = new CopyOnWriteArrayList<>();

  /**
   * Records a committed booking.
   *
   * @param event booked invoice
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(InvoiceBooked event) {
    received.add(event);
  }

  /** The events received for an account. */
  public List<InvoiceBooked> forArn(String arn) {
    return received.stream().filter(e -> e.arn().equals(arn)).toList();
  }

  /** The event of an invoice. */
  public Optional<InvoiceBooked> forInvoice(String invoiceNo) {
    return received.stream().filter(e -> e.invoiceNo().equals(invoiceNo)).findFirst();
  }
}
