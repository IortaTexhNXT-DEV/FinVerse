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
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.service.FormWorksheetService;
import com.iortatechnxt.brokerverse.tax.service.FormWorksheetService.LineValue;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * A BIR form worksheet of the report pack (FRBS 3.2.0, Appendix A VII): 0619-F (monthly final
 * withholding tax), 1603 (quarterly fringe benefit tax), 1702-Q (quarterly income tax, year to
 * date) and 1702 (annual income tax). The lines are configuration (V703); the forms' layout and
 * filing channel are open (AQ07), so the worksheet prepares the figures of the return.
 */
public final class FormWorksheetReport implements ReportDefinition {

  private static final String MONTH = "month";

  private final FormWorksheetService forms;
  private final Clock clock;
  private final Form form;

  /**
   * Creates the report.
   *
   * @param forms form worksheets
   * @param clock clock (parameter defaults)
   * @param form which form
   */
  FormWorksheetReport(FormWorksheetService forms, Clock clock, Form form) {
    this.forms = forms;
    this.clock = clock;
    this.form = form;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(TaxReportSupport.company());
    params.add(TaxReportSupport.year(clock));
    if (form.frequency() == Frequency.MONTH) {
      params.add(
          ParameterSpec.select(
              MONTH,
              "Month",
              IntStream.rangeClosed(1, 12).mapToObj(String::valueOf).toList(),
              String.valueOf(LocalDate.now(clock).getMonthValue())));
    } else if (form.frequency() != Frequency.YEAR) {
      params.add(TaxReportSupport.quarter(clock));
    }
    return new ReportMetadata(
        "TAX-" + form.code(),
        form.title(),
        ReportCategory.TAX_STATUTORY,
        form.description(),
        params,
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    int year = TaxReportSupport.year(p);
    TaxPeriod period =
        switch (form.frequency()) {
          case MONTH -> TaxPeriod.month(YearMonth.of(year, Integer.parseInt(p.text(MONTH))));
          case QUARTER -> TaxReportSupport.quarterPeriod(p);
          case QUARTER_YTD ->
              new TaxPeriod(LocalDate.of(year, 1, 1), TaxReportSupport.quarterPeriod(p).to());
          case YEAR -> new TaxPeriod(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        };
    List<LineValue> lines =
        forms.compute(TaxReportSupport.companyId(p), form.code(), period, period);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (LineValue l : lines) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put(TaxReportSupport.CODE, String.valueOf(l.lineNo()));
      row.put(TaxReportSupport.DESCRIPTION, l.label());
      row.put(TaxReportSupport.AMOUNT, l.amount());
      row.put("basis", l.note());
      rows.add(row);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(TaxReportSupport.CODE, "Line"),
            ReportColumn.text(TaxReportSupport.DESCRIPTION, "Description"),
            ReportColumn.amountNoTotal(TaxReportSupport.AMOUNT, "Amount"),
            ReportColumn.text("basis", "Basis"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .note(
            period.label()
                + "; worksheet of the return, form layout and channel to confirm (AQ07).")
        .build();
  }

  /** How often a form is filed. */
  enum Frequency {
    /** Monthly. */
    MONTH,
    /** Quarterly. */
    QUARTER,
    /** Quarterly, cumulative from the start of the year. */
    QUARTER_YTD,
    /** Annually. */
    YEAR
  }

  /**
   * A form.
   *
   * @param code form code in V703 ({@code 0619F}, {@code 1603}, {@code 1702Q}, {@code 1702})
   * @param title title
   * @param description description
   * @param frequency filing frequency
   */
  record Form(String code, String title, String description, Frequency frequency) {}
}
