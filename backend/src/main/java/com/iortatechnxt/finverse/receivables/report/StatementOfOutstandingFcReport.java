package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.service.PdcQueries;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
import org.springframework.stereotype.Component;

/** FIN-AR-SOO-FC (Src FAP007) Statement of Outstanding in one foreign currency (no conversion). */
@Component
public class StatementOfOutstandingFcReport extends AbstractOutstandingStatement {

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   * @param pdcs PDC register
   */
  public StatementOfOutstandingFcReport(ReceivablesQueries queries, PdcQueries pdcs) {
    super(queries, pdcs, "FIN-AR-SOO-FC", "Statement of Outstanding (FC)", true);
  }
}
