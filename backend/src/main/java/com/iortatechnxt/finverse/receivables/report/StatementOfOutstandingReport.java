package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.service.PdcQueries;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
import org.springframework.stereotype.Component;

/**
 * FIN-AR-SOO (Src FAP007A) Statement of Outstanding in local (base) currency; foreign currency
 * items are converted at their historical rate.
 */
@Component
public class StatementOfOutstandingReport extends AbstractOutstandingStatement {

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   * @param pdcs PDC register
   */
  public StatementOfOutstandingReport(ReceivablesQueries queries, PdcQueries pdcs) {
    super(queries, pdcs, "FIN-AR-SOO", "Statement of Outstanding (LC)", false);
  }
}
