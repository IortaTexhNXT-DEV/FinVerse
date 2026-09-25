package com.iortatechnxt.brokerverse.collections.promise.service;

import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise.Outcome;
import com.iortatechnxt.brokerverse.collections.promise.domain.PromiseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The rule of BRCLXN.055 (to confirm with BDOI, CQ16): a promise is kept when the payments applied
 * between the day it was made and its date plus the grace days reach the promised amount, or when
 * nothing is left to collect on the invoice; partially kept when some of it was paid; otherwise
 * broken.
 */
public final class PromiseEvaluator {

  private PromiseEvaluator() {}

  /**
   * Evaluates a promise.
   *
   * @param promised promised amount
   * @param paid net payments applied in the window
   * @param lastPaymentDate value date of the last payment, may be null
   * @param nothingLeft whether the invoice's outstanding is at or below the threshold
   * @return outcome
   */
  public static Outcome evaluate(
      BigDecimal promised, BigDecimal paid, LocalDate lastPaymentDate, boolean nothingLeft) {
    BigDecimal net = paid.max(BigDecimal.ZERO);
    PromiseStatus status;
    if (nothingLeft || net.compareTo(promised) >= 0) {
      status = PromiseStatus.KEPT;
    } else if (net.signum() > 0) {
      status = PromiseStatus.PARTIALLY_KEPT;
    } else {
      status = PromiseStatus.BROKEN;
    }
    return new Outcome(status, net, lastPaymentDate);
  }
}
