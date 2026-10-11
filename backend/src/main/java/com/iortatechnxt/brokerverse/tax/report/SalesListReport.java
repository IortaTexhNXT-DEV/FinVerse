package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.tax.service.TaxWorksheetService;
import org.springframework.stereotype.Component;

/** TAX-SLS – Summary List of Sales (premiums per customer with output VAT). */
@Component
public class SalesListReport extends SummaryListReport {

  /**
   * Creates the report.
   *
   * @param worksheets worksheets
   */
  public SalesListReport(TaxWorksheetService worksheets) {
    super(worksheets, true);
  }
}
