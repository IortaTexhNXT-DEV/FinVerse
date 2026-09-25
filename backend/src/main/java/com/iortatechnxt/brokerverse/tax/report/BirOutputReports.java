package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.tax.report.FormWorksheetReport.Form;
import com.iortatechnxt.brokerverse.tax.report.FormWorksheetReport.Frequency;
import com.iortatechnxt.brokerverse.tax.service.BirOutputQueries;
import com.iortatechnxt.brokerverse.tax.service.FormWorksheetService;
import com.iortatechnxt.brokerverse.tax.service.TaxWorksheetService;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The new BIR outputs of the report pack (FRBS 3.2.0, Appendix A VII; design section 10): monthly
 * and annual alphalists of payees, the 0619-F, 1603, 1702-Q and 1702 worksheets, the books of
 * accounts and the IC Broker's Annual Statement of Business Operations. SAWT is {@link SawtReport}.
 * Every output exports to Excel and PDF through the Report Centre; BIR formats are open (AQ07).
 */
@Configuration(proxyBeanMethods = false)
public class BirOutputReports {

  private static final String FORM_SOURCE = " (FRBS 3.2.0, App. A VII)";

  /**
   * {@code TAX-MAP}: monthly alphalist of payees (list #28).
   *
   * @param worksheets EWT worksheet
   * @param clock clock
   * @return report
   */
  @Bean
  public ReportDefinition taxMap(TaxWorksheetService worksheets, Clock clock) {
    return new PayeeAlphalistReport(worksheets, clock, false);
  }

  /**
   * {@code TAX-1604E}: annual alphalist of payees.
   *
   * @param worksheets EWT worksheet
   * @param clock clock
   * @return report
   */
  @Bean
  public ReportDefinition tax1604e(TaxWorksheetService worksheets, Clock clock) {
    return new PayeeAlphalistReport(worksheets, clock, true);
  }

  /**
   * {@code TAX-0619F}: monthly remittance of final income taxes withheld.
   *
   * @param forms form worksheets
   * @param clock clock
   * @return report
   */
  @Bean
  public ReportDefinition tax0619f(FormWorksheetService forms, Clock clock) {
    return new FormWorksheetReport(
        forms,
        clock,
        new Form(
            "0619F",
            "Monthly Remittance of Final Income Taxes Withheld (0619-F)",
            "Final withholding taxes of the month to remit" + FORM_SOURCE,
            Frequency.MONTH));
  }

  /**
   * {@code TAX-1603}: quarterly fringe benefit tax.
   *
   * @param forms form worksheets
   * @param clock clock
   * @return report
   */
  @Bean
  public ReportDefinition tax1603(FormWorksheetService forms, Clock clock) {
    return new FormWorksheetReport(
        forms,
        clock,
        new Form(
            "1603",
            "Quarterly Remittance of Fringe Benefit Tax (1603)",
            "Fringe benefits of the quarter, grossed-up value and tax due" + FORM_SOURCE,
            Frequency.QUARTER));
  }

  /**
   * {@code TAX-1702Q}: quarterly income tax return, year to date.
   *
   * @param forms form worksheets
   * @param clock clock
   * @return report
   */
  @Bean
  public ReportDefinition tax1702q(FormWorksheetService forms, Clock clock) {
    return new FormWorksheetReport(
        forms,
        clock,
        new Form(
            "1702Q",
            "Quarterly Income Tax Return (1702-Q)",
            "Gross income, expenses, taxable income, tax due and creditable taxes to the end of the"
                + " quarter"
                + FORM_SOURCE,
            Frequency.QUARTER_YTD));
  }

  /**
   * {@code TAX-1702}: annual income tax return.
   *
   * @param forms form worksheets
   * @param clock clock
   * @return report
   */
  @Bean
  public ReportDefinition tax1702(FormWorksheetService forms, Clock clock) {
    return new FormWorksheetReport(
        forms,
        clock,
        new Form(
            "1702",
            "Annual Income Tax Return (1702)",
            "Gross income, expenses, taxable income, tax due and creditable taxes of the year"
                + FORM_SOURCE,
            Frequency.YEAR));
  }

  /**
   * {@code TAX-BOOK-GJ}: general journal (list #18).
   *
   * @param queries ledger reads
   * @return report
   */
  @Bean
  public ReportDefinition taxBookGj(BirOutputQueries queries) {
    return new BookOfAccountsReport(queries, "GJ", "General Journal");
  }

  /**
   * {@code TAX-BOOK-PJ}: purchase journal (list #19).
   *
   * @param queries ledger reads
   * @return report
   */
  @Bean
  public ReportDefinition taxBookPj(BirOutputQueries queries) {
    return new BookOfAccountsReport(queries, "PJ", "Purchase Journal");
  }

  /**
   * {@code TAX-BOOK-SJ}: sales revenue journal (list #20).
   *
   * @param queries ledger reads
   * @return report
   */
  @Bean
  public ReportDefinition taxBookSj(BirOutputQueries queries) {
    return new BookOfAccountsReport(queries, "SJ", "Sales Revenue Journal");
  }

  /**
   * {@code TAX-BOOK-CRB}: cash receipts book (list #21).
   *
   * @param queries ledger reads
   * @return report
   */
  @Bean
  public ReportDefinition taxBookCrb(BirOutputQueries queries) {
    return new BookOfAccountsReport(queries, "CRB", "Cash Receipts Book");
  }

  /**
   * {@code TAX-BOOK-CDB}: cash disbursements book (list #22).
   *
   * @param queries ledger reads
   * @return report
   */
  @Bean
  public ReportDefinition taxBookCdb(BirOutputQueries queries) {
    return new BookOfAccountsReport(queries, "CDB", "Cash Disbursements Book");
  }

  /**
   * {@code TAX-BOOK-SL}: general ledger per account class (list #23-#27).
   *
   * @param queries ledger reads
   * @return report
   */
  @Bean
  public ReportDefinition taxBookSl(BirOutputQueries queries) {
    return new BookOfAccountsReport(queries, "SL", "General Ledger");
  }

  /**
   * {@code IC-BROKER-ASBO}: Broker's Annual Statement of Business Operations for the Insurance
   * Commission (list #31): policies placed, insurers, premium, commission and VAT on commission per
   * line of business.
   *
   * @param queries production register
   * @param clock clock
   * @return report
   */
  @Bean
  public ReportDefinition icBrokerAsbo(BirOutputQueries queries, Clock clock) {
    return new ReportDefinition() {
      @Override
      public ReportMetadata metadata() {
        return new ReportMetadata(
            "IC-BROKER-ASBO",
            "Broker's Annual Statement of Business Operations",
            ReportCategory.TAX_STATUTORY,
            "Insurance Commission statement: business placed per line of business in the year"
                + FORM_SOURCE,
            List.of(TaxReportSupport.company(), TaxReportSupport.year(clock)),
            Permission.TAX_VIEW);
      }

      @Override
      public ReportResult generate(ReportParameters p) {
        int year = TaxReportSupport.year(p);
        return TabularReportBuilder.of(p)
            .columns(
                ReportColumn.text("line", "Line of business"),
                ReportColumn.text("currency", "Currency"),
                ReportColumn.count("policies", "Policies placed"),
                ReportColumn.count("insurers", "Insurers"),
                ReportColumn.amount("premium", "Gross premium placed"),
                ReportColumn.amount("commission", "Commission earned"),
                ReportColumn.amount("commission_vat", "VAT on commission"))
            .rows(
                queries.production(
                    TaxReportSupport.companyId(p),
                    TaxReportSupport.yearPeriod(year).from(),
                    TaxReportSupport.yearPeriod(year).to()))
            .presorted()
            .note("Booked production of " + year + "; IC layout to confirm (AQ07).")
            .build();
      }
    };
  }
}
