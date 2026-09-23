package com.iortatechnxt.finverse.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cash and bank position.
 *
 * @param asOf reference date
 * @param total balance of the cash statement lines
 * @param accounts balance per cash or bank account
 * @param monthly month-end balance of the fiscal year (the current month at the reference date)
 */
public record CashWidget(
    LocalDate asOf, BigDecimal total, List<LabelledAmount> accounts, List<MonthlyValue> monthly) {

  /** Canonical constructor copying the lists. */
  public CashWidget {
    accounts = List.copyOf(accounts);
    monthly = List.copyOf(monthly);
  }
}
