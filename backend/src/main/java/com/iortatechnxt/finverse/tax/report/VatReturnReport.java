package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.tax.domain.WorksheetKind;
import com.iortatechnxt.finverse.tax.service.TaxWorksheet;
import com.iortatechnxt.finverse.tax.service.TaxWorksheetService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * TAX-VAT-2550Q – VAT return worksheet: VATable, zero-rated and exempt sales, output VAT,
 * purchases, input VAT, carry-over and VAT payable, with the ledger reconciliation as footnotes.
 */
@Component
public class VatReturnReport implements ReportDefinition {

  private final TaxWorksheetService worksheets;

  /**
   * Creates the report.
   *
   * @param worksheets worksheets
   */
  public VatReturnReport(TaxWorksheetService worksheets) {
    this.worksheets = worksheets;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "TAX-VAT-2550Q",
        "VAT Return Worksheet (BIR 2550Q)",
        ReportCategory.TAX_STATUTORY,
        "Output VAT on premiums, input VAT on purchases and VAT payable for a period",
        List.of(TaxReportSupport.company(), TaxReportSupport.from(), TaxReportSupport.to()),
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    TaxWorksheet w =
        worksheets.compute(
            TaxReportSupport.companyId(p), WorksheetKind.VAT, TaxReportSupport.period(p));
    TabularReportBuilder b =
        TabularReportBuilder.of(p)
            .columns(TaxReportSupport.summaryColumns())
            .rows(TaxReportSupport.summaryRows(w.lines()))
            .presorted()
            .withoutGrandTotal();
    TaxReportSupport.notes(w).forEach(b::note);
    return b.build();
  }
}
