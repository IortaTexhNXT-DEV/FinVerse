package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.tax.domain.ReturnFigures;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.TaxDocumentLine;
import com.iortatechnxt.brokerverse.tax.service.TaxWorksheet;
import com.iortatechnxt.brokerverse.tax.service.TaxWorksheetService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * TAX-MAP (Monthly Alphalist of Payees, list #28) and TAX-1604E (annual information return of
 * creditable taxes withheld, alphalist of payees) - FRBS 3.2.0, Appendix A VII: one line per payee
 * and ATC of the EWT worksheet of the month or year, with the income paid and the tax withheld.
 * Format and channel (DAT file) are open (AQ07); the report exports to Excel, PDF and CSV.
 */
public final class PayeeAlphalistReport implements ReportDefinition {

  private static final String MONTH = "month";

  private final TaxWorksheetService worksheets;
  private final Clock clock;
  private final boolean annual;

  /**
   * Creates the report.
   *
   * @param worksheets EWT worksheet
   * @param clock clock (parameter defaults)
   * @param annual the annual alphalist (1604-E) rather than the monthly one (MAP)
   */
  PayeeAlphalistReport(TaxWorksheetService worksheets, Clock clock, boolean annual) {
    this.worksheets = worksheets;
    this.clock = clock;
    this.annual = annual;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(TaxReportSupport.company());
    params.add(TaxReportSupport.year(clock));
    if (!annual) {
      params.add(
          ParameterSpec.select(
              MONTH,
              "Month",
              IntStream.rangeClosed(1, 12).mapToObj(String::valueOf).toList(),
              String.valueOf(LocalDate.now(clock).getMonthValue())));
    }
    return new ReportMetadata(
        annual ? "TAX-1604E" : "TAX-MAP",
        annual
            ? "Annual Information Return 1604-E - Alphalist of Payees"
            : "Monthly Alphalist of Payees (MAP)",
        ReportCategory.TAX_STATUTORY,
        annual
            ? "Payees subject to expanded withholding in the year with income and tax withheld"
                + " (FRBS 3.2.0, App. A VII)"
            : "Payees subject to expanded withholding in the month with income and tax withheld"
                + " (FRBS 3.2.0, App. A VII)",
        params,
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    int year = TaxReportSupport.year(p);
    TaxPeriod period =
        annual
            ? new TaxPeriod(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
            : TaxPeriod.month(YearMonth.of(year, Integer.parseInt(p.text(MONTH))));
    TaxWorksheet w = worksheets.compute(TaxReportSupport.companyId(p), WorksheetKind.EWT, period);
    Map<String, Map<String, Object>> byPayee = new LinkedHashMap<>();
    for (TaxDocumentLine d : w.section(TaxDocumentLine.WITHHOLDING)) {
      Map<String, Object> row =
          byPayee.computeIfAbsent(
              d.partyCode() + "|" + d.taxCode(),
              k -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put(TaxReportSupport.TIN, d.tin());
                r.put(TaxReportSupport.PARTY, d.partyName());
                r.put("atc", d.taxCode());
                r.put("nature", d.incomeNature());
                r.put(TaxReportSupport.BASE, BigDecimal.ZERO);
                r.put(TaxReportSupport.TAX, BigDecimal.ZERO);
                return r;
              });
      row.merge(
          TaxReportSupport.BASE, d.taxableAmount(), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
      row.merge(
          TaxReportSupport.TAX, d.taxAmount(), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
    }
    List<Map<String, Object>> rows = new ArrayList<>();
    int seq = 0;
    for (Map<String, Object> row : byPayee.values()) {
      seq++;
      row.put("seq", seq);
      row.put(
          "rate",
          ReturnFigures.effectiveRate(
              (BigDecimal) row.get(TaxReportSupport.BASE),
              (BigDecimal) row.get(TaxReportSupport.TAX)));
      rows.add(row);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("seq", "Seq"),
            ReportColumn.text(TaxReportSupport.TIN, "TIN"),
            ReportColumn.text(TaxReportSupport.PARTY, "Payee"),
            ReportColumn.text("atc", "ATC"),
            ReportColumn.text("nature", "Nature of income"),
            ReportColumn.percent("rate", "Rate"),
            ReportColumn.amount(TaxReportSupport.BASE, "Income payment"),
            ReportColumn.amount(TaxReportSupport.TAX, "Tax withheld"))
        .rows(rows)
        .presorted()
        .note(period.label() + "; format and DAT channel to confirm (AQ07).")
        .build();
  }
}
