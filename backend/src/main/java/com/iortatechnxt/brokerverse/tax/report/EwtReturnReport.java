package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.tax.domain.ReturnLineValues;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.TaxDocumentLine;
import com.iortatechnxt.brokerverse.tax.service.TaxWorksheet;
import com.iortatechnxt.brokerverse.tax.service.TaxWorksheetService;
import com.iortatechnxt.brokerverse.tax.service.WorksheetSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/**
 * TAX-EWT-1601EQ – Expanded withholding tax worksheet: income payments and tax withheld per ATC and
 * payee (supplier invoices and commissions), with the 0619-E deduction and the tax still due as
 * footnotes.
 */
@Component
public class EwtReturnReport implements ReportDefinition {

  private static final String ATC = "atc";
  private static final String NATURE = "nature";
  private static final String DOCUMENTS = "documents";

  private final TaxWorksheetService worksheets;

  /**
   * Creates the report.
   *
   * @param worksheets worksheets
   */
  public EwtReturnReport(TaxWorksheetService worksheets) {
    this.worksheets = worksheets;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "TAX-EWT-1601EQ",
        "Expanded Withholding Tax Worksheet (BIR 1601-EQ / 0619-E)",
        ReportCategory.TAX_STATUTORY,
        "Income payments and creditable tax withheld per ATC and payee for a period",
        List.of(TaxReportSupport.company(), TaxReportSupport.from(), TaxReportSupport.to()),
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    TaxWorksheet w =
        worksheets.compute(
            TaxReportSupport.companyId(p), WorksheetKind.EWT, TaxReportSupport.period(p));
    Map<String, List<TaxDocumentLine>> byKey = new TreeMap<>();
    w.documents()
        .forEach(
            d ->
                byKey
                    .computeIfAbsent(d.taxCode() + "|" + d.partyCode(), k -> new ArrayList<>())
                    .add(d));
    List<Map<String, Object>> rows = new ArrayList<>();
    byKey.forEach(
        (key, docs) -> {
          TaxDocumentLine first = docs.get(0);
          Map<String, Object> row = new LinkedHashMap<>();
          row.put(ATC, first.taxCode());
          row.put(NATURE, first.incomeNature());
          row.put(TaxReportSupport.PARTY, first.partyName());
          row.put(TaxReportSupport.TIN, first.tin());
          row.put(DOCUMENTS, docs.size());
          row.put(
              TaxReportSupport.BASE, WorksheetSupport.sum(docs, TaxDocumentLine::taxableAmount));
          row.put(TaxReportSupport.TAX, WorksheetSupport.sum(docs, TaxDocumentLine::taxAmount));
          rows.add(row);
        });
    TabularReportBuilder b =
        TabularReportBuilder.of(p)
            .columns(
                ReportColumn.text(NATURE, "Nature of income"),
                ReportColumn.text(TaxReportSupport.PARTY, "Payee"),
                ReportColumn.text(TaxReportSupport.TIN, "TIN"),
                ReportColumn.count(DOCUMENTS, "Documents"),
                ReportColumn.amount(TaxReportSupport.BASE, "Income payments"),
                ReportColumn.amount(TaxReportSupport.TAX, "Tax withheld"))
            .groupBy(ATC, "ATC")
            .rows(rows)
            .presorted();
    for (ReturnLineValues l : w.lines()) {
      if (!l.code().startsWith("ATC:")) {
        b.note(l.description() + ": " + l.amount().toPlainString());
      }
    }
    TaxReportSupport.notes(w).forEach(b::note);
    return b.build();
  }
}
