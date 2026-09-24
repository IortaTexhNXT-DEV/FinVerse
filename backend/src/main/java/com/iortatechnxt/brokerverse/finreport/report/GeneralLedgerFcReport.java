package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import org.springframework.stereotype.Component;

/** FIN-GL-LEDGER-FC – General Ledger in transaction currency (PREMIA FGL010A). */
@Component
public class GeneralLedgerFcReport implements ReportDefinition {

  private final LedgerEngine engine;

  /**
   * Creates the report.
   *
   * @param engine ledger engine
   */
  public GeneralLedgerFcReport(LedgerEngine engine) {
    this.engine = engine;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-GL-LEDGER-FC",
        "General Ledger (FC)",
        ReportCategory.GENERAL_LEDGER,
        "Foreign currency ledger per main account and currency (FGL010A)",
        LedgerEngine.generalParameters(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return engine.generalLedger(p, true);
  }
}
