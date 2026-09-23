package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.security.domain.Permission;
import org.springframework.stereotype.Component;

/** FIN-GL-SUBLEDGER-LC – Sub Ledger in local currency by party code (PREMIA FGL011B). */
@Component
public class SubLedgerLcReport implements ReportDefinition {

  private final LedgerEngine engine;

  /**
   * Creates the report.
   *
   * @param engine ledger engine
   */
  public SubLedgerLcReport(LedgerEngine engine) {
    this.engine = engine;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-GL-SUBLEDGER-LC",
        "Sub Ledger (LC)",
        ReportCategory.GENERAL_LEDGER,
        "Local currency ledger per control account and party (FGL011B)",
        LedgerEngine.subParameters(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return engine.subLedger(p, false);
  }
}
