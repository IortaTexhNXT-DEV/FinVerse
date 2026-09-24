package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.tax.domain.TaxType;
import com.iortatechnxt.brokerverse.tax.service.TaxWorksheetService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * TAX-DST-2000 – Documentary stamp tax on policies (BIR Form 2000 worksheet): DST per policy and
 * endorsement, by month of the accounting date.
 */
@Component
public class DstReturnReport implements ReportDefinition {

  private final TaxWorksheetService worksheets;

  /**
   * Creates the report.
   *
   * @param worksheets worksheets
   */
  public DstReturnReport(TaxWorksheetService worksheets) {
    this.worksheets = worksheets;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "TAX-DST-2000",
        "Documentary Stamp Tax Worksheet (BIR 2000)",
        ReportCategory.TAX_STATUTORY,
        "DST on policies and endorsements per month",
        List.of(TaxReportSupport.company(), TaxReportSupport.from(), TaxReportSupport.to()),
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return PremiumLevyReports.register(worksheets, TaxType.DST, p);
  }
}
