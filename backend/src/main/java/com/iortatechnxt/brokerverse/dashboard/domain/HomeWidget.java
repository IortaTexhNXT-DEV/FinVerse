package com.iortatechnxt.brokerverse.dashboard.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The widgets a role home page can show (Home &gt; Dashboard), in their delivered order. A role
 * without its own set shows them all; a widget of a product module that is switched off is never
 * shown.
 */
public enum HomeWidget {
  /** Headline figures of the ledger. */
  KPIS("Headline figures"),
  /** Work waiting for the user. */
  WORKLOAD("My workload"),
  /** Receivables collections and ageing. */
  COLLECTIONS("Collections"),
  /** Payables due (Payables module). */
  PAYABLES("Payables"),
  /** Cash position. */
  CASH("Cash position"),
  /** Budget against actual (Budget module). */
  BUDGET("Budget"),
  /** Monthly income and expense charts. */
  CHARTS("Income and expense charts");

  private final String displayName;

  HomeWidget(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }

  /**
   * The delivered set: every widget in catalogue order.
   *
   * @return widget codes
   */
  public static List<String> defaults() {
    return Arrays.stream(values()).map(Enum::name).toList();
  }

  /**
   * Finds a widget by code.
   *
   * @param code widget code
   * @return the widget, empty when unknown
   */
  public static Optional<HomeWidget> byCode(String code) {
    return Arrays.stream(values()).filter(w -> w.name().equals(code)).findFirst();
  }
}
