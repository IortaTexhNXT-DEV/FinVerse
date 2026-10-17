package com.iortatechnxt.brokerverse.cashiering;

import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.PaymentReversalCompleted;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.RefundValidationCompleted;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.UnappliedDispositionChanged;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Captures the answers cashiering publishes to the requesting modules (wave C1-C). */
@Component
public class CapturedCashieringAnswers {

  private final List<Object> received = new CopyOnWriteArrayList<>();

  @EventListener
  void changed(UnappliedDispositionChanged event) {
    received.add(event);
  }

  @EventListener
  void validated(RefundValidationCompleted event) {
    received.add(event);
  }

  @EventListener
  void reversed(PaymentReversalCompleted event) {
    received.add(event);
  }

  /** Events of a type. */
  public <T> List<T> of(Class<T> type) {
    return received.stream().filter(type::isInstance).map(type::cast).toList();
  }
}
