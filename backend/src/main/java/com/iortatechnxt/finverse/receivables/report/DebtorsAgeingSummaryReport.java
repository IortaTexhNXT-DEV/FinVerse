package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
import com.iortatechnxt.finverse.subledger.service.AgeingService;
import org.springframework.stereotype.Component;

/** FIN-AR-AGE-SUM (Src FAP008) Debtors Aged Analysis - Summary. */
@Component
public class DebtorsAgeingSummaryReport extends AbstractDebtorsAgeingSummary {

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   * @param ageing sub-ledger ageing (default slots)
   */
  public DebtorsAgeingSummaryReport(ReceivablesQueries queries, AgeingService ageing) {
    super(queries, ageing, "FIN-AR-AGE-SUM", "Debtors Aged Analysis - Summary", false);
  }
}
