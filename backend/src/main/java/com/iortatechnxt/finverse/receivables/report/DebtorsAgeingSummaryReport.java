package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
import org.springframework.stereotype.Component;

/** FIN-AR-AGE-SUM (Src FAP008) Debtors Aged Analysis - Summary. */
@Component
public class DebtorsAgeingSummaryReport extends AbstractDebtorsAgeingSummary {

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   */
  public DebtorsAgeingSummaryReport(ReceivablesQueries queries) {
    super(queries, "FIN-AR-AGE-SUM", "Debtors Aged Analysis - Summary", false);
  }
}
