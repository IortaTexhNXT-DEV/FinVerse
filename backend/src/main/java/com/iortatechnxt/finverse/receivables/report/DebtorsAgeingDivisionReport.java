package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
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
   */
  public DebtorsAgeingDivisionReport(ReceivablesQueries queries) {
    super(queries, "FIN-AR-AGE-DIV", "Debtors Aged Analysis - Summary - Division-wise", true);
  }
}
