package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import org.springframework.stereotype.Component;

/** FIN-GL-SUBLEDGER-FC – Sub Ledger in transaction currency by party code (PREMIA FGL011A). */
@Component
public class SubLedgerFcReport implements ReportDefinition {

  private final LedgerEngine engine;

  /**
   * Creates the report.
   *
   * @param engine ledger engine
   */
  public SubLedgerFcReport(LedgerEngine engine) {
    this.engine = engine;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-GL-SUBLEDGER-FC",
        "Sub Ledger (FC)",
        ReportCategory.GENERAL_LEDGER,
        "Foreign currency ledger per control account, party and currency (FGL011A)",
        LedgerEngine.subParameters(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return engine.subLedger(p, true);
  }
}
