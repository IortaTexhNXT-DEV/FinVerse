package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.security.domain.Permission;
import org.springframework.stereotype.Component;

/** FIN-GL-TXNLIST – List of Transactions Detailed (PREMIA FR2183). */
@Component
public class TransactionListReport implements ReportDefinition {

  private final VoucherListingEngine engine;

  /**
   * Creates the report.
   *
   * @param engine voucher listing engine
   */
  public TransactionListReport(VoucherListingEngine engine) {
    this.engine = engine;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-GL-TXNLIST",
        "List of Transactions Detailed",
        ReportCategory.GENERAL_LEDGER,
        "Account-wise voucher detail by transaction code and status (FR2183)",
        VoucherListingEngine.parameters(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return engine.generate(p, false);
  }
}
