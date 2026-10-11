package com.iortatechnxt.brokerverse.opsledger.domain;

import java.math.BigDecimal;
import java.util.List;

/** Collection status of an invoice's premium receivable, derived from its PR balances. */
public enum PaymentStatus {
  /** Nothing applied yet. */
  UNPAID,
  /** Part of the premium applied. */
  PARTIALLY_PAID,
  /** Premium fully applied (or covered by the client's 2307). */
  PAID,
  /** No client receivable: direct payment (BRNB.114) or a return invoice. */
  NOT_APPLICABLE;

  /**
   * The status of a receivable invoice from its premium components: paid when nothing is
   * outstanding, unpaid when nothing was collected, else partially paid.
   *
   * @param components the invoice's components
   * @return UNPAID, PARTIALLY_PAID or PAID
   */
  public static PaymentStatus of(List<OpsInvoiceComponent> components) {
    BigDecimal due = BigDecimal.ZERO;
    BigDecimal balance = BigDecimal.ZERO;
    for (OpsInvoiceComponent c : components) {
      if (c.getComponent().isPremiumReceivable()) {
        due = due.add(c.due());
        balance = balance.add(c.getBalance());
      }
    }
    if (balance.signum() <= 0) {
      return PAID;
    }
    return balance.compareTo(due) >= 0 ? UNPAID : PARTIALLY_PAID;
  }
}
