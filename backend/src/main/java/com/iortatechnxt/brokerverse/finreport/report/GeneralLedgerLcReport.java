package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import org.springframework.stereotype.Component;

/** FIN-GL-LEDGER-LC – General Ledger in local currency (PREMIA FGL010B). */
@Component
public class GeneralLedgerLcReport implements ReportDefinition {

  private final LedgerEngine engine;

  /**
   * Creates the report.
   *
   * @param engine ledger engine
   */
  public GeneralLedgerLcReport(LedgerEngine engine) {
    this.engine = engine;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-GL-LEDGER-LC",
        "General Ledger (LC)",
        ReportCategory.GENERAL_LEDGER,
        "Local currency ledger per main account with opening, running and closing balance (FGL010B)",
        LedgerEngine.generalParameters(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return engine.generalLedger(p, false);
  }
}
