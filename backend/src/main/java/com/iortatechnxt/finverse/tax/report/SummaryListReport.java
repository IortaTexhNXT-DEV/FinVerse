package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.tax.domain.WorksheetKind;
import com.iortatechnxt.finverse.tax.service.PartySummary;
import com.iortatechnxt.finverse.tax.service.TaxDocumentLine;
import com.iortatechnxt.finverse.tax.service.TaxWorksheetService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Summary List of Sales or of Purchases (BIR RELIEF): the VAT worksheet documents of a period
 * totalled per customer or supplier with TIN, exempt, zero-rated and taxable amounts and VAT. The
 * BIR relief-style CSV of the quarter is produced by {@code /api/v1/tax/exports/SLS|SLP}.
 */
abstract class SummaryListReport implements ReportDefinition {

  private static final String EXEMPT = "exempt";
  private static final String ZERO_RATED = "zeroRated";
  private static final String TAXABLE = "taxable";
  private static final String CAPITAL = "capital";

  private final TaxWorksheetService worksheets;
  private final boolean sales;

  SummaryListReport(TaxWorksheetService worksheets, boolean sales) {
    this.worksheets = worksheets;
    this.sales = sales;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        sales ? "TAX-SLS" : "TAX-SLP",
        sales ? "Summary List of Sales" : "Summary List of Purchases",
        ReportCategory.TAX_STATUTORY,
        sales
            ? "Premiums per customer: exempt, zero-rated and VATable sales with output VAT"
            : "Purchases per supplier: exempt, zero-rated, services and capital goods with input VAT",
        List.of(TaxReportSupport.company(), TaxReportSupport.from(), TaxReportSupport.to()),
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<TaxDocumentLine> docs =
        worksheets
            .compute(TaxReportSupport.companyId(p), WorksheetKind.VAT, TaxReportSupport.period(p))
            .section(sales ? TaxDocumentLine.SALES : TaxDocumentLine.PURCHASES);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (PartySummary s : PartySummary.of(docs)) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put(TaxReportSupport.TIN, s.tin());
      row.put(TaxReportSupport.PARTY, s.partyName());
      row.put(EXEMPT, s.exempt());
      row.put(ZERO_RATED, s.zeroRated());
      row.put(TAXABLE, s.services());
      row.put(CAPITAL, s.capitalGoods());
      row.put(TaxReportSupport.TAX, s.tax());
      rows.add(row);
    }
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text(TaxReportSupport.TIN, "TIN"));
    columns.add(ReportColumn.text(TaxReportSupport.PARTY, sales ? "Customer" : "Supplier"));
    columns.add(ReportColumn.amount(EXEMPT, "Exempt"));
    columns.add(ReportColumn.amount(ZERO_RATED, "Zero-rated"));
    columns.add(ReportColumn.amount(TAXABLE, sales ? "Taxable sales" : "Services"));
    if (!sales) {
      columns.add(ReportColumn.amount(CAPITAL, "Capital goods"));
    }
    columns.add(ReportColumn.amount(TaxReportSupport.TAX, sales ? "Output VAT" : "Input VAT"));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .rows(rows)
        .presorted()
        .note("One line per " + (sales ? "customer" : "supplier") + " (party code order).")
        .build();
  }
}
