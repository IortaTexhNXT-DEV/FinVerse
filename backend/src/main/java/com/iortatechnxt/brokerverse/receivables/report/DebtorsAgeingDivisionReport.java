package com.iortatechnxt.brokerverse.receivables.report;

import com.iortatechnxt.brokerverse.receivables.service.ReceivablesQueries;
import com.iortatechnxt.brokerverse.subledger.service.AgeingService;
import org.springframework.stereotype.Component;

/**
 * FIN-AR-AGE-DIV (Src FAP008 layout) Debtors Aged Analysis - Summary - Division-wise. The division
 * is the branch that raised the document.
 */
@Component
public class DebtorsAgeingDivisionReport extends AbstractDebtorsAgeingSummary {

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   * @param ageing sub-ledger ageing (default slots)
   */
  public DebtorsAgeingDivisionReport(ReceivablesQueries queries, AgeingService ageing) {
    super(
        queries, ageing, "FIN-AR-AGE-DIV", "Debtors Aged Analysis - Summary - Division-wise", true);
  }
}
