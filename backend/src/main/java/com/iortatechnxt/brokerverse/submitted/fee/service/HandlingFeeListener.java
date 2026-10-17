package com.iortatechnxt.brokerverse.submitted.fee.service;

import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.UnappliedDispositionChanged;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Receives the answers of Cashiering to the handling-fee requests (BRIDSP-31) in the cashiering
 * transaction: the record becomes APPLIED with the official receipt, or back to BILLED when
 * refused.
 */
@Component
public class HandlingFeeListener {

  private final HandlingFeeService fees;

  /**
   * Creates the listener.
   *
   * @param fees handling fees
   */
  public HandlingFeeListener(HandlingFeeService fees) {
    this.fees = fees;
  }

  /**
   * A disposition requested through the port changed.
   *
   * @param event change
   */
  @EventListener
  public void on(UnappliedDispositionChanged event) {
    fees.answered(event);
  }
}
