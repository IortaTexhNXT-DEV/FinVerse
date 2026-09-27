package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.tax.domain.AtcQuarterAmounts;
import com.iortatechnxt.brokerverse.tax.domain.Certificate2307Aggregator;
import com.iortatechnxt.brokerverse.tax.domain.ReturnFigures;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.EwtWorksheetBuilder;
import com.iortatechnxt.brokerverse.tax.service.TaxDocumentLine;
import com.iortatechnxt.brokerverse.tax.service.TaxWorksheet;
import com.iortatechnxt.brokerverse.tax.service.TaxWorksheetService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * TAX-QAP – Quarterly Alphalist of Payees (attachment of 1601-EQ): one line per payee and ATC with
 * the income of each month of the quarter, the rate and the tax withheld. The BIR alphalist CSV is
 * produced by {@code /api/v1/tax/exports/QAP}.
 */
@Component
public class AlphalistReport implements ReportDefinition {

  private final TaxWorksheetService worksheets;
  private final Clock clock;

  /**
   * Creates the report.
   *
   * @param worksheets worksheets
   * @param clock clock (parameter defaults)
   */
  public AlphalistReport(TaxWorksheetService worksheets, Clock clock) {
    this.worksheets = worksheets;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "TAX-QAP",
        "Quarterly Alphalist of Payees (QAP)",
        ReportCategory.TAX_STATUTORY,
        "Payees subject to expanded withholding: income by month of the quarter and tax withheld",
        List.of(
            TaxReportSupport.company(),
            TaxReportSupport.year(clock),
            TaxReportSupport.quarter(clock)),
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    TaxPeriod quarter = TaxReportSupport.quarterPeriod(p);
    TaxWorksheet w = worksheets.compute(TaxReportSupport.companyId(p), WorksheetKind.EWT, quarter);
    Map<String, TaxDocumentLine> firstDoc = new HashMap<>();
    w.documents().forEach(d -> firstDoc.putIfAbsent(d.partyCode(), d));
    Map<String, List<AtcQuarterAmounts>> byPayee =
        Certificate2307Aggregator.aggregate(quarter, EwtWorksheetBuilder.entries(w));
    List<Map<String, Object>> rows = new ArrayList<>();
    int seq = 0;
    for (Map.Entry<String, List<AtcQuarterAmounts>> e : byPayee.entrySet()) {
      TaxDocumentLine payee = firstDoc.get(e.getKey());
      for (AtcQuarterAmounts a : e.getValue()) {
        seq++;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("seq", seq);
        row.put(TaxReportSupport.TIN, payee.tin());
        row.put(TaxReportSupport.PARTY, payee.partyName());
        row.put("atc", a.atc());
        row.put("nature", a.incomeNature());
        row.put("rate", ReturnFigures.effectiveRate(a.total(), a.tax()));
        row.put("m1", a.month1());
        row.put("m2", a.month2());
        row.put("m3", a.month3());
        row.put(TaxReportSupport.BASE, a.total());
        row.put(TaxReportSupport.TAX, a.tax());
        rows.add(row);
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("seq", "Seq"),
            ReportColumn.text(TaxReportSupport.TIN, "TIN"),
            ReportColumn.text(TaxReportSupport.PARTY, "Payee"),
            ReportColumn.text("atc", "ATC"),
            ReportColumn.text("nature", "Nature of income"),
            ReportColumn.percent("rate", "Rate"),
            ReportColumn.amount("m1", "1st month"),
            ReportColumn.amount("m2", "2nd month"),
            ReportColumn.amount("m3", "3rd month"),
            ReportColumn.amount(TaxReportSupport.BASE, "Total income"),
            ReportColumn.amount(TaxReportSupport.TAX, "Tax withheld"))
        .rows(rows)
        .presorted()
        .note("Quarter " + quarter.label() + "; payees in party code order.")
        .build();
  }
}
