package com.iortatechnxt.brokerverse.receivables.report;

import com.iortatechnxt.brokerverse.receivables.service.PdcQueries;
import com.iortatechnxt.brokerverse.receivables.service.ReceivablesQueries;
import com.iortatechnxt.brokerverse.subledger.service.AgeingService;
import org.springframework.stereotype.Component;

/** FIN-AR-SOO-FC (Src FAP007) Statement of Outstanding in one foreign currency (no conversion). */
@Component
public class StatementOfOutstandingFcReport extends AbstractOutstandingStatement {

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   * @param pdcs PDC register
   * @param ageing sub-ledger ageing (default slots)
   */
  public StatementOfOutstandingFcReport(
      ReceivablesQueries queries, PdcQueries pdcs, AgeingService ageing) {
    super(queries, pdcs, ageing, "FIN-AR-SOO-FC", "Statement of Outstanding (FC)", true);
  }
}
