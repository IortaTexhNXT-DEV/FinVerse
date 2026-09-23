package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.tax.domain.TaxType;
import com.iortatechnxt.finverse.tax.service.TaxWorksheetService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * TAX-PREMTAX – Premium tax (2551Q), local government tax or fire service tax worksheet per month:
 * the levy charged on each policy and endorsement of the period.
 */
@Component
public class PremiumTaxReport implements ReportDefinition {

  private static final String TAX_TYPE = "taxType";

  private final TaxWorksheetService worksheets;

  /**
   * Creates the report.
   *
   * @param worksheets worksheets
   */
  public PremiumTaxReport(TaxWorksheetService worksheets) {
    this.worksheets = worksheets;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "TAX-PREMTAX",
        "Premium Tax, LGT and FST Worksheet",
        ReportCategory.TAX_STATUTORY,
        "Premium tax (2551Q), local government tax or fire service tax per month",
        List.of(
            TaxReportSupport.company(),
            ParameterSpec.select(
                TAX_TYPE,
                "Tax",
                List.of(TaxType.PREMIUM_TAX.name(), TaxType.LGT.name(), TaxType.FST.name()),
                TaxType.PREMIUM_TAX.name()),
            TaxReportSupport.from(),
            TaxReportSupport.to()),
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return PremiumLevyReports.register(worksheets, TaxType.valueOf(p.text(TAX_TYPE)), p);
  }
}
