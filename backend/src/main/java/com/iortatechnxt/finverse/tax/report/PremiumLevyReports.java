package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.tax.domain.TaxType;
import com.iortatechnxt.finverse.tax.domain.WorksheetKind;
import com.iortatechnxt.finverse.tax.service.LevyWorksheetBuilder;
import com.iortatechnxt.finverse.tax.service.TaxWorksheet;
import com.iortatechnxt.finverse.tax.service.TaxWorksheetService;

/**
 * Builds the register of a premium levy (DST, premium tax, LGT, FST): the policies and endorsements
 * of the period grouped by month, with the ledger reconciliation as footnotes.
 */
final class PremiumLevyReports {

  private PremiumLevyReports() {}

  static ReportResult register(TaxWorksheetService worksheets, TaxType type, ReportParameters p) {
    TaxWorksheet w =
        worksheets.compute(
            TaxReportSupport.companyId(p), WorksheetKind.ofLevy(type), TaxReportSupport.period(p));
    TabularReportBuilder b =
        TabularReportBuilder.of(p)
            .columns(TaxReportSupport.premiumColumns(LevyWorksheetBuilder.label(type)))
            .groupBy(TaxReportSupport.MONTH, "Month")
            .rows(w.documents().stream().map(TaxReportSupport::premiumRow).toList());
    TaxReportSupport.notes(w).forEach(b::note);
    return b.build();
  }
}
