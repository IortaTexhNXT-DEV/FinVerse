package com.iortatechnxt.brokerverse.report.core;

/** Report menu groups. */
public enum ReportCategory {
  GENERAL_LEDGER("General Ledger"),
  FINANCIAL_STATEMENTS("Financial Statements"),
  UNDERWRITING("Underwriting"),
  CLAIMS("Claims"),
  REINSURANCE("Reinsurance"),
  ACTUARIAL("Processing & Reserves"),
  RECEIVABLES_PAYABLES("Receivables & Payables"),
  BUDGET("Budget"),
  RECONCILIATION("Reconciliation"),
  CONTROL("Control & Audit"),
  TAX_STATUTORY("Tax & Statutory"),
  OPERATIONS("Operations");

  private final String label;

  ReportCategory(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
