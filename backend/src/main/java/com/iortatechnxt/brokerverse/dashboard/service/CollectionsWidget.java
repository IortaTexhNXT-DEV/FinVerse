package com.iortatechnxt.brokerverse.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Collections (approved receipts) against the debtors' outstanding balance and its ageing.
 *
 * @param asOf reference date
 * @param collectedMonthToDate receipts of the current month
 * @param collectedYearToDate receipts of the fiscal year
 * @param receivables outstanding debit items of debtors
 * @param notYetDue part of the receivables not yet due
 * @param ageingSlots slot definition used for the buckets, e.g. "30/60/90/120"
 * @param ageing receivables per ageing bucket (due-date basis; not-yet-due in the first bucket)
 * @param monthly receipts per month of the fiscal year
 */
public record CollectionsWidget(
    LocalDate asOf,
    BigDecimal collectedMonthToDate,
    BigDecimal collectedYearToDate,
    BigDecimal receivables,
    BigDecimal notYetDue,
    String ageingSlots,
    List<LabelledAmount> ageing,
    List<MonthlyValue> monthly) {

  /** Canonical constructor copying the lists. */
  public CollectionsWidget {
    ageing = List.copyOf(ageing);
    monthly = List.copyOf(monthly);
  }
}
