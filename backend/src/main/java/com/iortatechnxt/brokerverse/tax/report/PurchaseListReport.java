package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.tax.service.TaxWorksheetService;
import org.springframework.stereotype.Component;

/** TAX-SLP – Summary List of Purchases (purchases per supplier with input VAT). */
@Component
public class PurchaseListReport extends SummaryListReport {

  /**
   * Creates the report.
   *
   * @param worksheets worksheets
   */
  public PurchaseListReport(TaxWorksheetService worksheets) {
    super(worksheets, false);
  }
}
