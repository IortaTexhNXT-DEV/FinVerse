package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.security.domain.Permission;
import org.springframework.stereotype.Component;

/** FIN-GL-PROCLIST – List of Processed / Unprocessed Transactions (PREMIA FGL002). */
@Component
public class ProcessedTransactionsReport implements ReportDefinition {

  private final VoucherListingEngine engine;

  /**
   * Creates the report.
   *
   * @param engine voucher listing engine
   */
  public ProcessedTransactionsReport(VoucherListingEngine engine) {
    this.engine = engine;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-GL-PROCLIST",
        "List of Processed / Unprocessed Transactions",
        ReportCategory.GENERAL_LEDGER,
        "Posted and / or unposted vouchers with transaction-wise summary (FGL002)",
        VoucherListingEngine.parameters(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return engine.generate(p, true);
  }
}
